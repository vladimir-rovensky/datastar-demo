# Feature: Securities Screen Loading Spinner

## Goal
Simulate a slow bond lookup (1s delay) and give the user immediate visual feedback while the data loads. The initial render returns instantly with a modal loading spinner; a VirtualThread performs the load in the background and triggers an SSE re-render when done.

## Key Decisions
- **Delay location**: `BondRepository.findBondByCusip` sleeps 1s via `Util.sleep(1000)` — keeps the screen unaware of timing details.
- **Async pattern**: `Util.startAsync` (VirtualThread) launched from `initialRender`; result delivered via `finishLoading` + `triggerUpdate()`.
- **Stale guard**: The lambda captures the current `activeCusip` from `routeInfo` before the sleep. `finishLoading` re-reads `routeInfo.activeCusip()` and discards the result if it no longer matches — no extra field needed.
- **Null bond**: If the CUSIP is not found, `finishLoading` receives `null`; stale check is skipped (bond is null, can't get cusip) and the null result is applied as-is (screen shows the "no CUSIP" message).
- **Spinner modality**: Rendered as an absolute-positioned overlay covering the entire `securities-screen` div, blocking pointer events. Secondary toolbar remains visible but non-interactive while loading.

## Implementation Steps

1. **Add sleep to `BondRepository.findBondByCusip`** — insert `Util.sleep(1000)` at the top of the method body in `src/main/java/com/bookie/domain/entity/BondRepository.java`.

2. **Add `isLoading` field and `finishLoading` method to `SecuritiesScreen`** — add `private boolean isLoading` field; add a `private synchronized void finishLoading(Bond bond)` method that: checks staleness (`bond != null && !bond.getCusip().equals(getRouteInfo().activeCusip())`→ return), sets `currentBond = bond`, `isLoading = false`, calls `triggerUpdate()`.

3. **Update `initialRender` to go async when CUSIP changes** — when the CUSIP has changed, instead of calling `findBondByCusip` inline: set `currentBond = null`, `editingBond = null`, `isLoading = true`; capture `String targetCusip` from the path variable; call `Util.startAsync(() -> { Bond bond = bondRepository.findBondByCusip(targetCusip); finishLoading(bond); })`. Then fall through to `handleInitialRender` as before (renders the spinner immediately).

4. **Render the modal spinner in `getContent()`** — when `isLoading`, append a spinner overlay div inside `securities-screen` alongside the existing content structure. The overlay sits on top of everything via absolute positioning and `pointer-events: all`.

5. **Add spinner CSS to `getStyles()`** — inside the existing `@scope` block, add styles for `.loading-overlay` (absolute, inset 0, semi-transparent background, flex-centered) and `.spinner` (CSS border-based rotating ring animation, no new dependencies).

## Out of Scope
- Cancelling an in-flight load when the user navigates away.
- Progress indication beyond a simple spinner.
- Error handling for failed loads (null bond is treated as "not found", not as an error).

## Open Questions
- None — design is fully resolved.
