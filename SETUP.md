# 🚀 Deadlock Stats Tracker - 서버 실행 가이드

## 빠른 실행 (Windows)

### 방법 1: 자동 실행 스크립트
```cmd
# 프로젝트 폴더에서
run.bat
```

### 방법 2: 수동 실행
```cmd
# 1. 프로젝트 빌드
mvn clean install

# 2. 서버 실행
mvn jetty:run

# 3. 브라우저에서 접속
# http://localhost:8080
```

## 필수 요구사항

### Java 11+ 설치
- [Eclipse Temurin](https://adoptium.net/) 또는 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 다운로드
- 설치 후 `java -version`으로 확인

### Maven 설치
- [Apache Maven](https://maven.apache.org/download.cgi) 다운로드
- 환경변수 PATH에 Maven bin 폴더 추가
- 설치 후 `mvn -version`으로 확인

## Docker를 사용한 실행 (권장)

### Docker Desktop 설치 후
```bash
# 프로젝트 폴더에서
docker build -t deadlock-app .
docker run -d -p 8080:8080 --name deadlock-server deadlock-app

# 서버 접속: http://localhost:8080
```

### Docker Compose 사용
```bash
docker-compose up -d
```

## 서버 접속 정보

- **URL**: http://localhost:8080
- **포트**: 8080
- **Steam 로그인**: Steam OpenID 사용
- **API 연동**: Deadlock API 자동 연결

## 주요 기능

### 🎮 Steam 로그인
- Steam 계정으로 간편 로그인
- 프로필 정보 자동 연동

### 📊 게임 통계
- K/D/A 비율 및 승률 표시
- 최근 매치 기록 확인
- 캐릭터별 성능 분석

### 📱 반응형 디자인
- 모바일/태블릿/데스크톱 지원
- 다크 테마 게이밍 UI

## 문제 해결

### 포트 충돌 발생시
```bash
# 다른 포트로 실행
mvn jetty:run -Djetty.port=8081
```

### 빌드 오류 발생시
```bash
# 캐시 정리 후 재빌드
mvn clean
mvn install -U
```

### Steam 로그인 안됨
- Steam API 키 확인 (`SteamService.java`)
- 방화벽/프록시 설정 확인

## 개발자 모드

### 실시간 코드 변경 반영
```bash
# JSP 파일 변경시 자동 반영
mvn jetty:run -Djetty.reload=automatic
```

### 디버그 모드
```bash
# 디버그 포트 5005로 실행
mvn jetty:run -Dmaven.surefire.debug
```

## 배포

### WAR 파일 생성
```bash
mvn clean package
# target/spring-jsp-webapp.war 생성
```

### Tomcat 배포
1. WAR 파일을 Tomcat webapps 폴더에 복사
2. Tomcat 재시작
3. http://localhost:8080/spring-jsp-webapp 접속

---

## 📞 지원

문제가 발생하면 GitHub Issues를 통해 문의해주세요.

**즐거운 Deadlock 게임 되세요! 🎯**