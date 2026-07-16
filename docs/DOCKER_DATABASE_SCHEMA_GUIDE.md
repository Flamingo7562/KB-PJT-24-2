# Docker Compose 데이터베이스 스키마 공유 가이드

> 대상: Docker와 Flyway를 처음 사용하는 팀원
>
> 한 줄 원칙: **Docker Compose로 같은 MySQL을 실행하고, Git에 올린 Flyway SQL로 Schema를 공유합니다.**

## 1. 무엇을 공유하나요?

| 파일 | 역할 | Git 커밋 |
| --- | --- | --- |
| `compose.yaml` | 모든 팀원이 같은 MySQL·Flyway 환경을 실행 | O |
| `.env.example` | 필요한 환경변수 이름과 예시 | O |
| `.env` | 각자 사용할 포트와 비밀번호 | X |
| `db/migration/*.sql` | 테이블·컬럼 등 Schema 변경 이력 | O |
| Docker Named Volume | 각 팀원의 로컬 DB 데이터 | X |

DB 전체 Dump 파일을 주고받거나 각자 `ALTER TABLE`을 실행하지 않습니다. Schema 변경은 항상 새 Flyway SQL로 작성해 Git PR로 공유합니다.

## 2. 시작 전 확인

Docker Desktop을 설치하고 실행한 뒤 프로젝트 루트에서 확인합니다.

```sh
docker --version
docker compose version
```

두 명령에 버전이 표시되면 준비가 끝난 것입니다.

## 3. 팀 최초 1회 설정

이 단계는 DB 환경을 처음 만드는 담당자 한 명만 수행합니다. 권장 구조는 다음과 같습니다.

```text
KB-PJT-24-2/
  compose.yaml
  .env.example
  backend/
    src/main/resources/
      db/migration/
        V202607161300__create_users.sql
```

### 3.1 `compose.yaml`

아래 버전은 2026-07-16 기준 실행 예시입니다. 팀에서 JDK·Spring과의 호환성을 확인한 뒤 버전을 한 번 확정하고, 팀원 개인이 임의로 바꾸지 않습니다.

```yaml
services:
  db:
    image: mysql:8.4.10
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Seoul
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      - --default-time-zone=+09:00
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -p\"$${MYSQL_ROOT_PASSWORD}\""]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 20s

  flyway:
    image: flyway/flyway:12.9.0
    profiles: ["tools"]
    environment:
      FLYWAY_URL: jdbc:mysql://db:3306/${MYSQL_DATABASE}
      FLYWAY_USER: ${MYSQL_USER}
      FLYWAY_PASSWORD: ${MYSQL_PASSWORD}
      FLYWAY_CONNECT_RETRIES: 30
      FLYWAY_VALIDATE_MIGRATION_NAMING: "true"
    volumes:
      - ./backend/src/main/resources/db/migration:/flyway/sql:ro
    depends_on:
      db:
        condition: service_healthy

volumes:
  mysql-data:
```

### 3.2 `.env.example`

아래 값은 로컬 개발용 예시이며 실제 비밀번호가 아닙니다.

```dotenv
MYSQL_PORT=3306
MYSQL_DATABASE=kb_pjt
MYSQL_USER=kb_pjt_app
MYSQL_PASSWORD=change-me-local
MYSQL_ROOT_PASSWORD=change-me-root
```

실제 값이 들어 있는 `.env`는 커밋하지 않습니다.

### 3.3 첫 Migration SQL

파일명 형식은 `V연월일시분__영문_설명.sql`입니다. 같은 시각을 피하고, 설명의 공백은 `_`로 씁니다.

```sql
-- backend/src/main/resources/db/migration/V202607161300__create_users.sql
CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

위 SQL은 작성법을 보여 주는 예시입니다. 실제 테이블과 컬럼은 확정된 ERD에 맞춥니다.

### 3.4 최초 실행과 공유

Windows PowerShell에서는 다음과 같이 `.env.example`을 복사합니다.

```powershell
Copy-Item .env.example .env
```

그다음 설정, DB, Migration을 차례로 확인합니다.

```sh
docker compose config --quiet
docker compose up -d db
docker compose run --rm flyway migrate
docker compose run --rm flyway info
```

`migrate`가 성공하고 `info`에 Migration 상태가 `Success`로 표시되면 아래 파일만 PR에 올립니다.

```text
compose.yaml
.env.example
backend/src/main/resources/db/migration/*.sql
```

`.env`와 로컬 DB 데이터는 올리지 않습니다.

## 4. 팀원이 받은 뒤 실행

최신 코드를 받은 뒤 최초 한 번만 `.env`를 만듭니다.

```sh
git pull
```

```powershell
Copy-Item .env.example .env
```

이미 `.env`가 있다면 다시 복사하지 않습니다. `3306` 포트를 다른 프로그램이 사용 중이면 `.env`의 `MYSQL_PORT`만 `3307`처럼 바꿉니다.

DB를 실행하고 아직 적용되지 않은 Migration을 적용합니다.

```sh
docker compose up -d db
docker compose run --rm flyway migrate
docker compose run --rm flyway info
```

DB 접속 정보는 다음과 같습니다.

```text
Host: localhost
Port: .env의 MYSQL_PORT
Database: .env의 MYSQL_DATABASE
User: .env의 MYSQL_USER
Password: .env의 MYSQL_PASSWORD
```

작업을 멈출 때는 다음 명령을 사용합니다. DB 데이터는 Named Volume에 남습니다.

```sh
docker compose down
```

## 5. Schema를 변경해서 다시 공유하기

예를 들어 `users`에 `nickname` 컬럼을 추가한다면 기존 SQL을 수정하지 않고 새 파일을 만듭니다.

```sql
-- V202607161430__add_nickname_to_users.sql
ALTER TABLE users
    ADD COLUMN nickname VARCHAR(50) NULL AFTER email;
```

표준 순서는 다음과 같습니다.

1. 최신 코드를 받습니다.
2. 새 버전의 Migration SQL을 추가합니다.
3. `docker compose run --rm flyway migrate`로 기존 DB에 적용합니다.
4. `docker compose run --rm flyway validate`로 이력을 검증합니다.
5. 변경 SQL과 검증 결과를 PR에 포함합니다.
6. 다른 팀원은 머지된 코드를 받은 뒤 `migrate`만 다시 실행합니다.

이미 한 번 적용했거나 Git에 공유한 Migration은 수정·삭제하지 않습니다. 잘못된 내용은 더 높은 버전의 새 SQL로 고칩니다.

## 6. 자주 쓰는 명령

| 목적 | 명령 |
| --- | --- |
| DB 시작 | `docker compose up -d db` |
| 상태 확인 | `docker compose ps` |
| DB 로그 확인 | `docker compose logs db` |
| 새 Migration 적용 | `docker compose run --rm flyway migrate` |
| 적용 이력 확인 | `docker compose run --rm flyway info` |
| Migration 검증 | `docker compose run --rm flyway validate` |
| DB 중지 | `docker compose down` |
| 로컬 DB 완전 초기화 | `docker compose down -v` |

> **주의:** `docker compose down -v`는 Named Volume과 그 안의 로컬 DB 데이터를 모두 삭제합니다. 정말 처음부터 다시 만들 때만 사용합니다.

## 7. 문제 해결

- **포트가 이미 사용 중임:** `.env`의 `MYSQL_PORT`를 `3307`처럼 변경합니다.
- **DB 연결이 안 됨:** `docker compose ps`와 `docker compose logs db`로 DB가 `healthy`인지 확인합니다.
- **Checksum 오류가 남:** 이미 적용한 Migration이 수정된 것입니다. 기존 파일을 원래대로 되돌리고 새 Migration을 추가합니다.
- **`.env` 값을 바꿨는데 DB 계정이 그대로임:** MySQL 초기 환경변수는 빈 데이터 볼륨에서만 적용됩니다. 로컬 데이터를 지워도 되는 경우에만 `docker compose down -v` 후 다시 실행합니다.
- **내 DB에서는 되는데 팀원 DB에서는 안 됨:** 수동으로 만든 테이블·컬럼이 없는지 확인하고, 필요한 변경을 새 Migration SQL에 넣습니다.

## 8. PR 체크리스트

- [ ] Migration 파일명이 `V연월일시분__영문_설명.sql` 형식인가?
- [ ] 기존에 공유된 Migration을 수정하지 않았는가?
- [ ] `migrate`와 `validate`가 성공했는가?
- [ ] 변경한 테이블을 사용하는 MyBatis SQL도 확인했는가?
- [ ] `.env`, 실제 개인정보, 운영 데이터 Dump가 포함되지 않았는가?

## 참고 자료

- [Docker Compose 시작 가이드](https://docs.docker.com/compose/gettingstarted/)
- [Docker Compose Volume 문서](https://docs.docker.com/reference/compose-file/volumes/)
- [MySQL 공식 Docker 이미지](https://hub.docker.com/_/mysql)
- [Flyway Versioned Migration](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)
- [Flyway Validate](https://documentation.red-gate.com/flyway/reference/commands/validate)
