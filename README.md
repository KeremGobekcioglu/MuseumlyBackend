# Museumly Backend

Spring Boot backend for the Museumly app (Compose Multiplatform client). It handles Google sign-in, issues its own JWTs, and is being extended to hold the Met artwork catalogue and per-user sync data.

**Stack:** Java 21, Spring Boot 4.1.1, PostgreSQL 17, Flyway, Spring Security (OAuth2 resource server, HS256 JWT), springdoc-openapi, Testcontainers.

## Status

| Area | State |
|---|---|
| Google sign-in (`POST /auth/google`) | Done, tested |
| JWT issue and verification (stateless, 30-day TTL) | Done, tested |
| `GET /me` | Done |
| Request logging with user id in the log pattern | Done |
| CI (tests on every push/PR) and SSH deploy on `main` | Done |
| `artworks` table (Flyway `V3`) | Done, on `metCrawler` branch |
| Met Open Access CSV import into `artworks` | Done, **off by default**, on `metCrawler` branch |
| Worker that fills in Met image URLs | Not started |
| Favorites / feed-position sync | Not started |
| Profile fields (name, photo), sign-out, account deletion | Not started |
| Notifications and daily pick | Not started |

## API

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/google` | none | Body `{ "idToken": "..." }`. Verifies the Google ID token, creates the user on first login, returns `{ "accessToken": "..." }`. |
| GET | `/me` | Bearer JWT | Returns `id`, `email`, `createdAt` of the current user. |
| GET | `/actuator/health` | none | Health check. |

Everything else requires a valid Bearer token. Auth errors (missing, invalid, or expired token) come back as RFC 9457 `ProblemDetail` JSON. Interactive API docs are served by springdoc.

## Database

Schema is managed by Flyway (`src/main/resources/db/migration`); Hibernate only validates it (`ddl-auto: validate`).

- `V1__init.sql`: baseline.
- `V2__create_users_table.sql`: `users` (UUID id, unique `google_sub`, `email`, `created_at`).
- `V3__create_artworks_table.sql`: `artworks`, unique on `(source, source_id)`. Catalogue columns are filled by the importer. `status` (`DISCOVERED`, `ACCEPTED`, `REJECTED`, `FAILED`), `attempts`, `next_attempt_at`, `last_error`, `image_url` and `image_small_url` are reserved for the image worker.

## Met import

`MetCsvImportRunner` reads the Met Open Access `MetObjects.csv` and upserts rows in batches of 1000. It keeps only public-domain objects in these departments: European Paintings, European Sculpture and Decorative Arts, Islamic Art, Asian Art. Re-running is safe; existing rows are updated by `(source, source_id)`.

It is disabled by default. To run it once:

```
MUSEUMLY_IMPORT_MET_ENABLED=true
MUSEUMLY_IMPORT_MET_CSV_PATH=MetObjects.csv   # optional, relative to the working dir
```

If the file is missing or unreadable, the runner logs a warning and skips. `MetObjects.csv` is gitignored (about 340 MB); download it from the Met's Open Access repository.

The next step is a slow background worker that walks `DISCOVERED` rows, calls the Met API for image URLs, and moves each row to `ACCEPTED`, `REJECTED` or `FAILED` with retry/backoff.

## Running locally

Requirements: JDK 21, Docker (for Postgres via `compose.yaml` and for tests).

```
export JWT_SECRET=$(openssl rand -base64 32)   # base64, at least 32 bytes
./mvnw spring-boot:run
```

`spring-boot-docker-compose` starts the Postgres container from `compose.yaml` automatically. `JWT_SECRET` is required; the app fails at startup without it or if it is not valid base64.

Tests (`./mvnw verify`) use Testcontainers and a test-only secret from `src/test/resources/config/application.yaml`.

## Configuration

| Property / env var | Purpose |
|---|---|
| `JWT_SECRET` | Base64 HMAC key for signing JWTs. Never commit. |
| `jwt.ttl` | Token lifetime (default `30d`). |
| `google.oauth.client-id` | Expected audience of Google ID tokens. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Datasource under the `prod` profile. |
| `PORT` | Server port under `prod` (default 8080). |
| `MUSEUMLY_IMPORT_MET_*` | Met CSV import switches, see above. |

## Deployment

Pushes to `main` run the test job, then the deploy job SSHes to the server and triggers the deploy. The `Dockerfile` builds a JRE-only image that runs as a non-root user; the `prod` profile disables Docker Compose support.

## Layout

```
controller/   AuthController, UserController
service/      AuthService, UserService
security/     SecurityConfig, jwt/, google/, error/, logging/
entity/ repository/ dtos/ exception/
parser/       Met CSV importer (runner, mapper, row, writer)
```
