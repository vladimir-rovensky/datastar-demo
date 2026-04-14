# Feature: Incremental Position Updates

## Goal
Replace the full-recompute approach in `PositionService` with an event-driven incremental model.
`PositionService` maintains a `Map<PositionKey, Position>` and applies deltas as trades are booked,
modified, or deleted. It emits position events that `PositionsScreen` subscribes to, removing the
screen's dependency on the trade list entirely.

## Key Decisions
- **PositionService stores only positions, not trades**: enriched trade events carry the data needed to reverse deltas, so no trade list is needed in the service.
- **TradeDeletedEvent carries the full Trade**: `TradeRepository` looks up the trade before removing it from its map, so the event can include the full object.
- **TradeModifiedEvent carries both original and updated trades**: `TradeRepository` captures the original before replacing it.
- **TradeRepository uses Map<Long, Trade>**: replaces the current `List<Trade>`, making pre-delete/pre-modify O(1) lookup natural.
- **lastActivity = new Date()**: set to the current timestamp whenever a position is updated — simple, always accurate, no staleness risk.
- **Delta logic**: `±quantity` is applied to `currentPosition` if `tradeDate <= today`, and to `settledPosition` if `settleDate <= today`. Sign is positive for BUY, negative for SELL.
- **New events are records**: consistent with `TradesLoadedEvent`.

## Implementation Steps

1. **Refactor `TradeRepository` to use `Map<Long, Trade>`** — replace `List<Trade> trades` with
   `Map<Long, Trade> trades`. Update `getAllTrades()`, `findById()`, `deleteTrade()`,
   `modifyTrade()`, `bookTrade()`, and `generate()` accordingly. `deleteTrade()` must capture
   the trade before removal; `modifyTrade()` must capture the original before replacement.

2. **Update `TradeDeletedEvent`** — convert to a record carrying `Trade deletedTrade` instead of
   `Long tradeId`. Update `TradeRepository.deleteTrade()` to pass the full trade.

3. **Update `TradeModifiedEvent`** — convert to a record carrying `Trade originalTrade` and
   `Trade updatedTrade`. Update `TradeRepository.modifyTrade()` to pass both.

4. **Create `PositionChangedEvent` and `PositionsLoadedEvent`** — two new records in
   `com.bookie.infra.events`. `PositionChangedEvent(Position position)` and
   `PositionsLoadedEvent(List<Position> positions)`.

5. **Refactor `PositionService`** — inject `EventBus`. Replace the `compute(List<Trade>)` public
   method with event subscriptions. Maintain `Map<PositionKey, Position> positions` as internal
   state (synchronized). Subscribe to all four trade events:
   - `TradesLoadedEvent`: full compute from scratch into the map, emit `PositionsLoadedEvent`
   - `TradeBookedEvent`: apply `+delta` for the trade's key, emit `PositionChangedEvent`
   - `TradeModifiedEvent`: reverse `originalTrade`'s delta, apply `updatedTrade`'s delta, emit `PositionChangedEvent`
   - `TradeDeletedEvent`: reverse `deletedTrade`'s delta, emit `PositionChangedEvent`

   Delta helper: `signedQuantity(trade)` returns `+quantity` for BUY, `-quantity` for SELL.
   Apply to `currentPosition` if `tradeDate <= today`, to `settledPosition` if `settleDate <= today`.
   Always set `lastActivity = new Date()` on the updated position.

6. **Refactor `PositionsScreen`** — remove `trades` field and all recompute logic. Subscribe to
   `PositionsLoadedEvent` (replace full `positions` list, trigger update) and `PositionChangedEvent`
   (upsert the position into the list by cusip+book key, re-sort by `lastActivity`, trigger update).
   Remove subscriptions to the four trade events.

## Out of Scope
- Persistence — trades and positions remain in-memory only.
- Removing zero-quantity positions from the map after all trades are deleted.

## Open Questions
- None.
