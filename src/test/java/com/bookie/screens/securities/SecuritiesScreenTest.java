package com.bookie.screens.securities;

import com.bookie.TestBase;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class SecuritiesScreenTest extends TestBase {

    @Test
    public void editsSecurity() {
        givenExistingBonds(aBond("CSP1").setDescription("Old Description"));
        givenExistingTrades(aTrade("CSP1", TradeDirection.BUY, 10_000));

        var screen = switchToSecurities();
        screen.loadCusip("CSP1")
                .switchToEditMode()
                .getDescription().setValue("New Description");

        screen.save();

        switchToTrades()
                .verifyTradeDisplayed(0,
                        aTrade("CSP1", TradeDirection.BUY, 10_000),
                        aBond("CSP1").setDescription("New Description"));
    }

    @Test
    public void calculatesOutstandingSize() {
        givenExistingBonds(aBond("CSP1").setIssueSize(new BigDecimal(1_000_000)));

        var screen = switchToSecurities();
        screen.loadCusip("CSP1").switchToRedemption();

        screen.getOutstandingAmount().verifyValue(1_000_000);

        screen.switchToEditMode();
        screen.getIssueSize().setValue(3_000_000);
        screen.addSinkRow(LocalDate.of(2025, 1, 1), 100_000);
        screen.getOutstandingAmount().verifyValue(2_900_000);
        screen.save();

        reloadPage();

        screen = switchToSecurities();
        screen.getOutstandingAmount().verifyValue(2_900_000);
    }

    @Test
    public void notifiesWhenSecurityIsUpdatedBySomeoneElse() {
        trace(()-> {
        givenExistingBonds(aBond("CSP1").setDescription("Initial"));

        var screen = switchToSecurities("CSP1");
        screen.switchToEditMode();

            Bond bond = getSavedBond("CSP1").setDescription("Changed");
            saveBonds(bond);

        assertWarningShown("This bond was modified by someone else");
        });
    }

}
