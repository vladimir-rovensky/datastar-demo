# Feature: Batched Event Subscriptions

## Goal
Add a batched-subscription variant to `EventBus` so that screens can coalesce a surge of related events into a single re-render. Each event's per-type handler still runs on every event (so state mutations remain complete and typed), but a terminal `afterBatchProcessed` callback fires at most twice per window (once leading, once trailing), letting a screen call `triggerUpdate()` only once per batch. This preserves the existing "state change → reRender" mental model: the batch relocates the `triggerUpdate()` call from each individual handler into a single post-batch callback, rather than debouncing `reRender()` itself.

## Key Decisions

- **Where batching lives — in `EventBus`, not in `BaseScreen`.** Rejected the idea of coalescing `reRender()` directly because screens sometimes emit post-render payloads (e.g. `PositionsScreen.animateCurrentPositionIfChanged`) that must land after a specific render; a time-delayed render would reorder them and break the animation.
- **API shape — a builder with typed per-type handlers.** Rejected the simpler `subscribeBatched(List<Class<?>>, Consumer<List<Object>>)` shape because it forces subscribers to write `instanceof` / switch dispatch that `EventBus` already does today, and loses the `Class<T>`/`Consumer<T>` type safety.
  ```java
  var subscription = eventBus.subscribeBatched()
      .withWindowMs(150)                                   // optional, default 100
      .on(TradeBookedEvent.class,    this::onTradeBooked)
      .on(TradeModifiedEvent.class,  this::onTradeModified)
      .on(TradeDeletedEvent.class,   this::onTradeDeleted)
      .afterBatchProcessed(this::triggerUpdate)            // optional
      .subscribe();                                        // explicit terminal
  ```
- **Coalescing semantics — leading + trailing.** The first event on a closed window runs its handler and `afterBatchProcessed` immediately, and opens a 100 ms window. Events during the window are buffered. On window close, buffered handlers fire in publish order and `afterBatchProcessed` fires once more (only if the buffer was non-empty). At most two `afterBatchProcessed` calls per window; all events processed in order.
- **Default window size — 100 ms.** Exposed as a named constant on `EventBus` (`DEFAULT_BATCH_WINDOW_MS`) so it is trivially tweakable. Per-subscription override via `.withWindowMs(n)`.
- **Handlers become pure state mutators.** Callers strip `triggerUpdate()` from each `on*` handler and put it in `afterBatchProcessed`. This guarantees exactly one render per batch (except the leading/trailing pair, each of which still yields one render).
- **Mixing styles is allowed.** A screen can use plain `subscribe` and `subscribeBatched` together, and can register multiple independent batched subscriptions — each has its own window, buffer, and terminal callback.
- **Timing mechanism.** `EventBus` gains a shared `ScheduledExecutorService` (virtual-thread factory) for window-close tasks. The close task hands the drain off to the existing single-threaded `eventProcessor` so that batched handlers execute with the same ordering guarantees as non-batched ones.
- **Teardown.** The `Runnable` returned from `.subscribe()` cancels any in-flight window-close task, discards any buffered-but-unprocessed events, and unsubscribes the underlying per-type consumers from `EventBus`. Buffered events on a disposed subscription are dropped — the screen is gone, nothing to render.
- **Migration scope — `TradesScreen` and `PositionsScreen`.** Both get batched subscriptions for their surge-prone event types. `SecuritiesScreen` stays plain (single `BondSavedEvent`, no surge). `BondSavedEvent` in the trade/position screens also stays plain for the same reason. Non-screen subscribers (`PositionService`, `TradeRepository`) stay on plain `subscribe`.
- **`PositionsScreen` post-render animation in a batched world.** `onPositionChanged` today calls `animateCurrentPositionIfChanged(event)` after `triggerUpdate()` for each event. In a batch, we can't animate per event (intermediate `previousPosition`s would be stale after the final render, and repeated updates to the same cell would dogpile). Resolution: per-event handler stashes the *first-seen* `previousPosition` per position key into a screen-local map. `afterBatchProcessed` calls `triggerUpdate()` then iterates the map, synthesising a net-change animation (batch-start previous vs current state) per key and clearing the map. Common case (multiple distinct positions change in a surge) ⇒ one correct animation per key; rare same-key-twice case ⇒ one correct net animation.

## Implementation Steps

1. **Add the batched subscription feature to `EventBus`.** Single self-contained commit.
   - Add `public static final long DEFAULT_BATCH_WINDOW_MS = 100` on `EventBus`.
   - Add a `ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())` field alongside the existing `eventProcessor`.
   - Introduce two types in `com.bookie.infra` (nested in `EventBus` is fine if kept compact; otherwise split into sibling files):
     - `BatchedSubscriptionBuilder` — holds `windowMs` (init `DEFAULT_BATCH_WINDOW_MS`), `Map<Class<?>, Consumer<?>> handlers`, optional `Runnable afterBatchProcessed`. Methods: `withWindowMs(long)`, `<T> on(Class<T>, Consumer<T>)`, `afterBatchProcessed(Runnable)`, `Runnable subscribe()`.
     - `Batcher` — encapsulates leading + trailing logic. Fields: `List<BufferedEvent> pendingEvents`, `boolean windowOpen`, `ScheduledFuture<?> currentWindowTask`, the handler map, `afterBatchProcessed`, `windowMs`, a private lock. `BufferedEvent` is a tiny record of `(Class<?> type, Object event)`.
   - Add `public BatchedSubscriptionBuilder subscribeBatched()` on `EventBus`.
   - `.subscribe()` construction:
     - Build the `Batcher` with a snapshot of the handler map and config.
     - For each registered type `T`, call the existing `subscribe(T.class, evt -> batcher.accept(T.class, evt))` and collect the per-type unsubscribe `Runnable`s.
     - Return a combined `Runnable` that (a) invokes each per-type unsubscribe, (b) cancels `currentWindowTask` if present, (c) clears `pendingEvents`.
   - `Batcher.accept(Class<?> type, Object event)`:
     - Under the lock: if `!windowOpen`, set `windowOpen = true`, schedule `this::closeWindow` on `scheduler` after `windowMs`, and submit a task to `eventProcessor` that (i) invokes the matching handler for `event`, (ii) invokes `afterBatchProcessed` if set. If `windowOpen`, append `BufferedEvent(type, event)` to `pendingEvents`.
   - `Batcher.closeWindow()` (runs on `scheduler`): submit to `eventProcessor` a task that, under the lock, drains `pendingEvents` into a local list and resets `windowOpen = false` + `currentWindowTask = null`. Outside the lock (still on `eventProcessor`), if the drained list is non-empty, iterate in order invoking each type's handler on its event, then invoke `afterBatchProcessed` once (if set). If empty, no trailing fire.
   - Handler lookups must use the exact published class — same dispatch semantics as the existing `subscribe`.
   - **Verify** with a focused unit test (`src/test/java/com/bookie/infra/EventBusTest.java` if absent): publish 5 events across two registered types within 100 ms, assert each per-type handler ran once per event in publish order, and `afterBatchProcessed` ran exactly twice (leading + trailing). A second test with a single event asserts exactly one `afterBatchProcessed` call.

2. **Migrate `TradesScreen` to the batched variant.** Single observable commit.
   - Replace the three individual `eventBus.subscribe(TradeBookedEvent.class/TradeModifiedEvent.class/TradeDeletedEvent.class, …)` calls with one `subscribeBatched()....afterBatchProcessed(this::triggerUpdate).subscribe()` chain, stored in `eventSubscriptions` like the others.
   - Remove `triggerUpdate()` from the bodies of `onTradeBooked`, `onTradeModified`, `onTradeDeleted` — they become pure state mutators.
   - Leave the `BondSavedEvent` subscription as plain `subscribe`; that handler keeps its own `triggerUpdate()`.
   - **Verify** manually: rapid-fire several trade book/modify/delete operations and confirm the grid still reflects every change, while re-renders are coalesced (at most two per 100 ms burst). Single operations still feel instant (leading fire is synchronous relative to the event).
3. **Migrate `PositionsScreen` to the batched variant.** Single observable commit.
   - Same approach as TradeScreen. Don't worry about the current position animation - it will automatically cancel the previous animation.

## Out of Scope

- Debouncing `BaseScreen.reRender()` / `triggerUpdate()` itself — rejected to preserve post-render payload ordering (animations, etc.).
- Migrating `SecuritiesScreen`, `PositionService`, or `TradeRepository` to the batched variant.
- Backpressure, adaptive window sizing, or per-event priority — not needed at demo scale.
- Lifecycle shutdown for the new `scheduler` — the existing `eventProcessor` isn't shut down either; stay consistent with project style.

## Open Questions

- None
