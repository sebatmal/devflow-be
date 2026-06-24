# DevFlow Backend

GitHub 위에서 도는 팀프로젝트 관리 도구 **DevFlow**의 백엔드.  
Spring Boot 3 · Java 21 · `{data}` 응답 래퍼 · `FailMessage` 에러코드 · JWT + `@Authentication` resolver.

## 스택

Spring Boot 3.4 · Java 21 · Spring Web · Data JPA · H2 · JWT(jjwt) · RestClient · OpenAI API

---

## 실행

```bash
# 환경변수 설정
export GITHUB_CLIENT_ID=xxxx
export GITHUB_CLIENT_SECRET=xxxx
export GITHUB_REDIRECT_URI=http://localhost:5173/frontend/callback
export OPENAI_API_KEY=sk-...

./gradlew bootRun   # http://localhost:8080
# H2 콘솔: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:devflow)
```

---

## API 목록

### 인증

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/auth/github/login` | 불필요 | GitHub 인증 페이지로 리다이렉트 |
| GET | `/api/auth/github/callback?code=` | 불필요 | code → JWT 교환 |
| GET | `/api/me` | **필요** | 내 정보 조회 |

### 조직

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/organizations` | **필요** | 내 GitHub org 목록 |
| POST | `/api/organizations/{org}/connect` | **필요** | org 연결 + open 이슈 1주차 동기화 |

### 프로젝트 · 멤버

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/projects/{projectId}` | 불필요 | 프로젝트 조회 |
| GET | `/api/projects/{projectId}/members` | 불필요 | 멤버 목록 |

### 기능(Feature)

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/projects/{projectId}/features` | 불필요 | 기능 목록 |
| GET | `/api/features/{featureId}` | 불필요 | 기능 단건 조회 |
| POST | `/api/projects/{projectId}/features` | 불필요 | 기능 생성 |
| PATCH | `/api/features/{featureId}` | 불필요 | 기능 수정 (title·week·lane) |
| DELETE | `/api/features/{featureId}` | 불필요 | 기능 삭제 (하위 이슈·의존성 포함) |

### 작업(Task)

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/projects/{projectId}/tasks` | 불필요 | 전체 작업 목록 (의존성 포함) |
| PATCH | `/api/tasks/{taskId}` | 불필요 | 작업 주차 이동 (의존성 순서 검증) |

### 이슈

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/features/{featureId}/issues` | **필요** | 이슈 분리 확정 + GitHub 이슈 생성 |
| POST | `/api/repos/{owner}/{repo}/issues` | **필요** | GitHub 이슈 직접 생성 |

### 의존성

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/dependencies` | **필요** | 의존성 생성 (순환 검증 + GitHub blocked_by) |
| DELETE | `/api/dependencies?from=&to=` | 불필요 | 의존성 삭제 |

### PR

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/projects/{projectId}/prs` | 불필요 | PR 목록 |

### AI

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/features/{featureId}/suggest-issues` | 불필요 | 이슈 분해 추천 (OpenAI) |
| POST | `/api/projects/{projectId}/suggest-dependencies` | **필요** | 의존성 추천 (GitHub 이슈 동기화 + OpenAI) |

### TLR

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/tlr/analyze` | 불필요 | TLR 파이프라인 (의존성 추천 + DAG 검증 + 위상정렬) |
| GET | `/api/tlr/health` | 불필요 | 헬스 체크 |

---

## 에러 코드

| code | status | 의미 |
|------|--------|------|
| 40000 | 400 | 잘못된 요청 |
| 40001 | 400 | 요청 본문 검증 실패 |
| 40006 | 400 | 자기 자신은 선행이 될 수 없음 |
| 40100 | 401 | 인증 필요 |
| 40101 | 401 | 토큰 만료 |
| 40102 | 401 | 인증 정보 없음 |
| 40103 | 401 | 토큰 정보 오류 |
| 40104 | 401 | GitHub 토큰 없음 (재로그인 필요) |
| 40403 | 404 | 프로젝트 없음 |
| 40404 | 404 | 작업 없음 |
| 40405 | 404 | 의존성 없음 |
| 40901 | 409 | 순환 의존 (거부) |
| 40902 | 409 | 중복 의존성 |
| 40903 | 409 | 이미 이슈가 생성된 기능 |
| 40904 | 409 | 의존성 순서 때문에 이동 불가 |
| 50000 | 500 | 서버 오류 |
| 50201 | 502 | GitHub API 실패 |
| 50202 | 502 | AI API 실패 |
