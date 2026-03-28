# Agent Instructions

## Key Commands

- Format + build + test: `./gradlew`
- Test (no containers): `./gradlew test`
- Test (with containers): `./gradlew test -Pcontainers.enabled`
- Auto-format: `./gradlew spotlessApply`

Requires JDK 25 and Docker (for Testcontainers). Always run `./gradlew spotlessApply build` before finishing.

## Coding Guidelines

- No wildcard imports in Kotlin.
- All dependency versions in `gradle/libs.versions.toml`.
- Spotless enforces formatting - never manually reformat.

## Test Guidelines

- **Naming:** `givenThis_whenThis_thenThis` - mandatory. Ignore for `@ArchTest` as `ktlint` doesn't support it.
- **Assertions:** AssertJ only.
- **Delays:** Awaitility (`await().pollDelay(...).until(...)`) - never `Thread.sleep`.
- Testcontainers tests are skipped unless `-Pcontainers.enabled` is passed.
- **Scope:** Test both happy path and failure modes.
- **Execution:** Run tests once, save output to `build/test-run.log` inside the repo (`> build/test-run.log 2>&1`), then
  read from that file to extract errors. Never run the same test command multiple times, without changes in sources. You
  can store test output in multiple files if you want to compare before/after changes (ex. `build/test-run-{i}.log`).

## Integration Test Guidelines

- **Setup:**
  - Inject required JPA repositories for direct DB access in setup/teardown.
  - Depending on tested feature, select `@SpringBootTest`, `PostgresAwareTest`, `KafkaAwareTest`, and/or
    `@AutoConfigureRestTestClient` (this list may evolve).
- **Isolation:** `deleteAll()` in `@BeforeAll` and/or `@AfterEach`; seed data via JPA repository in `@BeforeEach`.

## Agent Instructions

- Read files before editing; match existing style.
- Validate with `get_errors` after every edit and fix issues.
- Minimal, focused changes - avoid touching unrelated code.
- Delegate to specialized agents (CVE Remediator, Plan) when applicable.
- Never use `Thread.sleep` in tests - use `Awaitility`.
- Never add dependency versions outside the version catalog.
- Read files using IDE features instead of shell commands.

## Troubleshooting

- Formatting failures -> `./gradlew spotlessApply`
- Testcontainers not running -> Docker must be running + `-Pcontainers.enabled`
- Dependency issues -> `./gradlew --refresh-dependencies`
