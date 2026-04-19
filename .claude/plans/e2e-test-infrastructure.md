# Feature: E2E Test Infrastructure

## Goal

Establish a test infrastructure for Bookie that lets us write near-E2E tests — tests that drive the application through a real browser the same way a user does — while keeping per-test execution time close to unit-test speeds. Slow/external dependencies (repositories, eventually any real I/O) are stubbed so tests don't pay their cost. Tests are parallelizable at the suite (class) level so the suite continues to scale as the app grows.

## Key Decisions

- **Minimal Spring context per test class, not full `@SpringBootTest` (Option B)**: each test class builds its own small `AnnotationConfigApplicationContext` declaring only fake repositories, the real `EventBus`, `SessionRegistry`, screens, and the real `Router` imported directly. Skips autoconfiguration and classpath scanning — context boot drops from ~1-2s to a few hundred ms. *Alternative considered*: full `@SpringBootTest` with `@TestConfiguration` (Option A) — rejected because contexts are heavy (~150MB each) and parallelism multiplies memory quickly. *Tradeoff accepted*: divergence from prod wiring (e.g. missed route registration, `@Value` setup, `@Scheduled` toggles). Mitigated by importing `Router` directly so at least the route graph stays in sync.

- **No smoke tier for now**: the divergence risk is small while Bookie is small. We'll revisit if Option B starts missing real bugs.

- **Per-test-class isolation, class-level parallelism**: each class builds its own context in `@BeforeAll` on its own random port, with its own fake state. JUnit 5 runs classes in parallel via `junit-platform.properties`. Methods within a class stay sequential (they share the class-scoped state and browser). This lets us scale out test count without rearchitecting the app's shared-state model (singleton repos, singleton `SessionRegistry`).

- **Playwright, not Selenium**: better auto-waiting for DOM mutations — the core primitive of a DataStar UI — plus a single dependency, cleaner API, and faster startup. Alternative considered: Selenium + WebDriverWait; rejected as more boilerplate for SSE-driven UIs.

- **Shared Chromium per worker thread, new `BrowserContext` per test**: `Playwright` + `Browser` held in a `ThreadLocal` (or equivalent) so each JUnit parallel worker has its own Chrome process. Each test gets `browser.newContext()` (cheap, ~10-30ms) for cookie/localStorage isolation, and a fresh `Page`. Avoids Chrome-per-test cost without fighting Playwright's threading model.

- **Interface extraction for `BondRepository` and `TradeRepository` only**: these are the slow ones (`@PostConstruct` DB-load sleep, per-query sleeps). `ReferenceDataRepository` is trivial static data — use the real class in tests. Shared logic (validation, etc.) lives as `static` methods or `default` methods on the interfaces so fakes only implement CRUD-style operations and anything that genuinely has to differ in tests.

- **`SessionRegistry.clearAll()` method added to the real class**: simplest reset hook, called in `@BeforeEach`. Harmless in prod. Alternative: test-only subclass — rejected as more code for no benefit.

- **No `@EnableScheduling` in test context**: the only scheduled work is `SessionRegistry.cleanup()`, which we don't need in tests since `clearAll()` handles reset. We lose test coverage of the cleanup path; acceptable for now.

- **New dependencies, test scope only**: `com.microsoft.playwright:playwright` and `org.springframework.boot:spring-boot-starter-test` (JUnit 5 + AssertJ + Mockito). CLAUDE.md's "ask before adding dependencies" cleared.

## Implementation Steps

1. **Add test dependencies** — add `com.microsoft.playwright:playwright` and `org.springframework.boot:spring-boot-starter-test` to `build.gradle.kts` under `testImplementation`. Verify: `./gradlew build` compiles with no tests present.

2. **Extract `BondRepository` interface** — rename the existing class to `DefaultBondRepository` implementing a new `BondRepository` interface. Shared logic (validators, `isValid`, `createXxxEntry` helpers that aren't I/O-dependent) moves onto the interface as `default` or `static` methods so fakes inherit them. Verify: app still runs, existing flows (Trades, Positions, Securities) unchanged.

3. **Extract `TradeRepository` interface** — look at how BondRepository / BondDAO / DefaultBondDAO works and do likewise. TradeDAO should be minimal, only containing crud ops that we need to override in tests.

4. **Add `SessionRegistry.clearAll()`** — synchronized method that disposes all sessions and clears the map. Verify: unit-testable behaviorally; the app still runs normally.

5. **Write `FakeBondDAO` and `FakeTradeDAO`** under `src/test/java`. In-memory maps, builder-style `with…` helpers for seeding from tests, `clear()` for per-test reset, no sleeps.

6. **Write `TestAppConfig`** — a plain `@Configuration` declaring `EventBus`, `SessionRegistry`, fake repositories, real `ReferenceDataRepository`, and `@Import(Router.class)` to reuse the prod route graph. Include a `PropertySourcesPlaceholderConfigurer` bean and a test property source for `bookie.version`, `bookie.cache.enabled`, `bookie.session.timeout-seconds`. Verify: an `AnnotationConfigApplicationContext` built from this config boots without errors and exposes the expected beans.

7. **Write `JettyBootstrap` helper + `BookieE2ETest` base class** — bootstrap wires a `DispatcherServlet` against the Spring context and starts Jetty on a random port. Base class handles `@BeforeAll` context/Jetty startup, `@AfterAll` teardown, `@BeforeEach` state reset. Include a trivial smoke test that asserts the server responds on the root redirect. Verify: smoke test passes; teardown leaves no live thread.

8. **Configure JUnit 5 parallelism** — add `src/test/resources/junit-platform.properties` with `parallel.enabled=true`, `parallel.mode.classes.default=concurrent`, `parallel.mode.default=same_thread`, and a reasonable `parallel.config.dynamic.factor`. Verify: duplicating the smoke test into two classes and running together shows them executing on different threads (log a thread name or port).

9. **Add Playwright helper + first real test** — a per-worker `Browser` manager (e.g. `ThreadLocal<Browser>` closed on JVM shutdown) and a JUnit extension (or fields on the base class) that provides a fresh `BrowserContext` and `Page` per test. First test: `TradesScreenTest` navigates to `/trades` with one seeded trade and asserts the row renders with the expected CUSIP and description. Verify: test passes; run it alongside the smoke test class and confirm they parallelize.

## Out of Scope

- Load/perf testing and benchmarking the suite.
- Cross-browser coverage (Firefox, WebKit) — Chromium only.
- Visual regression / screenshot diffing.
- Mocking DataStar frontend behavior — we drive a real browser.
- Full smoke tier with real `@SpringBootApplication` boot (deferred until Option B divergence shows a real bug).
- Test coverage of the `@Scheduled` session cleanup path (disabled in test contexts).
- Reference-data variations — `ReferenceDataRepository` used as-is.

## Open Questions

- Exact ergonomics of the Playwright per-worker lifecycle (`ThreadLocal` vs. JUnit `Extension` vs. static fields on the base class) — will be chosen during execution based on what reads cleanest.
- Whether state reset lives on the base class `@BeforeEach` method or as a JUnit extension — same, decide during execution.
- Whether `ClientChannel` / SSE updates behave correctly end-to-end in a Playwright-driven Chromium — expected to, but first real test will confirm. If not, may need to tune heartbeat or `bookie.cache.enabled` settings in test props.
- Interaction between class-level parallelism and shared Chromium per worker — confirm during step 9 that two parallel test classes routed to the same worker thread don't race on browser state.
