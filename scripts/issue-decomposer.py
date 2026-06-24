"""
기능 → 이슈 분해 파이프라인 (사람 검토 보조용)

구조(앞서 그린 다이어그램과 1:1 대응):

    입력: 기능
      └─[호출 1] 분해(LLM)        ── decompose()      : 축 결정 + 초기 분할 + 플래그
      └─[코드]   재분할 루프        ── resize_loop()    : 큰 이슈만 골라 [호출 2] split_issue()
      └─[호출 3] 완전성 비평(LLM)   ── check_completeness()
      └─[호출 4] AC 생성(LLM)       ── generate_acceptance_criteria()
      └─[코드]   DoD 주입           ── inject_dod()
    출력: 이슈 집합

핵심 원칙:
  - 반복/종료/사람 확인은 "코드"가 통제한다. LLM은 한 번에 잘하는 추론만 맡는다.
  - LLM 출력은 항상 구조화(JSON)로 받아 코드가 분기할 수 있게 한다.
  - 생성(분해)과 검증(완전성)은 다른 호출로 분리한다(자기 편향 방지).

실행: 환경변수 ANTHROPIC_API_KEY 설정 후 `python issue_decomposer.py`
의존성: pip install anthropic
"""

from __future__ import annotations

import json
import re
import os
from dataclasses import dataclass, field, asdict
from typing import Callable, Optional

from anthropic import Anthropic


# ─────────────────────────────────────────────────────────────────────────────
# 설정 — 임계값과 종료 조건은 전부 여기(코드)에서 고정한다. 모델에게 맡기지 않는다.
# ─────────────────────────────────────────────────────────────────────────────

MODEL = "claude-sonnet-4-6"   # 더 어려운 분해는 "claude-opus-4-8"로 교체 가능
MAX_DAYS = 2.0                # 이 추정치를 넘고 splittable이면 "너무 큼" → 재분할 대상
CONFIDENCE_THRESHOLD = 0.7    # 이 미만이면 사람 확인 대상
MAX_SPLIT_ROUNDS = 2          # 재분할 루프 상한 — 종료 보장. 모델은 스스로 못 멈춘다.

DOD = [                       # Definition of Done: 모든 이슈 공통 상수 (LLM 불필요)
    "코드 리뷰 승인",
    "단위/통합 테스트 통과",
    "스테이징 배포 확인",
    "관련 문서 업데이트",
]

client = Anthropic()


# ─────────────────────────────────────────────────────────────────────────────
# 데이터 모델
# ─────────────────────────────────────────────────────────────────────────────

@dataclass
class Issue:
    id: str
    title: str
    user_story: str                       # 수직 슬라이스: "사용자가 X 할 수 있다"
    pattern: str                          # SPIDR: Path / Interface / Data / Rules / Spike
    estimated_days: float
    splittable: bool                      # 더 작은 가치 슬라이스로 쪼갤 수 있는가
    confidence: float                     # 0~1, 모델이 이 분해를 얼마나 확신하는가
    is_spike: bool = False                # 조사용 이슈(가치 슬라이스 규칙의 의도된 예외)
    is_non_user_facing: bool = False      # 인프라/마이그레이션 등
    acceptance_criteria: list[str] = field(default_factory=list)
    dod: list[str] = field(default_factory=list)

    @staticmethod
    def from_dict(d: dict, fallback_id: str) -> "Issue":
        return Issue(
            id=str(d.get("id") or fallback_id),
            title=d.get("title", "(제목 없음)"),
            user_story=d.get("user_story", ""),
            pattern=d.get("pattern", "Path"),
            estimated_days=float(d.get("estimated_days", 1.0)),
            splittable=bool(d.get("splittable", False)),
            confidence=float(d.get("confidence", 0.5)),
            is_spike=bool(d.get("is_spike", False)),
            is_non_user_facing=bool(d.get("is_non_user_facing", False)),
        )


@dataclass
class CheckpointItem:
    """사람에게 띄울 항목. 확신 있는 건 빼고 '애매한 것만' 모은다."""
    kind: str          # "axis" | "low_confidence" | "spike" | "non_user_facing" | "still_large" | "gap" | "overlap"
    detail: str


# on_checkpoint(name, items): 사람 확인 지점. 실제 서비스에서는 여기서 UI에 띄우고
# 응답을 기다리며 블로킹한다. 기본 구현은 콘솔 출력만 하고 진행한다.
CheckpointHook = Callable[[str, list[CheckpointItem]], None]


def _default_checkpoint(name: str, items: list[CheckpointItem]) -> None:
    print(f"\n=== 사람 확인 지점: {name} — {len(items)}건 ===")
    for it in items:
        print(f"  · [{it.kind}] {it.detail}")
    if not items:
        print("  (확인할 애매한 항목 없음 — 조용히 통과)")


# ─────────────────────────────────────────────────────────────────────────────
# LLM 호출 유틸 — 항상 JSON으로 받고, 파싱 실패 시 1회 재시도한다.
# ─────────────────────────────────────────────────────────────────────────────

def _call_llm(system: str, user: str, max_tokens: int = 4096) -> str:
    resp = client.messages.create(
        model=MODEL,
        max_tokens=max_tokens,
        system=system,
        messages=[{"role": "user", "content": user}],
    )
    return "".join(b.text for b in resp.content if b.type == "text")


def _extract_json(text: str):
    """코드펜스/잡설을 걷어내고 첫 JSON 객체·배열을 파싱한다."""
    m = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL)
    if m:
        text = m.group(1)
    text = text.strip()
    start = min(
        (i for i in (text.find("{"), text.find("[")) if i != -1),
        default=-1,
    )
    if start > 0:
        text = text[start:]
    return json.loads(text)


def _call_json(system: str, user: str, max_tokens: int = 4096):
    raw = _call_llm(system, user, max_tokens)
    try:
        return _extract_json(raw)
    except (json.JSONDecodeError, ValueError):
        # 한 번 더, JSON만 내놓으라고 못박아 재시도
        raw = _call_llm(system, user + "\n\n반드시 유효한 JSON만 출력하라. 다른 텍스트 금지.", max_tokens)
        return _extract_json(raw)


# ─────────────────────────────────────────────────────────────────────────────
# 프롬프트 — 방법론을 모델에게 명시한다. JSON 키는 영어로 고정(코드 안정성).
# ─────────────────────────────────────────────────────────────────────────────

_ISSUE_SCHEMA = json.dumps({
    "id": "tmp-1",
    "title": "짧은 제목",
    "user_story": "사용자가 ~할 수 있다 (수직 슬라이스)",
    "pattern": "Path | Interface | Data | Rules | Spike",
    "estimated_days": 1.5,
    "splittable": False,
    "confidence": 0.82,
    "is_spike": False,
    "is_non_user_facing": False,
}, ensure_ascii=False, indent=2)

SYS_DECOMPOSE = f"""너는 기능을 이슈로 분해하는 엔진이다. 다음 방법론을 따른다.

[분할 방향] 수직 슬라이스: 기술 계층(DB/API/UI)으로 가로 분할하지 말 것.
각 이슈는 여러 계층을 관통해 사용자 가치를 주는 "사용자가 X 할 수 있다" 형태여야 한다.
예외: 조사가 필요하면 Spike, 인프라성 작업이면 is_non_user_facing=true로 표시.

[분할 패턴] SPIDR 중 가장 적합한 축 하나를 기능 전체에 적용한다.
  Path(경로) / Interface(UI·플랫폼) / Data(데이터 변형) / Rules(규칙) / Spike(조사)

[플래그] 각 이슈마다:
  - estimated_days: 이상적 작업일 추정(소수 가능)
  - splittable: 더 작은 '가치 슬라이스'로 쪼갤 수 있으면 true
  - confidence: 이 이슈가 옳고 잘 잘렸다는 확신 0~1 (애매하면 낮게)

각 이슈 형식:
{_ISSUE_SCHEMA}

출력은 다음 JSON만:
{{"axis": "<선택한 SPIDR 축>", "axis_rationale": "<한 문장>", "issues": [<이슈들>]}}"""

SYS_SPLIT = f"""너는 너무 큰 이슈 하나를 더 작은 수직 슬라이스로 쪼개는 엔진이다.
SPIDR 패턴을 사용해 2~4개의 더 작은 이슈로 나눈다. 각각은 여전히 "사용자가 X 할 수 있다" 형태여야 한다.
각 이슈 형식:
{_ISSUE_SCHEMA}
출력은 {{"issues": [<이슈들>]}} JSON만."""

SYS_CRITIC = """너는 분해 결과를 검증하는 비평가다. 직접 만들지 않았으므로 냉정하게 본다.
주어진 [기능 전체]와 [이슈 목록]을 비교해, WBS 100% 규칙 위반만 찾는다.
  - gaps: 기능에 있는데 어떤 이슈도 덮지 않은 부분(누락)
  - overlaps: 두 이슈가 같은 일을 중복으로 다루는 경우
없으면 빈 배열. 출력은 다음 JSON만:
{"gaps": ["<누락 설명>"], "overlaps": [["<id_a>", "<id_b>", "<중복 이유>"]]}"""

SYS_AC = """너는 각 이슈의 수용 기준(AC)을 작성한다.
일반 이슈: Given-When-Then 형식 1~3개. ("Given ..., When ..., Then ...")
Spike 이슈: 조사로 답해야 할 핵심 질문 1~2개.
출력은 {"<issue_id>": ["<기준1>", ...]} 형태의 JSON만."""


# ─────────────────────────────────────────────────────────────────────────────
# [호출 1] 분해
# ─────────────────────────────────────────────────────────────────────────────

def decompose(feature: str) -> tuple[str, str, list[Issue]]:
    data = _call_json(SYS_DECOMPOSE, f"[기능]\n{feature}")
    issues = [
        Issue.from_dict(d, fallback_id=f"I{idx+1}")
        for idx, d in enumerate(data.get("issues", []))
    ]
    return data.get("axis", "?"), data.get("axis_rationale", ""), issues


# ─────────────────────────────────────────────────────────────────────────────
# [호출 2] 한 이슈 재분할
# ─────────────────────────────────────────────────────────────────────────────

def split_issue(feature: str, issue: Issue) -> list[Issue]:
    user = f"[기능 맥락]\n{feature}\n\n[너무 큰 이슈]\n{issue.title}: {issue.user_story} (약 {issue.estimated_days}일)"
    data = _call_json(SYS_SPLIT, user)
    return [Issue.from_dict(d, fallback_id=f"{issue.id}-{i+1}") for i, d in enumerate(data.get("issues", []))]


# ─────────────────────────────────────────────────────────────────────────────
# [코드] 재분할 루프 — 여기가 핵심. 루프 주체가 모델이 아니라 코드다.
# ─────────────────────────────────────────────────────────────────────────────

def _too_big(issue: Issue) -> bool:
    return (not issue.is_spike) and issue.estimated_days > MAX_DAYS and issue.splittable


def resize_loop(feature: str, issues: list[Issue]) -> list[Issue]:
    current = list(issues)
    for _ in range(MAX_SPLIT_ROUNDS):
        big = [i for i in current if _too_big(i)]
        if not big:                       # 종료 조건 1: 더 쪼갤 게 없음
            break
        settled = [i for i in current if not _too_big(i)]
        replacements: list[Issue] = []
        for issue in big:
            replacements.extend(split_issue(feature, issue))   # [호출 2]
        current = settled + replacements
    # 종료 조건 2: 라운드 상한 도달. 남은 큰 이슈는 그대로 두되 나중에 사람에게 플래그.
    for idx, issue in enumerate(current):  # id 재부여(중복 방지)
        issue.id = f"I{idx+1}"
    return current


# ─────────────────────────────────────────────────────────────────────────────
# [호출 3] 완전성 비평 (생성과 분리된 별도 호출)
# ─────────────────────────────────────────────────────────────────────────────

def check_completeness(feature: str, issues: list[Issue]) -> dict:
    listing = "\n".join(f"- {i.id} | {i.title}: {i.user_story}" for i in issues)
    data = _call_json(SYS_CRITIC, f"[기능 전체]\n{feature}\n\n[이슈 목록]\n{listing}")
    return {"gaps": data.get("gaps", []), "overlaps": data.get("overlaps", [])}


# ─────────────────────────────────────────────────────────────────────────────
# [호출 4] AC 생성  +  [코드] DoD 주입
# ─────────────────────────────────────────────────────────────────────────────

def generate_acceptance_criteria(issues: list[Issue]) -> None:
    listing = "\n".join(
        f"- {i.id} | {i.title}: {i.user_story}" + (" [SPIKE]" if i.is_spike else "")
        for i in issues
    )
    data = _call_json(SYS_AC, f"[이슈 목록]\n{listing}")
    for issue in issues:
        issue.acceptance_criteria = data.get(issue.id, [])


def inject_dod(issues: list[Issue]) -> None:
    for issue in issues:          # DoD는 LLM 없이 코드가 일괄 주입
        issue.dod = list(DOD)


# ─────────────────────────────────────────────────────────────────────────────
# 사람 확인 대상 추출 — '애매한 것만' 고른다(신뢰도 보정).
# ─────────────────────────────────────────────────────────────────────────────

def collect_review_items(issues: list[Issue]) -> list[CheckpointItem]:
    items: list[CheckpointItem] = []
    for i in issues:
        if i.confidence < CONFIDENCE_THRESHOLD:
            items.append(CheckpointItem("low_confidence", f"{i.id} {i.title} (확신 {i.confidence:.2f})"))
        if i.is_spike:
            items.append(CheckpointItem("spike", f"{i.id} {i.title} — 조사용 이슈"))
        if i.is_non_user_facing:
            items.append(CheckpointItem("non_user_facing", f"{i.id} {i.title} — 비사용자대면"))
        if _too_big(i):
            items.append(CheckpointItem("still_large", f"{i.id} {i.title} — 상한까지 쪼갰으나 여전히 큼"))
    return items


# ─────────────────────────────────────────────────────────────────────────────
# 오케스트레이터 — 위 조각을 순서대로 엮고, 호출 경계에서 사람 확인을 띄운다.
# ─────────────────────────────────────────────────────────────────────────────

def run_pipeline(feature: str, on_checkpoint: Optional[CheckpointHook] = None) -> list[Issue]:
    on_checkpoint = on_checkpoint or _default_checkpoint

    # [호출 1] 분해
    axis, rationale, issues = decompose(feature)
    # 사람 확인 ①: 분할 축 (가장 상위 결정만 승인)
    on_checkpoint("분할 축 승인", [CheckpointItem("axis", f"{axis} — {rationale}")])

    # [코드] 재분할 루프
    issues = resize_loop(feature, issues)

    # [호출 3] 완전성 비평
    report = check_completeness(feature, issues)
    coverage_items = (
        [CheckpointItem("gap", g) for g in report["gaps"]]
        + [CheckpointItem("overlap", f"{a} ↔ {b}: {why}") for a, b, *rest in report["overlaps"] for why in [rest[0] if rest else ""]]
        + collect_review_items(issues)
    )
    # 사람 확인 ②: 커버리지 맵 + 애매한 이슈만
    on_checkpoint("커버리지 / 애매한 이슈", coverage_items)

    # [호출 4] AC + [코드] DoD
    generate_acceptance_criteria(issues)
    inject_dod(issues)
    return issues


# ─────────────────────────────────────────────────────────────────────────────
# 데모
# ─────────────────────────────────────────────────────────────────────────────

def _silent_checkpoint(_name: str, _items: list[CheckpointItem]) -> None:
    """JSON 모드에서는 중간 출력을 모두 무시한다."""
    pass


def run_json_mode(feature: str) -> None:
    """CLI JSON 모드: stdout에 파싱 가능한 JSON만 출력한다."""
    issues = run_pipeline(feature, on_checkpoint=_silent_checkpoint)
    output = {
        "issues": [asdict(i) for i in issues],
    }
    print(json.dumps(output, ensure_ascii=False))


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="기능 → 이슈 분해 파이프라인")
    parser.add_argument("--json", action="store_true", help="JSON stdout 출력 모드")
    parser.add_argument("--feature", type=str, help="분해할 기능 텍스트")
    args = parser.parse_args()

    if not os.getenv("ANTHROPIC_API_KEY"):
        raise SystemExit("환경변수 ANTHROPIC_API_KEY를 먼저 설정하세요.")

    if args.json:
        if not args.feature:
            raise SystemExit("--json 모드에서는 --feature 인자가 필수입니다.")
        run_json_mode(args.feature)
    else:
        feature = args.feature or (
            "사용자가 소셜 계정(구글/카카오)이나 이메일로 가입·로그인하고, "
            "비밀번호를 잊었을 때 이메일로 재설정할 수 있다."
        )
        result = run_pipeline(feature)

        print("\n" + "=" * 60)
        print("최종 이슈")
        print("=" * 60)
        for i in result:
            print(f"\n[{i.id}] {i.title}  ({i.pattern}, ~{i.estimated_days}일, 확신 {i.confidence:.2f})")
            print(f"  · {i.user_story}")
            for ac in i.acceptance_criteria:
                print(f"  - AC: {ac}")
