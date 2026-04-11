package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.CouponType;
import com.bookie.domain.entity.DayCountConvention;
import com.bookie.infra.EscapedHtml;

import java.util.Arrays;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.infra.TemplatingEngine.html;

public class IncomeSection {

    public static EscapedHtml render(Bond bond, boolean editing) {
        var disabled = !editing;
        var couponTypeOptions = Arrays.stream(CouponType.values()).map(CouponType::name).toList();
        var dayCountOptions = Arrays.stream(DayCountConvention.values()).map(DayCountConvention::name).toList();
        var couponTypeName = bond.getCouponType() != null ? bond.getCouponType().name() : null;
        var dayCountName = bond.getDayCount() != null ? bond.getDayCount().name() : null;
        var resetSchedule = bond.getResetSchedule();

        var resetTable = (resetSchedule == null || resetSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No reset schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Reset Date", Bond.ResetEntry::resetDate),
                        column("New Rate", Bond.ResetEntry::newRate))
                        .withRows(resetSchedule)
                        .render();

        return html("""
                <div class="bond-income">
                    <div class="form-fields">
                        ${couponType}
                        ${coupon}
                        ${spread}
                        ${couponFrequency}
                        ${dayCount}
                        ${floatingIndex}
                    </div>
                    ${resetTable}
                </div>
                """,
                "couponType", formField("Coupon Type").withInput(selectInput("couponType", couponTypeOptions, couponTypeName).withDisabled(disabled)),
                "coupon", formField("Coupon").withInput(numberInput("coupon", bond.getCoupon()).withDisabled(disabled)),
                "spread", formField("Spread").withInput(numberInput("spread", bond.getSpread()).withDisabled(disabled)),
                "couponFrequency", formField("Coupon Frequency").withInput(numberInput("couponFrequency", bond.getCouponFrequency()).withDisabled(disabled)),
                "dayCount", formField("Day Count").withInput(selectInput("dayCount", dayCountOptions, dayCountName).withDisabled(disabled)),
                "floatingIndex", formField("Floating Index").withInput(textInput("floatingIndex", bond.getFloatingIndex()).withDisabled(disabled)),
                "resetTable", resetTable);
    }
}
