package com.bookie.screens.securities;

import com.bookie.TestBase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bookie.infra.builders.BondBuilder.aBond;

public class RedemptionTest extends TestBase {

    @Test
    public void calculatesOutstandingSize() {
        givenExistingBonds(aBond("CSP1").setIssueSize(new BigDecimal(1_000_000)));

        var securities = page.switchToSecurities();
        securities.loadCusip("CSP1").switchToRedemption();

        securities.getOutstandingAmount().verifyValue(1_000_000);

        securities.switchToEditMode();
        securities.getIssueSize().setValue(3_000_000);
        securities.addSinkRow(LocalDate.of(2025, 1, 1), 100_000);
        securities.getOutstandingAmount().verifyValue(2_900_000);
        securities.save();

        page.reload();

        securities = page.switchToSecurities();
        securities.getOutstandingAmount().verifyValue(2_900_000);
    }
}
