# CLAUDE.md - Deadlock Stats Tracker 개발 가이드

이 파일은 Claude Code와의 협업을 위한 프로젝트별 컨텍스트와 가이드를 제공합니다.

## 프로젝트 개요

**Deadlock Stats Tracker** - Spring Framework 기반 Deadlock 게임 통계 추적 웹 애플리케이션

### 기술 스택
- **Backend**: Spring MVC 5.3.23, Java 11
- **Frontend**: JSP + JSTL, CSS3, JavaScript ES6+
- **API**: Steam OpenID, Deadlock API, Apache HttpClient
- **Build**: Maven, Jetty (개발), Tomcat (운영)
- **Containerization**: Docker, Docker Compose

## 개발 환경 설정

### 필수 도구
- Java 11+
- Maven 3.6+
- Docker (선택사항)

### 실행 명령어
```bash
# 개발 서버 실행
mvn jetty:run

# 빌드 및 패키징
mvn clean package

# Docker 실행
docker-compose up -d
```

### 린트 및 검증
```bash
# 컴파일 및 테스트
mvn clean compile test

# WAR 파일 생성
mvn clean package
```

## 프로젝트 구조

```
src/main/
├── java/com/example/
│   ├── controller/          # Spring MVC 컨트롤러
│   │   ├── HomeController.java      # 메인 페이지
│   │   ├── AuthController.java      # Steam 인증
│   │   └── ProfileController.java   # 사용자 프로필
│   ├── service/             # 비즈니스 로직
│   │   ├── SteamService.java        # Steam API 연동
│   │   ├── DeadlockService.java     # Deadlock API 연동
│   │   └── HomeService.java         # 홈 페이지 서비스
│   └── model/               # 데이터 모델
│       └── User.java               # 사용자 모델
└── webapp/
    ├── WEB-INF/
    │   ├── views/           # JSP 뷰 템플릿
    │   │   ├── layout/      # 공통 레이아웃
    │   │   ├── home.jsp     # 메인 페이지
    │   │   └── profile.jsp  # 프로필 페이지
    │   ├── spring/          # Spring 설정
    │   └── web.xml          # 웹 설정
    └── resources/           # 정적 리소스
        ├── css/            # 스타일시트
        ├── js/             # JavaScript
        └── images/         # 이미지 리소스
```

## 개발 가이드라인

### 코딩 컨벤션
- **Java**: Oracle Java 코딩 스타일 준수
- **JSP**: JSTL 태그 라이브러리 활용
- **CSS**: BEM 방법론 기반 클래스 네이밍
- **JavaScript**: ES6+ 문법 사용, 모듈화 권장

### 파일 네이밍
- **Controller**: `*Controller.java` (PascalCase)
- **Service**: `*Service.java` (PascalCase)
- **JSP**: `*.jsp` (lowercase)
- **CSS**: `*.css` (lowercase)
- **JavaScript**: `*.js` (lowercase)

### API 연동 패턴
- Service 레이어에서 외부 API 호출
- HTTP 클라이언트는 Apache HttpComponents 사용
- JSON 파싱은 Jackson ObjectMapper 활용
- 에러 처리 및 fallback 데이터 제공

## 주요 기능 모듈

### 1. 인증 시스템 (AuthController)
- Steam OpenID 2.0 인증
- 세션 기반 사용자 상태 관리
- 로그인/로그아웃 처리

### 2. 게임 통계 (DeadlockService)
- Deadlock API 연동
- 매치 데이터 파싱 및 가공
- 실시간 통계 계산

### 3. 사용자 프로필 (ProfileController)
- 개인화된 대시보드
- 매치 히스토리 표시
- 통계 시각화

### 4. 반응형 UI
- 모바일 우선 반응형 디자인
- 다크 테마 게이밍 스타일
- CSS Grid 및 Flexbox 활용

## 배포 및 운영

### 개발 환경
- Jetty 내장 서버 사용
- 포트: 8080
- 핫 리로드 지원

### 운영 환경
- WAR 파일 배포
- Tomcat 서버 권장
- 환경별 설정 분리

### Docker 배포
```bash
# 이미지 빌드
docker build -t deadlock-app .

# 컨테이너 실행
docker run -d -p 8080:8080 --name deadlock-server deadlock-app
```

## API 연동 설정

### Steam API
- Steam Web API 키 필요
- OpenID 엔드포인트: `https://steamcommunity.com/openid/`
- 사용자 프로필 API 연동

### Deadlock API
- 베이스 URL: `https://deadlock-api.com`
- 플레이어 매치 데이터 조회
- 실시간 통계 API 연동

## 문제 해결

### 일반적인 이슈
1. **포트 충돌**: `mvn jetty:run -Djetty.port=8081`
2. **빌드 실패**: `mvn clean install -U`
3. **Steam 로그인 실패**: API 키 및 네트워크 설정 확인

### 디버깅
- 로그 레벨: INFO (기본), DEBUG (개발시)
- 브라우저 개발자 도구 활용
- Maven Surefire 플러그인으로 단위 테스트

## 향후 개발 계획

### 단기 목표
- [ ] 실제 Deadlock API 연동 완성
- [ ] 사용자 설정 페이지 추가
- [ ] 캐릭터별 상세 통계 구현

### 중기 목표
- [ ] 데이터베이스 연동 (H2/PostgreSQL)
- [ ] 친구 시스템 구현
- [ ] 실시간 알림 기능

### 장기 목표
- [ ] 모바일 앱 개발 (React Native)
- [ ] API 캐싱 최적화
- [ ] 다국어 지원 (i18n)

## MCP 서버 설정 (Claude Code)

### 필수 MCP 서버
프로젝트 개발 효율성을 위해 다음 MCP 서버들을 설정하는 것을 권장합니다:

#### 1. Playwright MCP (테스팅 & 자동화)
```bash
# 설치
npm install -g @anthropic-ai/mcp-server-playwright

# Claude Code 설정 파일에 추가
# ~/.config/Claude/claude_desktop_config.json (Linux)
# ~/Library/Application Support/Claude/claude_desktop_config.json (macOS)
# %APPDATA%/Claude/claude_desktop_config.json (Windows)
```

```json
{
  "mcpServers": {
    "playwright": {
      "command": "npx",
      "args": ["@anthropic-ai/mcp-server-playwright"],
      "env": {}
    }
  },
  "subAgents": "See ~/.claude/subagents.json for detailed configuration"
}
```

#### 2. Sequential MCP (복잡한 분석)
```json
{
  "mcpServers": {
    "sequential": {
      "command": "npx",
      "args": ["@anthropic-ai/mcp-server-sequential"],
      "env": {}
    }
  }
}
```

#### 3. Context7 MCP (문서화 & 패턴)
```json
{
  "mcpServers": {
    "context7": {
      "command": "npx", 
      "args": ["@anthropic-ai/mcp-server-context7"],
      "env": {}
    }
  }
}
```

### MCP 활용 예시

#### E2E 테스트 자동화 (Playwright)
```javascript
// tests/e2e/steam-login.spec.js
test('Steam 로그인 플로우', async ({ page }) => {
  await page.goto('http://localhost:8080');
  await page.click('text=Steam으로 시작하기');
  await expect(page).toHaveURL(/steamcommunity\.com/);
});
```

#### 성능 모니터링
```javascript
test('프로필 페이지 성능', async ({ page }) => {
  await page.goto('http://localhost:8080/profile');
  const metrics = await page.evaluate(() => 
    performance.getEntriesByType('navigation')
  );
  console.log('로딩 시간:', metrics[0].loadEventEnd - metrics[0].navigationStart);
});
```

### Claude Code 명령어 패턴

#### 출력 스타일 설정
```bash
# 출력 스타일 옵션 확인
/output-style

# 간결한 출력 (기본값 - 개발 시 권장)
/output-style concise

# 상세한 출력 (학습/디버깅 시 권장)  
/output-style verbose

# 최소한의 출력 (CI/CD 환경 권장)
/output-style minimal

# JSON 형태 출력 (API 통합 시 권장)
/output-style json

# 프로젝트별 권장 설정
/output-style concise --show-tokens true  # 토큰 사용량 모니터링
```

#### 분석 작업
```bash
# 코드 품질 분석 (Sequential MCP 활용)
claude analyze --seq --focus quality

# 성능 분석 (Playwright MCP 활용)  
claude analyze --play --focus performance
```

#### 테스트 작업 (SubAgent 자동 활성화)
```bash
# E2E 테스트 생성 및 실행 (Playwright SubAgent 자동 활성화)
claude test --play --type e2e

# Steam 로그인 플로우 테스트
claude test "Steam login flow" --delegate playwright

# 성능 테스트 (SubAgent가 자동으로 성능 메트릭 수집)
claude test --performance --delegate playwright

# 반응형 디자인 테스트 
claude test --visual --delegate playwright

# 통합 테스트 실행
claude test --seq --comprehensive
```

#### 개선 작업
```bash
# 반응형 디자인 개선
claude improve --play --focus accessibility

# API 성능 최적화
claude improve --seq --focus performance
```

#### 설정 관리
```bash
# 현재 설정 확인
claude config show

# Deadlock 프로젝트 최적화 설정
claude config set output-style concise
claude config set show-tokens true
claude config set auto-commit false
claude config set default-language java
claude config set tab-size 4
```

## 기여 가이드

### 코드 스타일
- Google Java Style Guide 준수
- 메서드 및 클래스 JavaDoc 주석 필수
- 단위 테스트 작성 권장

### 커밋 메시지
```
feat: Steam 로그인 기능 구현
fix: 프로필 페이지 로딩 오류 수정
docs: API 연동 가이드 업데이트
style: CSS 반응형 디자인 개선
test: E2E 테스트 케이스 추가
perf: API 응답 시간 최적화
```

### MCP 기반 워크플로우 (SubAgent 통합)
1. **분석**: `claude analyze --seq` → 복잡한 시스템 분석
2. **개발**: Spring 패턴 기반 구현
3. **테스트**: `claude test --delegate playwright` → Playwright SubAgent 자동 E2E 테스트
4. **검증**: `claude improve --validate` → 품질 검증
5. **배포**: Docker 기반 컨테이너 배포

### SubAgent 설정 위치
SubAgent 설정은 `~/.claude/subagents.json`에서 관리됩니다:

```bash
# SubAgent 설정 확인
cat ~/.claude/subagents.json

# 설정 편집
code ~/.claude/subagents.json
```

### 자동 활성화 조건
- **Playwright Agent**: test, e2e, browser, automation 키워드
- **Sequential Agent**: analyze, debug, architecture 키워드  
- **Context7 Agent**: document, guide, pattern 키워드
- **Deadlock 특화**: Steam 로그인, 프로필 페이지, 게임 통계 UI

---

**참고**: 이 프로젝트는 교육 목적으로 개발되었으며, Valve Corporation과 공식적인 관련이 없습니다.