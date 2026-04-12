# Feature: NumberInput Web Component

## Goal
Replace the native `<input type="number">` with a custom `<number-input>` web component that wraps `<input type="text">` and supports configurable display formatting (currency, decimal). The component exposes a numeric `value` property so it integrates seamlessly with DataStar's `data-bind` and `data-on:change`.

## Key Decisions
- **Light DOM (no Shadow DOM)**: the internal `<input type="text">` lives in the regular DOM so it inherits all `global-styles.css` rules automatically.
- **Format as a string attribute**: `format="currency"` or `format="decimal"`, with a separate `decimals` attribute (default 2). No enum, no helpers beyond `withFormat` and `withDecimals` in Java.
- **Focus/blur behaviour**: on focus show raw numeric value at the configured decimal places (e.g. `1234.56`); on blur re-format (e.g. `$1,234.56`).
- **Event mirroring**: internal `input` events → dispatch `input` on host (DataStar signal sync); blur → dispatch `change` on host (for `data-on:change` listeners).
- **Loading CSS via selector extension**: `data-class="{loading: $indicator}"` lands on `<number-input>`, not the internal `<input>`, so the three loading-related CSS rules are extended to also cover `number-input.loading`.
- **Caching**: same ETag / `max-age=0, must-revalidate` pattern as `global-styles.css`, with `cacheEnabled` dev bypass.

## Implementation Steps

1. **Add `/number-input.js` route in `Router.java`**
   - Add `.GET("/number-input.js", this::serveNumberInputJs)` to the route builder.
   - Add `serveNumberInputJs(ServerRequest)` handler mirroring `serveGlobalStyles` exactly (ETag = `appVersion`, `Cache-Control: max-age=0, must-revalidate`, `cacheEnabled` bypass).
   - Add `numberInputJsResource()` helper: `FileSystemResource("src/main/resources/static/number-input.js")` with `ClassPathResource("/static/number-input.js")` fallback.
   - Verify: `curl /number-input.js` returns 200, second request returns 304.

2. **Add script tag in `Shell.java`**
   - After the existing `<script type="module" src="/datastar1.0.0.RC8.js">` tag, add:
     `<script type="module" src="/number-input.js"></script>`
   - Verify: browser DevTools shows `number-input.js` loaded on any screen.

3. **Write `src/main/resources/static/number-input.js`**
   - Define `NumberInputElement extends HTMLElement`.
   - `observedAttributes`: `["name", "disabled", "format", "decimals"]`.
   - `connectedCallback`: create `<input type="text">`, append to self; attach `focus`, `blur`, and `input` listeners; call `_applyFormatted()` to show initial value.
   - `value` getter: return `this._numericValue` (a `Number` or `null`).
   - `value` setter: store as `Number`; if internal input is not focused, call `_applyFormatted()`.
   - `focus` listener on internal input: set `input.value` to the raw numeric value rendered to the configured decimal places (e.g. `(1234.5678).toFixed(2)` → `"1234.57"`); no prefix, no commas.
   - `blur` listener on internal input: parse `input.value` (strip `$` and commas, then `parseFloat`); store result; call `_applyFormatted()`; dispatch `new Event('change', { bubbles: true })` on host.
   - `input` listener on internal input: parse current text (strip non-numeric except `.` and `-`), store as `_numericValue`; dispatch `new Event('input', { bubbles: true })` on host.
   - `_applyFormatted()`: formats `_numericValue` and writes to `input.value`. For `format="currency"`: `$` prefix + `toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })`. For `format="decimal"`: `toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })` with no prefix. No format: `toString()`.
   - `attributeChangedCallback`: mirror `name` → `input.name`; mirror `disabled` → `input.disabled`; `format`/`decimals` changes → call `_applyFormatted()`.
   - `customElements.define('number-input', NumberInputElement)`.
   - Verify: a `<number-input format="currency" value="1234.56">` renders `$1,234.56`, shows `1234.56` on focus, re-formats on blur.

4. **Extend loading CSS in `global-styles.css`**
   - `input.loading { opacity: 0; }` → also add `number-input.loading input { opacity: 0; }`.
   - The two `.input-wrapper:has(input.loading)::before/::after` rules → extend each selector to also cover `.input-wrapper:has(number-input.loading)`.
   - Verify: `accruedInterest` field in Trade Ticket shows spinner and hides the inner input while loading.

5. **Update `NumberInput.java`**
   - Add `private String format` and `private int decimals = 2` fields.
   - Add `withFormat(String format)` and `withDecimals(int decimals)` builder methods returning `this`.
   - Change `render()` to emit `<number-input>` instead of `<input type="number">`, passing `name`, `value`, `format`, `decimals`, `${binding}`, and `${attrs}`.
   - Verify: Trade Ticket quantity and accrued interest fields render as `<number-input>`, DataStar binding works, `data-on:change` on the form fires correctly.

## Out of Scope
- Thousand-separator input masking while typing (only applied on blur/formatted display).
- Negative number support beyond what `parseFloat` handles naturally.
- Locale support beyond `en-US`.

## Open Questions
- None.