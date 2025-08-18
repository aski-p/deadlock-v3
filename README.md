# Deadlock Stats Tracker

Spring Boot + JSP 기반의 Deadlock 게임 통계 추적 웹 애플리케이션입니다.

## 주요 기능

- **Steam OpenID 로그인**: Steam 계정으로 간편 로그인
- **게임 통계 추적**: K/D/A, 승률, 매치 히스토리 확인
- **프로필 페이지**: 개인화된 게임 통계 대시보드
- **실시간 데이터**: Deadlock API를 통한 실시간 게임 데이터

## 기술 스택

### Backend
- **Spring Framework 5.3.23**: MVC 웹 프레임워크
- **Java 11**: 프로그래밍 언어
- **JSP + JSTL**: 뷰 템플릿 엔진
- **Maven**: 의존성 관리 및 빌드 도구

### Frontend
- **HTML5 + CSS3**: 마크업 및 스타일링
- **JavaScript (ES6+)**: 클라이언트 사이드 스크립팅
- **Responsive Design**: 모바일 친화적 반응형 디자인

### API 연동
- **Steam Web API**: 사용자 인증 및 프로필 정보
- **Deadlock API**: 게임 통계 및 매치 데이터
- **HTTP Client**: Apache HttpComponents

## 프로젝트 구조

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── controller/          # Spring MVC 컨트롤러
│   │   │   ├── HomeController.java
│   │   │   ├── AuthController.java
│   │   │   └── ProfileController.java
│   │   ├── service/             # 비즈니스 로직 서비스
│   │   │   ├── SteamService.java
│   │   │   ├── DeadlockService.java
│   │   │   └── HomeService.java
│   │   └── model/               # 데이터 모델
│   │       └── User.java
│   └── webapp/
│       ├── WEB-INF/
│       │   ├── views/           # JSP 뷰 파일
│       │   │   ├── layout/      # 레이아웃 컴포넌트
│       │   │   ├── home.jsp
│       │   │   └── profile.jsp
│       │   ├── spring/          # Spring 설정 파일
│       │   └── web.xml
│       └── resources/           # 정적 리소스
│           ├── css/
│           ├── js/
│           └── images/
└── test/                        # 테스트 코드
```

## 설치 및 실행

### 사전 요구사항
- Java 11 이상
- Maven 3.6 이상
- 웹 서버 (Tomcat, Jetty 등)

### 로컬 실행

1. **프로젝트 클론**
   ```bash
   git clone <repository-url>
   cd deadlock-v3
   ```

2. **의존성 설치**
   ```bash
   mvn clean install
   ```

3. **서버 실행** (Jetty 사용)
   ```bash
   mvn jetty:run
   ```

4. **브라우저에서 확인**
   ```
   http://localhost:8080
   ```

### WAR 파일 배포

1. **WAR 파일 생성**
   ```bash
   mvn clean package
   ```

2. **WAR 파일 배포**
   ```bash
   # target/spring-jsp-webapp.war 파일을 웹 서버에 배포
   ```

## API 설정

### Steam API
- Steam Web API 키가 필요합니다
- `SteamService.java`에서 API 키 설정
- Steam OpenID를 통한 사용자 인증

### Deadlock API
- Deadlock API 서버와 연동
- `DeadlockService.java`에서 API 엔드포인트 설정
- 게임 통계 및 매치 데이터 조회

## 주요 기능 설명

### 1. Steam 로그인
- Steam OpenID 2.0을 사용한 인증
- 사용자 프로필 정보 자동 연동
- 세션 기반 상태 관리

### 2. 프로필 페이지
- **매치 기록**: 최근 게임 결과 및 상세 통계
- **통계**: 전체 게임 통계 및 성과 지표
- **캐릭터**: 캐릭터별 성능 분석 (예정)

### 3. 반응형 디자인
- 모바일, 태블릿, 데스크톱 대응
- 다크 테마 기반 게이밍 UI
- 부드러운 애니메이션 효과

## 환경 설정

### application.properties (예정)
```properties
# Steam API 설정
steam.api.key=YOUR_STEAM_API_KEY
steam.api.base.url=http://api.steampowered.com

# Deadlock API 설정
deadlock.api.base.url=https://deadlock-api.com

# 세션 설정
server.servlet.session.timeout=30m
```

## 개발 가이드

### 새로운 기능 추가
1. Service Layer에 비즈니스 로직 구현
2. Controller에 REST API 엔드포인트 추가
3. JSP 뷰 파일에 UI 구성
4. JavaScript로 동적 기능 구현

### 스타일 가이드
- CSS는 모듈별로 분리 (`main.css`, `home.css`, `profile.css`)
- BEM 방법론 기반 클래스 네이밍
- 반응형 디자인 우선 고려

## 향후 계획

- [ ] 데이터베이스 연동 (사용자 설정, 즐겨찾기 등)
- [ ] 캐릭터별 상세 통계
- [ ] 친구 시스템 및 랭킹 비교
- [ ] 실시간 알림 기능
- [ ] API 캐싱 최적화
- [ ] 다국어 지원

## 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다.

## 기여

버그 리포트 및 기능 제안은 GitHub Issues를 통해 해주세요.

---

**참고**: 이 애플리케이션은 Valve Corporation과 관련이 없으며, Deadlock은 Valve Corporation의 상표입니다.