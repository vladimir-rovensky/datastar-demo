package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.CouponType;
import com.bookie.domain.entity.DayCountConvention;
import com.bookie.infra.EscapedHtml;


import static com.bookie.components.DataGrid.column;
import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.TemplatingEngine.html;

public class IncomeSection {

    public static EscapedHtml render(Bond bond, boolean editing, BondRepository bondRepository) {
        var disabled = !editing;

        return html("""
                <div class="bond-income fill-height">
                    <div class="form-fields" data-on:change="${inputAction}">
                        ${couponType}
                        ${coupon}
                        ${spread}
                        ${couponFrequency}
                        ${dayCount}
                        ${floatingIndex}
                    </div>
                    ${resetTable}
                    <style>
                    @scope {
                        :scope {
                            padding: var(--sp-lg);
                            display: flex;
                            flex-direction: column;
                            gap: var(--sp-lg);
                        }

                        .form-fields { align-content: start; }

                        .reset-schedule-panel {
                            display: flex;
                            flex-direction: column;
                            gap: var(--sp-sm);
                            min-height: 0;
                            flex: 1;
                        }

                        #reset-schedule-grid {
                            display: flex;
                            flex-direction: column;
                            max-width: 500px;
                            flex: 1;
                            min-height: 0;
                        }
                    }
                    </style>
                </div>
                """,
                "inputAction", X.put(html("'/security/${cusip}/edit/' + evt.target.name", "cusip", bond.getCusip()))
                        .withRequestCancellation(false)
                        .withIncludeSignals(html("new RegExp(evt.target.name)")),
                "couponType", formField("Coupon Type").withInput(selectInput("couponType", CouponType.class, bond.getCouponType()).withDisabled(disabled))
                        .withError(bondRepository.validateCouponType(bond.getCouponType(), bond.getResetSchedule())),
                "coupon", formField("Coupon").withInput(numberInput("coupon", bond.getCoupon()).withDisabled(disabled))
                        .withError(bondRepository.validateCoupon(bond.getCoupon(), bond.getCouponType())),
                "spread", formField("Spread").withInput(numberInput("spread", bond.getSpread()).withDisabled(disabled)),
                "couponFrequency", formField("Coupon Frequency").withInput(numberInput("couponFrequency", bond.getCouponFrequency()).withDisabled(disabled))
                        .withError(bondRepository.validateCouponFrequency(bond.getCouponFrequency())),
                "dayCount", formField("Day Count").withInput(selectInput("dayCount", DayCountConvention.class, bond.getDayCount()).withDisabled(disabled))
                        .withError(bondRepository.validateDayCount(bond.getDayCount())),
                "floatingIndex", formField("Floating Index").withInput(textInput("floatingIndex", bond.getFloatingIndex()).withDisabled(disabled))
                        .withError(bondRepository.validateFloatingIndex(bond.getFloatingIndex(), bond.getCouponType())),
                "resetTable", getResetScheduleGrid(bond, disabled, bondRepository));
    }

    private static EscapedHtml getResetScheduleGrid(Bond bond, boolean disabled, BondRepository bondRepository) {
        var resetSchedule = bond.getResetSchedule();

        return html("""
                        <div class="reset-schedule-panel fill-height">
                            <h3>Reset Schedule</h3>
                            <div id="reset-schedule-grid" data-on:change="${resetScheduleAction}">
                            ${grid}
                            </div>
                        </div>
                """,
                "resetScheduleAction", X.put("/security/" + bond.getCusip() + "/edit/resetSchedule").withIncludeSignals("resetSchedule.*"),
                "grid",
                DataGrid
                    .withColumns(
                            column("Reset Date", Bond.ResetEntry::getResetDate)
                                    .withRenderer(r -> formField()
                                            .withInput(dateInput("resetSchedule." + r.getId() + ".resetDate", r.getResetDate()).withDisabled(disabled))
                                            .withError(bondRepository.validateResetScheduleDate(r.getResetDate(), resetSchedule))
                                            .render()),
                            column("New Rate", Bond.ResetEntry::getNewRate)
                                    .withRenderer(r -> formField()
                                            .withInput(numberInput("resetSchedule." + r.getId() + ".newRate", r.getNewRate()).withDisabled(disabled))
                                            .withError(bondRepository.validateResetScheduleRate(r.getNewRate()))
                                            .render()))
                    .withRows(resetSchedule)
                    .withRowID(Bond.ResetEntry::getId)
                    .withRowIDSignal(r -> "resetSchedule." + r.getId() + ".id")
                    .onDeleteRow(!disabled ? r -> X.delete("/security/" + bond.getCusip() + "/edit/resetSchedule/" + r.getId()).render() : null)
                    .onAddRow(!disabled ? X.post("/security/" + bond.getCusip() + "/edit/resetSchedule").render() : null)
                    .withNoRowsMessage("No Reset Schedule")
                    .render());
    }
}
