package com.bookie.screens.securities;

import com.bookie.TestBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static org.hamcrest.MatcherAssert.assertThat;

public class IncomeTest extends TestBase {

    @Test
    public void editsResetSchedule() {
        givenExistingBonds(aBond("CSP"));

        var securities = page.switchToSecurities("CSP")
                .switchToEditMode()
                .switchToIncome();

        securities.addResetRow(LocalDate.of(2025, 1, 1), 1)
                .addResetRow(LocalDate.of(2026, 1, 1), 2)
                .addResetRow(LocalDate.of(2027, 1, 1), 3)
                .deleteResetRow(1)
                .save();

        var bond = getBondDAO().findByCusip("CSP");

        assertThat(bond.getResetSchedule().size(), Matchers.equalTo(2));
        assertThat(bond.getResetSchedule().get(0).getResetDate(), Matchers.equalTo(LocalDate.of(2025, 1, 1)));
        assertThat(bond.getResetSchedule().get(0).getNewRate(), Matchers.equalTo(BigDecimal.valueOf(1)));

        assertThat(bond.getResetSchedule().get(1).getResetDate(), Matchers.equalTo(LocalDate.of(2027, 1, 1)));
        assertThat(bond.getResetSchedule().get(1).getNewRate(), Matchers.equalTo(BigDecimal.valueOf(3)));
    }
}
