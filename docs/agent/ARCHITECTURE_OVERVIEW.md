# Architecture Overview

Read this short overview once before material implementation for an issue or branch. Use `docs/README.md` only when deeper task-specific guidance is needed.

## Runtime shape

- The repository is a monorepo: `frontend/` contains the browser application, `backend/` contains the server application, and root scripts coordinate shared checks and local infrastructure.
- The frontend is a JavaScript-only Vue 3 and Vite application. Vue Router owns navigation, Pinia owns client state, and `src/services/` owns HTTP access through Axios.
- The backend is a Java 17 WAR for Tomcat 9 using Spring Framework 5 without Spring Boot. Annotation-based configuration separates the Root Context from the Spring MVC Servlet Context.
- Backend domain and business flows follow `controller -> service -> mapper`; MyBatis mapper interfaces call SQL in `backend/src/main/resources/mappers/`.
- MySQL 8.4 runs through Docker Compose. Flyway SQL under `backend/src/main/resources/db/migration/` is the schema source of truth.
- Docker Compose provides MySQL and opt-in Flyway or seed tools only; run the Vue application and Tomcat WAR separately.

## Change boundaries

- Frontend pages live in `src/views/`, reusable UI in `src/components/`, state in `src/stores/`, routes in `src/router/`, and backend calls in `src/services/`.
- Backend code is grouped by domain under `com.gighub`. Keep web, business, and persistence responsibilities in their existing layers and use explicit DTOs at API boundaries.
- `AppInitializer`, `RootConfig`, `WebMvcConfig`, and `DatabaseConfig` are the current backend wiring entrypoints.
- The frontend HTTP client defaults to `/api` and can override its base URL with `VITE_API_BASE_URL`. During local Vite development, `DEV_PROXY_TARGET` changes only the target of the `/api` proxy.
- A frontend service or domain package name does not prove end-to-end implementation. Check the affected service's `USE_MOCK` path and a matching backend controller before changing a flow.
- Schema changes belong in a new Flyway migration; update the schema overview and affected mapper behavior in the same change.

## Authoritative sources

- Application dependency and runtime versions: `frontend/package.json` and `backend/build.gradle`.
- Container image versions and local infrastructure: `compose.yaml`.
- Frontend routes and request behavior: `frontend/src/router/index.js`, `frontend/src/services/http.js`, and the affected `frontend/src/services/*.js`.
- Backend wiring and layer boundaries: `backend/src/main/java/com/gighub/config/` and current domain packages.
- Database structure: Flyway migrations first, then `docs/agent/SCHEMA_OVERVIEW.md` for compact context.
- Task-specific guides and runbooks: `docs/README.md`.

Update this overview only when a top-level runtime, directory responsibility, request path, persistence boundary, or authoritative source changes. Keep feature details in task-specific documents.
