# Feature: Stress-Test Trade Generation

## Goal
Add a configurable stress-test mode that makes the app continuously book trades at a fixed rate, uniformly spread across each second. Controlled by a new property `bookie.stress.trades.per.second` (default `0` = off). When non-zero, the initial 1000-trade seed is skipped and instead the app perpetually generates random trades at the configured rate, retrying until each generated trade is valid before booking it. Reuses the existing random trade generator in `TradeRepository` so stress-mode trades look like normal seed trades.

## Key Decisions

- **Property injection via `@Value` field on `TradeRepository`** — matches the field-injection style already used in `Router.java:35` and `BaseScreen.java:19`, and avoids further growing the existing 5-arg constructor. Alternative considered: constructor injection (as in `SessionRegistry.java:27`) — rejected to keep the constructor small.
- **Stress mode replaces the seed, does not follow it** — when `stressTradesPerSecond > 0`, the initial `while (count < TRADE_COUNT)` loop does not run. The book starts empty and stress generation begins immediately (after the existing 1000ms fake DB sleep).
- **Still gated by `generateFakeData`** — the existing `setGenerateFakeData(false)` escape hatch (used by tests) must continue to disable both paths. Stress mode lives inside the same `if (!generateFakeData) return;` guard.
- **Retry-until-valid per slot** — each pacing slot must produce a booked trade. If the random trade is invalid, regenerate and retry within the same slot (do not sleep, do not skip). Differs from the current seed behavior which silently skips invalid trades. Same `Random(42)` seed is kept for determinism.
- **Pacing via `LockSupport.parkNanos(1_000_000_000L / rate)`** between bookings — simplest approximation of "uniformly distributed across the second". At 1000/s this is a 1ms park, well within sleep granularity. Single virtual thread (the one already spawned by `init()`).
- **Runs forever** — no stop condition; the perpetual virtual thread lives for the lifetime of the app.
- **Extract `buildRandomTrade(...)`** — pull the per-trade construction body out of `generate()` into a helper so both the seed path and the stress path share one generator. Keeps the RNG sequence identical to today's behavior when stress is off.
- **Default value lives in both `application.properties` and the `@Value` default** — explicit `bookie.stress.trades.per.second=0` in `application.properties` so the property is visible and discoverable, plus `:0` default in the `@Value` annotation as a safety net.

## Implementation Steps

1. **Extract `buildRandomTrade` helper in `TradeRepository.java`** — pure refactor, no behavior change.
   - Pull the per-trade construction block from inside the `while` loop of `generate(...)` (lines ~202–218) into a new `private Trade buildRandomTrade(Random rng, List<Bond> bonds, List<String> books, List<String> counterparties, LocalDate today)` that returns a fully populated `Trade`.
   - `generate(...)` now calls `buildRandomTrade(...)` inside its existing loop; the `isValid` check and `bookNewTrade` call stay in `generate`.
   - Verify: start the app without setting the stress property; confirm 1000 seed trades are still generated identically (same CUSIPs, same positions — RNG sequence unchanged).

2. **Add stress-mode property and generation path in `TradeRepository.java`**, and register the property in `application.properties`.
   - Add field `@Value("${bookie.stress.trades.per.second:0}") private int stressTradesPerSecond;`.
   - Add a new `private void runStressLoop(List<Bond> bonds, List<String> books, List<String> counterparties, LocalDate today)` that creates `Random rng = new Random(42)`, computes `long delayNanos = 1_000_000_000L / stressTradesPerSecond`, and runs `while (true)`:
     - Inner loop: `do { trade = buildRandomTrade(...); } while (!isValid(trade));`
     - `bookNewTrade(trade);`
     - `LockSupport.parkNanos(delayNanos);`
   - In `generate(...)`, after the `if (!generateFakeData) return;` guard and after loading `bonds`/`books`/`counterparties`/`today`, branch: if `stressTradesPerSecond > 0` call `runStressLoop(...)` (and return — it never returns); otherwise run the existing seed loop.
   - Add `bookie.stress.trades.per.second=0` to `src/main/resources/application.properties` so the property is explicit and discoverable.
   - Verify:
     - With property unset or `=0`, seed behavior is unchanged (1000 trades).
     - With `bookie.stress.trades.per.second=1000`, no seed trades are created at startup, and trades are booked continuously at ~1000/s (observe via trade count over time or logging).
     - With a lower rate like `10`, confirm pacing visually (trades appear roughly 100ms apart).

## Out of Scope

- Runtime control of the rate (e.g. admin endpoint to change `stressTradesPerSecond` while running). Startup-only.
- Multi-producer / multi-thread generation for very high rates (>100k/s). Single virtual thread with `parkNanos` is sufficient for the target 1000/s scale.
- Metrics, dashboards, or progress logging for the stress loop.
- Any cap or stop condition on total trades booked.
- Back-pressure if downstream (event bus, position service) can't keep up — that's precisely what the stress test is meant to expose.
- Changes to `application-dev.properties` or any test profile.

## Open Questions

None — all design questions answered in the brainstorming session.
