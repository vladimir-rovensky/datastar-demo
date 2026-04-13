# Feature: Securities Screen Validation

## Goal
Add live validation to the SecuritiesScreen, following the same pattern as TradeTicketPopup/TradeRepository: validation methods on BondRepository, errors wired into form fields and grid cells on every re-render, Save button disabled when invalid, and a server-side guard throw on save.

## Key Decisions

- **Validation lives in BondRepository**: Follows the established TradeRepository pattern exactly. No separate validator class.
- **Sections receive BondRepository as a parameter**: `GeneralSection.render()`, `IncomeSection.render()`, and `RedemptionSection.render()` all gain a `BondRepository bondRepository` parameter so they can call `bondRepository.validate*()` inline, the same way TradeTicketPopup calls `tradeRepository.validate*()`.
- **FormField label is optional**: When no label is set (or blank), FormField renders only the `.input-wrapper` div without the outer `<label>` element. This allows grid cells to use formField for per-cell error display without spurious label markup.
- **Grid cells use formField(null)**: Each grid cell renderer wraps its input in `formField(null).withInput(...).withError(...).render()` so per-cell errors display identically to form field errors.
- **Grid-level error** (reset schedule "must have >= 1 entry"): Rendered as a `<p>` element inside the reset schedule grid container, above the DataGrid.
- **Spread**: No validation applied.
- **Coupon Frequency**: Must be one of {1, 2, 4, 12}.

## Validation Rules

### General Section
| Field | Rule |
|---|---|
| ISIN | Required |
| Ticker | Required |
| Issuer Name | Required |
| Bond Type | Required |
| Currency | Required |
| Country | Required |
| Issue Date | Required |
| Maturity Date | Required; must be after Issue Date |
| First Coupon Date | Optional; if present must be after Issue Date and before Maturity Date |
| Face Value | Required; > 0 |
| Issue Price | Required; > 0 |
| Issue Size | Required; >= 0 |

### Income Section
| Field | Rule |
|---|---|
| Coupon Type | Required |
| Coupon Frequency | Required; must be one of {1, 2, 4, 12} |
| Day Count | Required |
| Coupon | Required if CouponType == FIXED |
| Floating Index | Required if CouponType == FLOATING |
| Reset Schedule | Must have >= 1 entry if CouponType == FLOATING |
| ResetEntry.resetDate | Required; must be distinct across all entries |
| ResetEntry.newRate | Required; >= 0 |

### Redemption Section (apply only when entries exist)
| Field | Rule |
|---|---|
| CallEntry.callDate | Required; distinct across all call entries |
| CallEntry.callPrice | Required; >= 0 |
| PutEntry.putDate | Required; distinct across all put entries |
| PutEntry.putPrice | Required; >= 0 |
| SinkingFundEntry.sinkDate | Required; distinct across all sinking fund entries |
| SinkingFundEntry.amount | Required; >= 0 |

## Implementation Steps

1. **Modify `FormField` to support blank label** — Add a no-arg `formField()` factory method. In `render()`: when `label` is null or blank, render only `<div class="input-wrapper" ${error}>${input}</div>` (no outer `<label>` element).

2. **Add validation to `BondRepository`** — Add individual `validate*()` methods (each returns `null` for valid or an error string) covering all rules above. Add `isValid(Bond bond)` that returns `true` only when every `validate*()` returns `null`. Method signatures for cross-field validators pass only the values they need (e.g., `validateMaturityDate(LocalDate maturityDate, LocalDate issueDate)`). Per-entry date-distinctness validators accept the full entry list and the current entry's id to exclude the current entry from the uniqueness check.

3. **Update `GeneralSection`** — Add `BondRepository bondRepository` parameter to `render()`. Wire `.withError(bondRepository.validate*(...))` on every formField that has a validation rule. `validateIssueDate` and `validateFirstCouponDate` receive additional context values needed for cross-field checks.

4. **Update `IncomeSection`** — Add `BondRepository bondRepository` parameter to `render()`. Wire `.withError()` on all income formFields. In the reset schedule grid renderer, wrap each input in `formField(null).withInput(...).withError(bondRepository.validateReset*(...))).render()`. Add a grid-level error `<p>` above the DataGrid for `bondRepository.validateResetSchedule(bond.getResetSchedule(), bond.getCouponType())`.

5. **Update `RedemptionSection`** — Add `BondRepository bondRepository` parameter to `render()`. In each of the three grid renderers (call, put, sinking fund), wrap each input in `formField(null).withInput(...).withError(bondRepository.validate*(...))).render()`.

6. **Update `SecuritiesScreen`** — Pass `bondRepository` to all three `Section.render()` calls. In `renderEditActions()`, call `bondRepository.isValid(editingBond)` and add `disabled` attribute to the Save button when false. In `saveEdit()`, throw `RuntimeException("Tried to save an invalid bond.")` if `!bondRepository.isValid(editingBond)`.

## Out of Scope
- Spread validation (no rules applied).
- Description, Sector, Seniority Level, Dated Date, Last Coupon Date, ratings fields (all optional).
- Any UI feedback on the grid-level "dates distinct" check beyond blocking Save (per-cell date errors handle that).

## Open Questions
- None — all design decisions resolved.
