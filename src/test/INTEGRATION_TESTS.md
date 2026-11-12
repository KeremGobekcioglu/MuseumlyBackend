# Integration tests: note for future me

These tests start the real Spring context against a throwaway Postgres 17 (Testcontainers), so **Docker must be running**.

Run all: `./mvnw test` (needs `JWT_SECRET` env var set, or use the one from `src/test/resources/config/application.yaml`).
Run one: `./mvnw test -Dtest=MetCsvImportIntegrationTest`

| Class | What it covers |
|---|---|
| `MetCsvImportIntegrationTest` | CSV importer: filtering, date cleanup, upsert keeps worker state |
| `MetWorkerPipelineIntegrationTest` | `MetEnrichmentWorker.tick()` with real picker/writer/DB, Met HTTP client mocked |
| `AuthEdgeCasesIntegrationTest` | Login creates one user row; deleted-user, expired, forged, missing token; blank idToken |
| `SchemaConstraintsIntegrationTest` | Flyway applied, `status` CHECK, unique `(source, source_id)`, unique `google_sub` |
| `AuthFlowTest`, `HealthCheckTest`, `MetEnrichmentWriterTest`, `MetCandidatePickerTest` | Older integration tests (happy-path login, health, writer SQL, picker SQL) |

## How to extend

- **New DB test:** copy the header of `SchemaConstraintsIntegrationTest` (`@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` + `@Transactional`); rows roll back after each test.
- **New HTTP test:** add `@AutoConfigureMockMvc`, autowire `MockMvc`, and `@MockitoBean` the Google client (see `AuthEdgeCasesIntegrationTest`).
- **Not `@Transactional` with MockMvc:** requests commit, so use unique emails/subs (`UUID.randomUUID()`).
- **New migration:** bump the expected count in `SchemaConstraintsIntegrationTest.allFlywayMigrationsAppliedSuccessfully`.
- **Worker is `@ConditionalOnProperty`** (off in tests), so build it by hand: `new MetEnrichmentWorker(target, 0L, picker, client, writer)`.
- **A failed SQL statement aborts the Postgres transaction:** keep it as the last statement of the test.
- `TestcontainersConfiguration` is package-private, so new tests live in `com.kg.museumly.backend` (or make it public first).
