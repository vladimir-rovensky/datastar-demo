# Feature: FetchBuilder — typed DataStar fetch action builder

## Goal
Replace hand-written DataStar action strings like `@post('/url', {retry: 'always', openWhenHidden: true})` with a fluent Java builder that implements `Renderable`, so call sites produce type-safe, composable action expressions and the options API is consistent across the codebase.

## Key Decisions
- **Entry points on `HtmlExtensions.X`**: `X.get(url)`, `X.post(url)`, `X.put(url)`, `X.delete(url)` — method-per-verb avoids a stringly-typed HTTP method parameter.
- **`FetchBuilder implements Renderable`**: `render()` returns `EscapedHtml`; the existing `TemplatingEngine` already handles `Renderable` values in template substitutions, so no infrastructure changes are needed.
- **`Retry` as nested enum**: keeps it scoped to `FetchBuilder`; values `NEVER` (default, omitted from output) and `ALWAYS`.
- **`payload` as raw JS string**: `withPayload(String jsExpression)` accepts any JS expression; null by default (property omitted). Lets the `DataGrid` viewport calls use the builder too.

## Implementation Steps

1. **Create `FetchBuilder`** — new file `src/main/java/com/bookie/infra/FetchBuilder.java`.
   - `implements Renderable`
   - Private constructor; HTTP method and URL set at construction.
   - Fields: `openWhenHidden` (`Boolean`, null = omit), `retry` (`Retry`, null = omit), `excludeAllSignals` (boolean), `includeSignalsPattern` (String), `requestCancellationDisabled` (boolean), `payload` (String, null = omit).
   - Methods: `withOpenWhenHidden()`, `withOpenWhenHidden(boolean)`, `withRetry(Retry)`, `withExcludeAllSignals()`, `withIncludeSignals(String pattern)`, `withRequestCancellation(boolean)` (false → `requestCancellation: 'disabled'`, true = omit), `withPayload(String jsExpression)`.
   - Nested enum `Retry { NEVER, ALWAYS }`.
   - `render()` assembles the `@method('url', {...})` string; omits the options object entirely if no options are set.

2. **Wire entry points into `HtmlExtensions`** — modify `src/main/java/com/bookie/infra/HtmlExtensions.java`.
   - Add `get(String url)`, `post(String url)`, `put(String url)`, `delete(String url)` methods that return a new `FetchBuilder`.

3. **Replace call sites in `Shell.java`** — `Shell.java:82` (popstate `@get`) and `Shell.java:142` (init `@post` with `openWhenHidden` and `retry`).

4. **Replace call sites in `Link.java`** — `Link.java:43` (`@get` with `filterSignals: {exclude: /.*/}`).

5. **Replace call sites in `TradeTicketPopup.java`** — four simple `@post` calls at lines 92, 93, 106, 154, 155.

6. **Replace call sites in `DataGrid.java`** — sort header (`@post`, line 86), filter cell (`@post`, line 259), column-picker button (`@get`, line 342), column-picker popup actions (`@post` + `@delete`, lines 154–155), and the two viewport scroll/resize handlers (lines 106, 112) using `withPayload(...)`.

7. **Replace call sites in `TradesScreen.java`** — `onRowDoubleClick` (`@post`, line 101), `onDeleteRow` (`@get`, line 102), confirm/cancel buttons (lines 141–142).

8. **Replace call sites in `SecuritiesScreen.java`** — edit, save, and cancel buttons (lines 201, 207–208).

9. **Replace call sites in `GeneralSection.java`, `IncomeSection.java`, `RedemptionSection.java`** — the `data-on:change` form-field wrappers using `requestCancellation: 'disabled'` and dynamic `filterSignals: {include: new RegExp(evt.target.name)}`, plus schedule grid `@delete`/`@put` calls.

## Out of Scope
- Any new DataStar option types not already present in the codebase.

## Open Questions
- None — all design decisions resolved in brainstorming.