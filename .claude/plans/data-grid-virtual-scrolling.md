# Feature: DataGrid virtual scrolling

## Goal
Add server-driven vertical virtual scrolling to `DataGrid`. When the grid's endpoint is set, the server renders only a window of rows (visible range plus an overscan buffer) rather than the full list. The client posts its scroll offset and viewport height to the grid's endpoint (debounced on scroll, and on container resize via a `ResizeObserver`); the server updates its state and re-renders. When the endpoint is not set, the grid continues to render every row and performs no scroll/resize wiring — fully backwards compatible. Row heights are fixed (shared constant between Java and CSS), so the visible window can be derived from `scrollTop / rowHeight`. Rows are absolutely positioned inside a sized inner wrapper using `transform: translateY(index * rowHeight)`, so no spacer divs are needed and the native scrollbar naturally reflects the full list. A short fade-in animation smooths the arrival of newly morphed-in rows.

## Key Decisions

- **Server-driven virtualization** rather than client-side. Reason: fits the DataStar demo philosophy — every DOM change flows through the server. Alternative considered: client-side virtualization with all rows in state — rejected; negates the point of the demo.

- **Fixed row height via shared constant**: `DataGrid.ROW_HEIGHT_PX = 36` in Java, matches `--row-height: 36px` in `global-styles.css`. The `.data-grid-row { height: var(--row-height); }` rule is removed and `height` is written inline from Java, so row height has a single Java source of truth. `--row-height` stays for the filter row and any other grid pieces that reference it. Reason: drift risk is acceptable since the Java constant is the authoritative one for row geometry.

- **Absolute positioning + `translateY`** rather than top/bottom spacer divs. Each row: `position: absolute; top: 0; width: 100%; transform: translateY(${rowIndex * ROW_HEIGHT_PX}px)`. The inner wrapper has `position: relative` and `height: ${totalRows * ROW_HEIGHT_PX}px`, giving the scrollbar the right size. Alternative considered: spacer divs above/below the window — rejected; absolute positioning is cleaner and transforms are compositor-friendly.

- **Overscan buffer** rendered above and below the visible viewport so short scrolls don't expose blank regions before the debounce fires. Size lives as a constant on `DataGrid` (start with 10 rows each side). Alternative considered: no buffer — rejected; fast scrolls would show gaps.

- **Virtualization only when `endpoint != null`**. Reason: preserves existing consumers that have no endpoint. Mirrors the current rule for sort/column-picker/filter.

- **Initial server-side window** before any scroll/resize event has fired: first `100` rows (constant `DEFAULT_INITIAL_WINDOW`). On mount, the `ResizeObserver` fires almost immediately and posts the real viewport height; the server then renders the correct window on next re-render.

- **Scroll payload via DataStar `payload`, not signals**. `data-on:scroll__debounce_150="@post('${endpoint}/viewport', {payload: {scrollTop: el.scrollTop, viewportHeight: el.clientHeight}})"`. Reason: scroll state is transient and shouldn't leak into other endpoints' signal maps.

- **Resize via inline `<script>` + `ResizeObserver`**. IIFE pattern using `document.currentScript.parentElement` to get the scroll container. Guards against double-initialisation with a `resizeObserverInitialized` flag on the element (morph can re-run the script if the container is replaced; idempotence keeps us safe). The observer dispatches a custom `data-grid-viewport-resize` event on the container, and a DataStar `data-on:data-grid-viewport-resize` handler posts the same `{scrollTop, viewportHeight}` payload to the viewport endpoint.

- **Stable IDs for morph preservation**: both the scroll container (`${gridId}-body`) and the inner wrapper (`${gridId}-body-content`) get stable IDs. Reason: idiomorph matches by ID, so re-renders from sort/filter/column-picker/viewport updates preserve the scroll container element and its `scrollTop`. Without stable IDs, every morph would reset scroll to 0.

- **Window coercion on shrink**: if filter/sort reduces total rows so the stored window lands past the end, the server slides it back to end at `totalRows`. If total rows is smaller than the window size, render all of them (`[0..totalRows)`). The browser auto-clamps `scrollTop` when content height shrinks — no explicit client-side reset needed.

- **Fade-in animation on `.data-grid-row`** (~120ms opacity 0→1). Reason: smooths the pop when new rows enter during scroll. Since idiomorph preserves existing rows by ID, the CSS `animation` only runs on freshly inserted rows — rows that stay visible through a morph don't re-animate.

- **Debounce at 150ms**. Balance between request rate and perceived lag.

## Implementation Steps

### Step 1: Share row height constant between Java and CSS
**Files**:
- `src/main/java/com/bookie/components/DataGrid.java`
- `src/main/resources/static/global-styles.css`

Changes:
- Add `public static final int ROW_HEIGHT_PX = 36;` at the top of `DataGrid`.
- In `renderRow`, add `style="height: ${ROW_HEIGHT_PX}px"` inline on the row div (using the text-block + `html()` placeholder pattern).
- Remove the `height: var(--row-height);` line from `.data-grid-row` in `global-styles.css`. Leave the `--row-height: 36px;` definition and the filter-row / header `min-height: var(--row-height)` usages unchanged.

**Verify**: run the app, open any grid. Row heights look identical to before. Filter row and header row still match. Inspecting a `.data-grid-row` shows `style="height: 36px"` inline and no `height` in the computed CSS rule.

### Step 2: Restructure DOM to absolutely-positioned rows inside a sized wrapper
**Files**: `src/main/java/com/bookie/components/DataGrid.java` and `src/main/resources/static/global-styles.css`.

Changes:
- In `render()`, give the existing `.data-grid-body` a stable id: `id="${gridId}-body"`.
- Wrap `bodyRows` inside a new inner div: `<div id="${gridId}-body-content" class="data-grid-body-content" style="height: ${totalRows * ROW_HEIGHT_PX}px">…</div>`, where `totalRows = sortRows(filterRows(this.rows)).size()`.
- In `renderRow`, accept the row's index within the filtered+sorted list. Update `renderRow`'s caller (the `EscapedHtml.concat` over `displayRows`) to pass the index — either switch to an indexed iteration or compute index once.
- In the rendered row div, add inline style: `position: absolute; top: 0; width: 100%; height: ${ROW_HEIGHT_PX}px; transform: translateY(${rowIndex * ROW_HEIGHT_PX}px)`. Merge with the existing height style from Step 1 (one `style` attribute).
- In `global-styles.css`, add `.data-grid-body-content { position: relative; }`. `.data-grid-body` keeps `flex: 1` and gains `overflow: auto` if not already effectively there (check existing rules).
- No virtualization yet — every row is still rendered, just absolutely positioned at its true index.

**Verify**: all rows render, grid scrolls normally, sort/filter/column-picker all still work visually. Inspect DOM — rows have `position: absolute` with increasing `translateY` values. Scroll container has the expected height from the inner wrapper. Hover, striped-rows, and action column all still look right.

### Step 3: Add server-side virtualization state and render the window
**Files**: `src/main/java/com/bookie/components/DataGrid.java`

Changes:
- Add constants: `private static final int OVERSCAN_ROWS = 10;` and `private static final int DEFAULT_INITIAL_WINDOW = 100;`.
- Add fields: `private int scrollTop = 0;` and `private int viewportHeight = 0;`.
- In `render()`, compute the window:
  - If `endpoint == null` → render every row (existing behaviour).
  - Else if `viewportHeight == 0` → render `[0, min(DEFAULT_INITIAL_WINDOW, totalRows))`.
  - Else compute `firstVisible = scrollTop / ROW_HEIGHT_PX`, `visibleCount = ceil(viewportHeight / ROW_HEIGHT_PX)`, then `startIndex = max(0, firstVisible - OVERSCAN_ROWS)` and `endIndex = min(totalRows, firstVisible + visibleCount + OVERSCAN_ROWS)`.
  - Coerce: if `totalRows <= (endIndex - startIndex)` just use `[0, totalRows)`; else if `startIndex >= totalRows`, slide the window so `endIndex = totalRows` and `startIndex = max(0, totalRows - (OVERSCAN_ROWS * 2 + visibleCount))`.
- Render only rows in `[startIndex, endIndex)`, but each row still uses its absolute index (i.e. the position in the full filtered+sorted list) when computing `translateY`. The inner wrapper's height stays `totalRows * ROW_HEIGHT_PX`.
- No client wiring yet — state only ever changes through internal re-renders.

**Verify**: on a grid with `endpoint` set (e.g. a screen already using sort/filter), inspect DOM — only ~100 rows are present in `.data-grid-body-content`, not all of them. Inner wrapper height equals `totalRows * 36px`. On a grid without an endpoint, every row is still rendered. Sort/filter/column-picker still work; filtering to a small list renders all (no empty window).

### Step 4: Wire debounced scroll to `/viewport` endpoint
**Files**: `src/main/java/com/bookie/components/DataGrid.java`

Changes:
- In `setupRoutes`, add `.POST("viewport", request -> getGrid.apply(request).applyViewport(request))`.
- Add handler:
  ```java
  @SuppressWarnings("unchecked")
  private ServerResponse applyViewport(ServerRequest request) throws Exception {
      var body = request.body(new ParameterizedTypeReference<Map<String, Object>>() {});
      this.scrollTop = ((Number) body.getOrDefault("scrollTop", 0)).intValue();
      this.viewportHeight = ((Number) body.getOrDefault("viewportHeight", 0)).intValue();
      this.reRender();
      return ServerResponse.ok().build();
  }
  ```
- On the scroll container div, when `endpoint != null`, add:
  `data-on:scroll__debounce_150="@post('${endpoint}/viewport', {payload: {scrollTop: el.scrollTop, viewportHeight: el.clientHeight}})"`.

**Verify**: scroll a long grid slowly — network tab shows `/viewport` POSTs roughly every 150ms; DOM morphs to reveal new rows that were not previously in the DOM. Scroll back up — previously-seen rows morph back in. Sort while scrolled — scroll position preserved, the server's window updates against the new ordering.

### Step 5: Wire `ResizeObserver` via inline script + DataStar custom event
**Files**: `src/main/java/com/bookie/components/DataGrid.java`

Changes:
- When `endpoint != null`, emit an inline `<script>` tag immediately after the scroll container's opening attributes (inside the body wrapper, but before the inner content div) with an IIFE:
  ```html
  <script>
  (function() {
      var container = document.currentScript.parentElement;
      if (container.resizeObserverInitialized) return;
      container.resizeObserverInitialized = true;
      new ResizeObserver(function() {
          container.dispatchEvent(new CustomEvent('data-grid-viewport-resize'));
      }).observe(container);
  })();
  </script>
  ```
- On the scroll container div, add:
  `data-on:data-grid-viewport-resize="@post('${endpoint}/viewport', {payload: {scrollTop: el.scrollTop, viewportHeight: el.clientHeight}})"`.
  (Consider also adding `__debounce_150` to collapse bursts during drag-resize.)
- Note: keep the `<script>` inside the same parent that morph preserves by id (`${gridId}-body`). Because the flag lives on the element, even if the script runs a second time after an odd morph, it won't attach twice.

**Verify**: resize the browser window — a `/viewport` POST fires; the grid's window widens or narrows accordingly (inspect DOM row count). The observer should not multi-fire on an unrelated re-render (check network tab).

### Step 6: Fade-in animation on rows
**Files**: `src/main/resources/static/global-styles.css`

Changes:
- Add a keyframe `@keyframes data-grid-row-fadein { from { opacity: 0; } to { opacity: 1; } }`.
- Add `animation: data-grid-row-fadein 120ms ease-out;` to `.data-grid-row`.

**Verify**: scrolling a long grid shows newly-appearing rows fading in smoothly. Rows that stay visible across a re-render (e.g. after sort or column picker) do NOT re-animate. Rows scrolling out do not flash.

## Out of Scope
- Horizontal virtualization (column virtualization).
- Variable row heights.
- Client-side virtualization.
- Persisting scroll position across sessions or navigations.
- Keyboard-driven scroll (page up/down) optimisations beyond what the native scroll container provides.
- Skeleton rows or loading placeholders in the blank zone produced by very fast scrolls.
- "Scroll to row" programmatic API.
- Tests (project has no test infrastructure for UI yet; existing pattern is manual verification per step).

## Open Questions
- DataStar `el` reference in `data-on:` expressions is assumed to point at the triggering element. If `el` isn't bound in this version of DataStar, the scroll handler will need to be rewritten (e.g. via `evt.target` or an intermediate `data-ref`). Confirm during Step 4 verification.
- The `@post` action's `payload` option is assumed to override the default signal-body. If the framework doesn't support this shape, fall back to stashing two underscore-prefixed signals (`_viewportTop`, `_viewportHeight`) and posting without a payload override.
- Whether an extra `__debounce` on the resize event is needed, or whether `ResizeObserver`'s own coalescing is sufficient. Start without it and add if resize bursts are noisy.
