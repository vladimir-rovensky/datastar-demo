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
        var resetSchedule = bond.getResetSchedule();

        var resetTable = (resetSchedule == null || resetSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No reset schedule.</p>
                        """)
                : getResetScheduleGrid(resetSchedule, disabled);

        return html("""
                <div class="bond-income">
                    <div class="form-fields" data-on:change="@post('/securities/input/' + evt.target.name, {requestCancellation: 'disabled', filterSignals: {include: new RegExp(evt.target.name)}})">
                        ${couponType}
                        ${coupon}
                        ${spread}
                        ${couponFrequency}
                        ${dayCount}
                        ${floatingIndex}
                    </div>
                    <div>
                    ${resetTable}
                    </div>
                </div>
                """,
                "couponType", formField("Coupon Type").withInput(selectInput("couponType", CouponType.class, bond.getCouponType()).withDisabled(disabled)),
                "coupon", formField("Coupon").withInput(numberInput("coupon", bond.getCoupon()).withDisabled(disabled)),
                "spread", formField("Spread").withInput(numberInput("spread", bond.getSpread()).withDisabled(disabled)),
                "couponFrequency", formField("Coupon Frequency").withInput(numberInput("couponFrequency", bond.getCouponFrequency()).withDisabled(disabled)),
                "dayCount", formField("Day Count").withInput(selectInput("dayCount", DayCountConvention.class, bond.getDayCount()).withDisabled(disabled)),
                "floatingIndex", formField("Floating Index").withInput(textInput("floatingIndex", bond.getFloatingIndex()).withDisabled(disabled)),
                "resetTable", resetTable);
    }

    private static EscapedHtml getResetScheduleGrid(List<Bond.ResetEntry> resetSchedule, boolean disabled) {
        return DataGrid
                .withColumns(
                        column("Reset Date", Bond.ResetEntry::getResetDate)
                                .withRenderer(r -> dateInput("resetDate", r.getResetDate())
                                        .withDisabled(disabled)
                                        .noBind()),
                        column("New Rate", Bond.ResetEntry::getNewRate)
                                .withRenderer(r -> numberInput("newRate", r.getNewRate())
                                        .withDisabled(disabled)
                                        .noBind()))
                .withRows(resetSchedule)
                .withRowID(Bond.ResetEntry::getId)
                .withRowAttrs(r -> html("""
                        data-on:change="@post('/securities/resetSchedule/${rowID}/' + evt.target.name, {payload: {[evt.target.name]: evt.target.value}})"
                """, "rowID", r.getId()))
                .render();
    }
}
