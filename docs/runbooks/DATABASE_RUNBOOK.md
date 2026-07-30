# 로컬 데이터베이스 실행 Runbook

## 목적과 현재 기준

이 문서는 Docker Compose로 MySQL을 실행하고 Flyway Migration을 적용한 뒤 Spring·MyBatis 연결까지 확인하는 팀 공통 절차입니다. 로컬 개발 DB의 시작, 검증, 중지와 장애 대응에 사용합니다.

| 항목                | 현재 기준                           |
| ------------------- | ----------------------------------- |
| 문서 상태           | 현재 기준                           |
| 마지막 실제 검증    | 2026-07-30                          |
| MySQL               | `mysql:8.4.10`                      |
| Flyway CLI          | `flyway/flyway:12.9.0`              |
| MySQL Connector/J   | `9.7.0`                             |
| 현재 Migration Head | `202607301152`                      |
| 도메인 테이블       | 23개 (`flyway_schema_history` 제외) |

실행 코드와 이 문서가 다르면 다음 원본을 우선합니다.

| 대상                                | 단일 원본                                                     |
| ----------------------------------- | ------------------------------------------------------------- |
| 컨테이너·포트·볼륨·Flyway 실행 설정 | 루트 `compose.yaml`                                           |
| DB 스키마                           | `backend/src/main/resources/db/migration/V*.sql`              |
| JDBC·MyBatis·트랜잭션 설정          | `backend/src/main/java/com/gighub/config/DatabaseConfig.java` |
| DB 라이브러리 버전과 검증 작업      | `backend/build.gradle`                                        |
| 스키마의 작업용 요약                | [`../agent/SCHEMA_OVERVIEW.md`](../agent/SCHEMA_OVERVIEW.md)  |

Spring 애플리케이션은 Flyway를 자동 실행하지 않습니다. Migration은 별도 Flyway 컨테이너가 적용하며, 애플리케이션은 호스트에서 외부 설정 파일을 읽어 MySQL에 연결합니다.

```text
V*.sql ──> Flyway 컨테이너 ── db:3306 ──> MySQL 컨테이너 ──> mysql-data 볼륨
Spring/Tomcat(호스트) ── localhost:${MYSQL_PORT} ────────────────┘
```

## 사전 조건

- Docker Desktop의 Linux 컨테이너 엔진이 실행 중이어야 합니다.
- JDK 17을 사용합니다.
- Node.js와 npm을 사용합니다.
- 최초 실행 시 Docker 이미지와 Gradle 의존성을 내려받을 네트워크 또는 이미 준비된 로컬 캐시가 필요합니다.
- 모든 명령은 저장소 루트의 PowerShell에서 실행합니다.
- 실제 비밀번호는 `.env`와 `backend/config/database-local.properties`에만 두고 Git에 추가하지 않습니다.

## 최초 1회 준비

로컬 파일이 없을 때만 예제를 복사합니다.

```powershell
Copy-Item .env.example .env
Copy-Item backend/config/database.example.properties backend/config/database-local.properties
```

두 파일에서 다음 값을 서로 맞춥니다.

| `.env`           | `database-local.properties`  |
| ---------------- | ---------------------------- |
| `MYSQL_DATABASE` | JDBC URL의 데이터베이스 이름 |
| `MYSQL_USER`     | JDBC 사용자                  |
| `MYSQL_PASSWORD` | JDBC 비밀번호                |
| `MYSQL_PORT`     | JDBC URL의 `localhost` 포트  |

호스트 애플리케이션의 JDBC 주소는 `localhost:${MYSQL_PORT}`를 사용하지만, Docker 네트워크 안의 Flyway는 항상 `db:3306`으로 연결합니다.

새 clone, Connector/J 버전 변경 또는 Gradle `clean` 실행 후에는 Flyway 컨테이너가 마운트할 JDBC Driver를 먼저 준비합니다.

```powershell
.\backend\gradlew.bat -p backend prepareFlywayDriver
```

정상 완료되면 다음 파일이 생성됩니다.

```text
backend/build/flyway-drivers/mysql-connector-j-9.7.0.jar
```

현재 `npm run db:migrate`에는 Driver 준비 작업이 포함되어 있지 않으므로 이 선행 단계를 생략하지 않습니다.

## 표준 시작과 Migration

### 1. Compose 설정 확인

```powershell
docker compose config --quiet
```

출력이 없이 종료 코드가 `0`이면 정상입니다.

### 2. MySQL 시작

```powershell
docker compose up -d db
docker compose ps
```

`db` 서비스가 `healthy`가 될 때까지 기다립니다.

### 3. 적용 전 상태 확인

```powershell
docker compose --profile tools run --rm flyway info
```

빈 DB라면 Migration이 `Pending`, 이미 적용한 DB라면 `Success`로 표시됩니다.

### 4. Migration 적용

```powershell
npm.cmd run db:migrate
```

현재 기준으로 다음 다섯 Migration이 순서대로 적용되어야 합니다.

| Version        | 파일                                                      |
| -------------- | --------------------------------------------------------- |
| `202607200001` | `V202607200001__create_gig_hub_baseline.sql`              |
| `202607211440` | `V202607211440__add_signup_and_workplace_schema.sql`      |
| `202607221300` | `V202607221300__support_contract_escrow_test_flow.sql`    |
| `202607301027` | `V202607301027__remove_invited_from_work_case_status.sql` |
| `202607301152` | `V202607301152__split_workplace_address.sql`              |

같은 명령을 다시 실행했을 때 `Schema ... is up to date. No migration necessary.`가 나오면 반복 실행도 정상입니다.

### 5. 이력과 파일 검증

```powershell
docker compose --profile tools run --rm flyway validate
docker compose --profile tools run --rm flyway info
```

현재 기준의 정상 결과는 다섯 Migration의 검증 성공, Schema version `202607301152`, 모든 항목의 `Success`입니다.

## Spring·MyBatis 연결 검증

절대경로를 자신의 저장소 위치로 바꾸고 `/` 구분자를 사용합니다. 경로에 공백이 있으므로 JVM 인수 전체를 큰따옴표로 감쌉니다.

```powershell
.\backend\gradlew.bat -p backend "-Dgighub.database.config=C:/absolute/path/to/KB PJT/backend/config/database-local.properties" databaseTest
```

예제의 기본 DB 이름을 사용한 정상 출력 형식은 다음과 같습니다. DB 이름을 변경했다면 첫 줄의 이름도 달라지며, `users` 행 수는 로컬 데이터에 따라 달라집니다.

```text
Connected database: kb_pjt, users table rows: N
BUILD SUCCESSFUL
```

일반 `npm run check`와 백엔드 기본 `test`는 `@Tag("database")` 테스트를 제외하므로 DB 연결 확인에는 `databaseTest`를 별도로 실행해야 합니다.

### 실제 Tomcat 실행

WAR를 실행하는 Tomcat에도 같은 설정 파일 경로가 필요합니다. IntelliJ의 Tomcat Run/Debug Configuration 또는 사용하는 Tomcat 실행 스크립트의 JVM 옵션에 다음 값을 추가합니다.

```text
-Dgighub.database.config="C:/absolute/path/to/KB PJT/backend/config/database-local.properties"
```

이 속성에는 비밀번호가 아니라 로컬 설정 파일의 절대경로만 넣습니다. 속성이 없거나 파일을 읽지 못하면 Spring Root Context가 생성되지 않아 애플리케이션이 시작되지 않습니다.

## 선택적 계약·에스크로 Seed

계약·에스크로 테스트 시나리오가 필요할 때만 다음 명령을 실행합니다.

```powershell
npm.cmd run db:seed:contract
```

이 명령은 미적용 Migration을 먼저 적용한 뒤 [`test-contract-escrow.sql`](../../backend/src/test/resources/db/seed/test-contract-escrow.sql)을 실행합니다. 로컬 DB의 합성 테스트 데이터만 대상으로 하며 공용 DB나 운영 DB에서는 실행하지 않습니다.

| 항목           | 초기 상태                                        |
| -------------- | ------------------------------------------------ |
| 사장님 로그인  | `test_owner_17` / `Test1234!`                    |
| 근로자 로그인  | `test_worker_17` / `Test1234!`                   |
| 근무·일급      | 2026-08-01 09:00~18:00, 무급 휴게 60분·300,000원 |
| 사장님 지갑    | 가용 700,000원, 잠금 300,000원                   |
| 근로자 지갑    | 가용 0원, 에스크로 확보액 300,000원              |
| 업무 처리 상태 | 근무 `ACCEPTED`, 에스크로 `HELD`, 정산 `WAITING` |

같은 명령을 다시 실행하면 전용 테스트 계정과 `[TEST-17]` 근무 건만 위 상태로 되돌립니다. 다른 사용자의 데이터는 삭제하지 않습니다. 전체 DB를 초기화하는 `docker compose down -v`나 Flyway `clean`을 이 Seed의 재실행 방법으로 사용하지 않습니다.

## 중지와 데이터 보존

```powershell
docker compose down
```

컨테이너와 네트워크는 제거되지만 `mysql-data` Named Volume의 데이터는 유지됩니다.

> [!CAUTION]
> `docker compose down -v`는 Named Volume과 로컬 DB 전체를 삭제합니다. 일반 시작·중지·오류 복구 절차로 사용하지 않습니다. 데이터 초기화가 별도 작업의 명시적 목적이고 삭제 대상을 확인한 경우에만 수행합니다.

## 스키마 변경 절차

1. 현재 Migration Head보다 큰 새 Version의 `V<version>__<description>.sql`을 추가합니다.
2. 이미 공유되었거나 적용된 Versioned Migration은 수정하거나 삭제하지 않습니다.
3. `prepareFlywayDriver`, `info`, `db:migrate`, `validate`, `info` 순서로 확인합니다.
4. 변경된 Mapper XML과 Service 트랜잭션의 관련 테스트를 실행합니다.
5. 같은 PR에서 [`../agent/SCHEMA_OVERVIEW.md`](../agent/SCHEMA_OVERVIEW.md)의 Head, 테이블 관계와 불변식을 갱신합니다.
6. 파괴적 변경, 수동 데이터 보정 또는 팀원이 수행할 작업이 있으면 PR과 사람용 안내에 명시합니다.

Checksum 불일치가 발생해도 `repair`를 먼저 실행하지 않습니다. 적용된 SQL이 변경되었는지 확인하고 원본을 복구한 뒤, 필요한 변경은 새 Migration으로 추가합니다.

## 문제 해결

| 증상                                   | 확인과 조치                                                                                             |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Docker API 또는 pipe 연결 오류         | Docker Desktop을 실행하고 Linux 엔진 준비가 끝났는지 확인합니다.                                        |
| `db`가 `unhealthy`                     | `docker compose logs --tail 100 db`로 초기화·인증 오류를 확인합니다.                                    |
| Flyway Driver 파일 또는 mount 오류     | `.\backend\gradlew.bat -p backend prepareFlywayDriver`를 다시 실행합니다.                               |
| 호스트 포트 충돌                       | `.env`의 `MYSQL_PORT`와 JDBC URL 포트를 함께 바꿉니다.                                                  |
| `.env` 비밀번호 변경 후 접근 거부      | MySQL 초기 계정값은 빈 Volume을 처음 만들 때만 적용됩니다. 기존 Volume의 자격 증명과 혼동하지 않습니다. |
| `users` 테이블이 없다는 DB 테스트 실패 | 먼저 `npm.cmd run db:migrate`를 실행합니다.                                                             |
| Flyway Checksum 오류                   | 적용된 Migration의 수정 여부를 확인하고 원본을 복구합니다. 즉시 `repair`하지 않습니다.                  |
| 애플리케이션 설정 파일 오류            | `-Dgighub.database.config` 절대경로와 필수 JDBC 속성을 확인합니다.                                      |

## 완료 기준

- `docker compose config --quiet`가 성공합니다.
- `db` 서비스가 `healthy`입니다.
- `db:migrate`를 두 번 실행해도 안전하며 두 번째 실행은 최신 상태를 보고합니다.
- `flyway validate`가 모든 Migration을 검증합니다.
- `flyway info`의 Head가 이 문서 및 Schema Overview와 일치합니다.
- `databaseTest`가 실제 로컬 MySQL 연결과 `users` 조회를 통과합니다.
- 비밀정보, 실제 개인정보와 실제 계좌정보가 Git, Seed와 로그에 포함되지 않습니다.
