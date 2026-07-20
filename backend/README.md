# Gig Hub Backend

Spring Framework Legacy, MyBatis, MySQL 기반의 WAR 애플리케이션입니다. Spring Boot와 JPA는 사용하지 않으며 외부 Tomcat 9.0.118에 배포합니다.

## 현재 상태

- Gradle Java 17 + WAR 프로젝트
- Java Config 기반 Root Context·Servlet Context 초기화
- `GET /api/health` 최소 API와 MockMvc 테스트
- Checkstyle 및 Javadoc 생성 설정
- DB, Session Security, 실제 도메인 기능은 TODO 상태

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
2. 환경변수 이름과 `.env.example`을 합의한 뒤 HikariCP·MyBatis·Transaction 설정을 추가합니다.
3. Session 기반 Spring Security와 로그인 API를 추가합니다.
4. `work` 도메인부터 Controller → Service → Mapper 수직 흐름을 하나 완성합니다.

비밀번호, API Key, 실제 개인정보와 환경별 로컬 경로는 소스에 하드코딩하지 않습니다.

