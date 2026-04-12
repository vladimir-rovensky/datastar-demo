package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.CouponType;
import com.bookie.domain.entity.DayCountConvention;
import com.bookie.infra.EscapedHtml;


import java.util.List;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.infra.TemplatingEngine.html;

public class IncomeSection {

    public static EscapedHtml render(Bond bond, boolean editing) {
        var disabled = !editing;

        return html("""
                <div class="bond-income fill-height">
                    <div class="form-fields" data-on:change="@post('/securities/input/' + evt.target.name, {requestCancellation: 'disabled', filterSignals: {include: new RegExp(evt.target.name)}})">
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
                        #reset-schedule-grid {
                            display: flex;
                            flex-direction: column;
                            max-width: 500px;
                         }
                    }
                    </style>
                </div>
                """,
                "couponType", formField("Coupon Type").withInput(selectInput("couponType", CouponType.class, bond.getCouponType()).withDisabled(disabled)),
                "coupon", formField("Coupon").withInput(numberInput("coupon", bond.getCoupon()).withDisabled(disabled)),
                "spread", formField("Spread").withInput(numberInput("spread", bond.getSpread()).withDisabled(disabled)),
                "couponFrequency", formField("Coupon Frequency").withInput(numberInput("couponFrequency", bond.getCouponFrequency()).withDisabled(disabled)),
                "dayCount", formField("Day Count").withInput(selectInput("dayCount", DayCountConvention.class, bond.getDayCount()).withDisabled(disabled)),
                "floatingIndex", formField("Floating Index").withInput(textInput("floatingIndex", bond.getFloatingIndex()).withDisabled(disabled)),
                "resetTable", getResetScheduleGrid(bond.getResetSchedule(), disabled));
    }

    private static EscapedHtml getResetScheduleGrid(List<Bond.ResetEntry> resetSchedule, boolean disabled) {
        return html("""
                        <div id="reset-schedule-grid" class="fill-height" data-on:change="@post('/securities/resetSchedule', {filterSignals: {include: /resetSchedule.*/}})">
                        ${grid}
                        </div>
                """, "grid",
                DataGrid
                    .withColumns(
                            column("Reset Date", Bond.ResetEntry::getResetDate)
                                    .withRenderer(r -> dateInput("resetSchedule." + r.getId() + ".resetDate", r.getResetDate())
                                            .withDisabled(disabled)),
                            column("New Rate", Bond.ResetEntry::getNewRate)
                                    .withRenderer(r -> numberInput("resetSchedule." + r.getId() + ".newRate", r.getNewRate())
                                            .withDisabled(disabled)))
                    .withRows(resetSchedule)
                    .withRowID(Bond.ResetEntry::getId)
                    .withRowIDSignal(r -> "resetSchedule." + r.getId() + ".id")
                    .onDeleteRow(!disabled ? r -> html("@delete('/securities/resetSchedule/${id}')", "id", r.getId()) : null)
                    .onAddRow(!disabled ? html("@put('/securities/resetSchedule')") : null)
                    .withNoRowsMessage("No Reset Schedule")
                    .render());
    }
}
