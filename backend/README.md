# Gig Hub Backend

Spring Framework Legacy, MyBatis, MySQL 기반의 WAR 애플리케이션입니다. Spring Boot와 JPA는 사용하지 않으며 외부 Tomcat 9.0.118에 배포합니다.

## 패키지 구조

주요 도메인과 공통 패키지는 다음과 같습니다. 실제 구현 상태와 세부 계층은 코드와 테스트를 기준으로 확인합니다.

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

## 로컬 DB와 Tomcat 연결

Docker MySQL 시작, Flyway Migration, `database-local.properties`, 실제 DB 연결 테스트와 Tomcat VM option은 [DB Runbook](../docs/runbooks/DATABASE_RUNBOOK.md)을 따릅니다.

애플리케이션은 Flyway Migration을 자동 실행하지 않습니다. Tomcat을 시작하기 전에 Runbook의 `migrate`와 `validate`를 완료하며, 비밀번호와 로컬 절대 경로는 Git에 커밋하지 않습니다.

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

비밀번호, API Key, 실제 개인정보와 환경별 로컬 경로는 소스에 하드코딩하지 않습니다.
