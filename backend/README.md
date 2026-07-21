# Gig Hub Backend

Spring Framework Legacy, MyBatis, MySQL 기반의 WAR 애플리케이션입니다. Spring Boot와 JPA는 사용하지 않으며 외부 Tomcat 9.0.118에 배포합니다.

## 현재 상태

- Gradle Java 17 + WAR 프로젝트
- Java Config 기반 Root Context·Servlet Context 초기화
- `GET /api/health` 최소 API와 MockMvc 테스트
- Checkstyle 및 Javadoc 생성 설정
- 외부 properties 기반 HikariCP·MyBatis·Transaction 설정
- Session Security와 실제 도메인 기능은 TODO 상태

## 패키지 구조

```text
src/main/java/com/gighub/
  config/       Root·MVC·Servlet 초기화 설정
  common/       공통 API 응답과 예외 처리
  health/       서버 기동 확인 API
  auth/         로그인·Session·권한
  member/       고용주·근로자 계정
  work/         근무 건·초대·수락
  contract/     계약·서명·문서함
  attendance/   QR·위치 기반 출퇴근
  wallet/       Mock 계좌·선예치 원장
  settlement/   정산·환불·노쇼 처리
```

각 도메인의 실제 구현은 다음 계층을 기본으로 합니다.

```text
domain/
  controller/   HTTP 입력 검증과 응답 변환
  service/      Transaction과 비즈니스 규칙
  mapper/       MyBatis Mapper Interface
  dto/          명시적 요청·응답·내부 전달 객체
```

SQL은 Java Annotation이 아닌 `src/main/resources/mappers/`의 MyBatis Mapper XML에 둡니다.

## 로컬 Docker MySQL 연결

`backend` 폴더에서 예제 파일을 복사하고 루트 `.env`와 같은 DB 이름·사용자·비밀번호로 수정합니다.

```powershell
Copy-Item config/database.example.properties config/database-local.properties
```

`database-local.properties`는 Git과 WAR에서 제외됩니다. `database.jdbc-url`의 포트는 루트 `.env`의
`MYSQL_PORT`와 같아야 하며, 호스트에서 실행하는 Tomcat은 Compose 내부 주소 `db:3306`이 아니라
`localhost:MYSQL_PORT`를 사용합니다.

IntelliJ의 Tomcat Run Configuration에서 `VM options`에 다음 시스템 속성을 추가합니다. 경로는 실제
저장소의 절대 경로로 바꿉니다.

```text
-Dgighub.database.config="C:/absolute/path/to/KB PJT/backend/config/database-local.properties"
```

설정 파일이 없거나 필수 키가 빠지면 애플리케이션 시작이 실패합니다. 이 설정은 기존 Flyway Migration을
자동 실행하지 않으므로 Tomcat 시작 전에 루트에서 Flyway `migrate`와 `validate`를 완료해야 합니다.

### 실제 DB 조회 테스트

Tomcat과 동일한 `RootConfig`로 로컬 MySQL의 DB 이름과 `users` 테이블 행 수를 읽는 Test 전용 작업입니다.
일반 `test`와 `npm run check`에서는 실행되지 않습니다.

```powershell
.\gradlew.bat '-Dgighub.database.config=C:/absolute/path/to/KB PJT/backend/config/database-local.properties' databaseTest
```

성공하면 Test 출력에서 다음 형식의 결과를 확인할 수 있습니다. 행 수가 `0`이어도 테이블 조회에는 성공한
것입니다.

```text
Connected database: kb_pjt, users table rows: 0
```

## 검증 명령

`backend` 폴더를 IntelliJ 프로젝트로 열었다면 IntelliJ Terminal에서 실행합니다.

```powershell
.\gradlew.bat clean check
.\gradlew.bat war
.\gradlew.bat javadoc
```

생성 결과:

- WAR: `build/libs/gig-hub.war`
- Test: `build/reports/tests/test/index.html`
- Checkstyle: `build/reports/checkstyle/`
- Javadoc: `build/docs/javadoc/index.html`

## 다음 구현 순서

1. Tomcat 9.0.118에 빈 WAR를 배포하고 `/api/health` 응답을 확인합니다.
2. 실제 MySQL을 사용하는 최소 Mapper·Transaction 통합 테스트를 추가합니다.
3. Session 기반 Spring Security와 로그인 API를 추가합니다.
4. `work` 도메인부터 Controller → Service → Mapper 수직 흐름을 하나 완성합니다.

비밀번호, API Key, 실제 개인정보와 환경별 로컬 경로는 소스에 하드코딩하지 않습니다.
