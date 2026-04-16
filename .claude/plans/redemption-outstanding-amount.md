# Feature: Outstanding Amount in Redemption Section

## Goal
Add Outstanding Amount to the Redemption section, calculated as Issue Size minus the sum of all sinking fund entries with a sink date on or before today. Move Issue Size from General to the top of Redemption. Restructure the Redemption layout to show Issue Size and Outstanding Amount as a form-fields row above three equal-width schedule columns.

## Key Decisions
- **Outstanding Amount is null if any sink entry has a null date or null amount**: handles the transient state when a user adds a new entry but hasn't filled it in yet. Since validations prevent saving nulls, this only occurs during an active edit.
- **CSS grid for the three schedule columns**: `grid-template-columns: 1fr 1fr 1fr` gives equal widths that automatically fill the available space — simpler than flexbox with explicit flex values.
- **Issue Size handled via existing `handleInput` route**: the `data-on:change` in the new form-fields wrapper triggers the same `/securities/input/{field}` POST as General used to, no new route needed.
- **`max-width: 500px` on `.data-grid` removed**: the grid column constrains width naturally; the cell input `width: 100%` rule is kept.

## Implementation Steps

1. **Restructure Redemption layout** — `RedemptionSection.java` and `SecuritiesScreen.java` CSS
   - Wrap the three panels in `<div class="redemption-schedules fill-height">` inside the existing `.bond-redemption`
   - Rename the "Sinking Fund" `<h3>` to "Sink Schedule"
   - Remove the scoped `@scope { .data-grid { max-width: 500px } }` style; keep `.data-grid-cell input { width: 100%; }`
   - In `SecuritiesScreen.java` `getStyles()`: add `.redemption-schedules { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: var(--sp-lg); flex: 1; min-height: 0; }` and simplify `.redemption-panel` to `display: flex; flex-direction: column; gap: var(--sp-sm);`

2. **Move Issue Size and add Outstanding Amount** — `GeneralSection.java` and `RedemptionSection.java`
   - `GeneralSection.java`: remove `${issueSize}` from the HTML template and its `"issueSize", formField(...)` argument from the `html()` call
   - `RedemptionSection.java`: add `import java.math.BigDecimal` and `import java.time.LocalDate`; compute `outstandingAmount` by looping over `sinkingFundSchedule` — if any entry has a null `sinkDate` or null `amount`, set result to null; otherwise sum amounts where `sinkDate` is not after `LocalDate.now()` and subtract from `issueSize`
   - Add a `<div class="form-fields redemption-form-fields">` with `data-on:change="@post('/securities/input/' + evt.target.name, {requestCancellation: 'disabled', filterSignals: {include: new RegExp(evt.target.name)}})"` before the `.redemption-schedules` div, containing Issue Size (`numberInput("issueSize", ...).withFormat("currency").withDisabled(disabled)`) and Outstanding Amount (`numberInput("outstandingAmount", outstandingAmount).withFormat("currency").noBind().withDisabled(true)`)

## Out of Scope
- Persisting Outstanding Amount (it is always recalculated on render)
- Validating that Outstanding Amount is non-negative

## Open Questions
- None.
