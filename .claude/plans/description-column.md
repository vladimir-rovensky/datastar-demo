# Feature: Description Column on Trades and Positions Screens

## Goal
Add a Description column to TradesScreen and PositionsScreen showing the bond description for each row's CUSIP. Descriptions are loaded asynchronously in virtual threads to avoid blocking the UI, kept live via a new BondSavedEvent, and fall back to an empty string when the bond hasn't loaded yet.

## Key Decisions
- **Event source**: `BondSavedEvent` is published by `BondRepository.saveBond()`, not by `SecuritiesScreen` — `EventBus` is injected into `BondRepository`.
- **No bulk getAllBonds**: screens load only the CUSIPs they need via a new `findBondsByCusips(Collection<String> cusips)` method, with a 1500ms simulated delay.
- **Async pattern**: mirrors `SecuritiesScreen` — return immediately, then `triggerUpdate()` once bonds are loaded. A `loadBondsFor(Set<String>)` helper strips already-loaded CUSIPs before dispatching.
- **Incremental adds only**: on add events, load only CUSIPs not yet in `bondByCusip`. Deletes are ignored.
- **Column placement**: Trades — after CUSIP column; Positions — after Book column.
- **Null-safe**: `getDescriptionFor(cusip)` returns `""` when bond is absent.

## Implementation Steps

1. **Create `BondSavedEvent`** — `src/main/java/com/bookie/infra/events/BondSavedEvent.java`. Holds the saved `Bond`. Mirrors `TradeBookedEvent`.

2. **Inject `EventBus` into `BondRepository` and publish `BondSavedEvent`** — add `EventBus` constructor parameter; in `saveBond()`, after the `replaceAll`, call `eventBus.publish(new BondSavedEvent(bond))`.

3. **Add `findBondsByCusips` to `BondRepository`** — `public Map<String, Bond> findBondsByCusips(Collection<String> cusips)`: call `Util.sleep(1500)`, then filter `bonds` by the given set and return a `HashMap<String, Bond>` keyed by CUSIP.

4. **Add Description to `TradesScreen`**:
   - Inject `BondRepository` and `EventBus`; add `Map<String, Bond> bondByCusip = new HashMap<>()`
   - Add `private void loadBondsFor(Set<String> cusips)` — removes already-present keys, returns if empty, otherwise starts a virtual thread: calls `findBondsByCusips`, then `synchronized` merges result into `bondByCusip` and calls `triggerUpdate()`
   - In constructor (after `this.trades = ...`) and in `onTradesLoaded`: call `loadBondsFor` with all trade CUSIPs
   - In `onTradeBooked` and `onTradeModified`: call `loadBondsFor(Set.of(trade.getCusip()))`
   - Subscribe to `BondSavedEvent`: if `bondByCusip` contains that CUSIP, replace the entry and `triggerUpdate()`; unsubscribe in `dispose()`
   - Add `private String getDescriptionFor(String cusip)` returning `bondByCusip.getOrDefault(cusip, null)` description or `""`
   - Add `column("Description", t -> getDescriptionFor(t.getCusip()))` after the CUSIP column in `getContent()`

5. **Add Description to `PositionsScreen`** — same pattern as step 4, using `Position::getCusip`. Initial load and reload driven by `onPositionsLoaded`. Incremental load in `onPositionChanged` (load CUSIP only if not yet in map). Column placed after the Book column.

## Out of Scope
- Loading bonds for deleted trades/positions
- Prefetching bonds not yet referenced by any trade or position

## Open Questions
- None.
