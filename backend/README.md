# Backend

Spring Framework legacy, MyBatis 기반 백엔드 프로젝트 위치입니다.

## 예정 구조

```text
backend/
  build.gradle
  settings.gradle
  src/
  config/
    checkstyle/
      checkstyle.xml
```

## 실행 / 빌드

Gradle 프로젝트 생성 후 아래 명령을 사용합니다.

```sh
# Windows
.\gradlew.bat -p backend check
.\gradlew.bat -p backend war

# macOS / Linux
./gradlew -p backend check
./gradlew -p backend war
```

Gradle wrapper를 `backend/` 안에 둘 경우에는 `backend` 폴더에서 실행합니다.

```sh
# Windows
.\gradlew.bat check

# macOS / Linux
./gradlew check
```

## Lint

백엔드 lint는 Gradle `check` 태스크에서 Checkstyle을 실행하는 방식으로 관리합니다.

```sh
npm run lint:be
```

