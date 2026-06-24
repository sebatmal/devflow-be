# DevFlow Backend

GitHub 위에서 도는 팀프로젝트 관리 도구 **DevFlow**의 백엔드.
컨벤션은 `mokkoji-be` 기반 (Spring Boot 3 · Java 21 · `{data}` 응답 래퍼 · `FailMessage` 에러코드 · JWT + `@Authentication` resolver).

## 현재 구현 (슬라이스 1)
- 공통 인프라(응답/예외/JWT/CORS/H2)
- **GitHub OAuth 로그인** + `GET /api/me`
- **실제 GitHub 이슈 생성** `POST /api/repos/{owner}/{repo}/issues`

## 준비물 — GitHub OAuth App 등록 (최초 1회, 5분)
1. GitHub → Settings → Developer settings → **OAuth Apps** → New OAuth App
2. 입력
   - Application name: `DevFlow (local)`
   - Homepage URL: `http://localhost:8080`
   - **Authorization callback URL: `http://localhost:8080/api/auth/github/callback`**
3. 발급된 **Client ID / Client Secret** 을 환경변수로:
   ```bash
   export GITHUB_CLIENT_ID=xxxx
   export GITHUB_CLIENT_SECRET=xxxx
   # (선택) 로그인 후 프론트로 토큰 리다이렉트하려면:
   # export FRONTEND_REDIRECT=http://localhost:5175/auth/callback
   ```
   Windows PowerShell: `$env:GITHUB_CLIENT_ID="xxxx"` 등

## 실행
```bash
./gradlew bootRun        # http://localhost:8080
# H2 콘솔: http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:devflow)
```

## 동작 흐름 (로컬 테스트)
1. 브라우저로 **`http://localhost:8080/api/auth/github/login`** 접속 → GitHub 인증
2. 콜백이 `FRONTEND_REDIRECT` 미설정 시 **JSON으로 accessToken 반환**:
   ```json
   { "data": { "accessToken": "eyJ...", "user": { "login": "INSANE-P", ... } } }
   ```
3. 그 토큰으로 인증 API 호출:
   ```bash
   curl -H "Authorization: Bearer eyJ..." http://localhost:8080/api/me

   curl -X POST http://localhost:8080/api/repos/{owner}/{repo}/issues \
     -H "Authorization: Bearer eyJ..." -H "Content-Type: application/json" \
     -d '{ "title": "결제 요청 API", "body": "PG 연동 후 승인 요청", "labels": ["BE"] }'
   ```
   → 실제 GitHub 레포에 이슈가 생성되고 `{ number, htmlUrl }` 반환.

## 다음 슬라이스 (예정)
- 도메인: Project / Member / Task(기능·이슈) / Dependency
- 의존성 생성 + **위상정렬 순환 검증** + GitHub `blocked by` 연동
- AI 이슈 분해 / 의존성 추천 (먼저 목 응답 스텁 → 이후 LLM)
  - ※ 의존성 추천은 **머지 안 된 활성 이슈만** AI에 전달 (완료 서브그래프는 동결)

## 스택
Spring Boot 3.4 · Java 21 · Spring Web · Data JPA · H2 · JWT(jjwt) · RestClient(GitHub 연동)
