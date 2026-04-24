package com.bookie.screens.securities;

import com.bookie.TestBase;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class SecuritiesScreenTest extends TestBase {

    @Test
    public void editsSecurity() {
        givenExistingBonds(aBond("CSP1").setDescription("Old Description"));
        givenExistingTrades(aTrade("CSP1", TradeDirection.BUY, 10_000));

        var securities = page.switchToSecurities();
        securities.loadCusip("CSP1")
                .switchToEditMode()
                .getDescription().setValue("New Description");

        securities.save();

        page.switchToTrades()
                .verifyTradeDisplayed(0,
                        aTrade("CSP1", TradeDirection.BUY, 10_000),
                        aBond("CSP1").setDescription("New Description"));
    }

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

    @Test
    public void notifiesWhenSecurityIsUpdatedBySomeoneElse() {
        givenExistingBonds(aBond("CSP1").setDescription("Initial"));

        var securities = page.switchToSecurities("CSP1");
        securities.switchToEditMode();

        Bond bond = getSavedBond("CSP1").setDescription("Changed");
        saveBonds(bond);

        page.assertWarningShown("This bond was modified by someone else");
    }

    @Test
    public void loadingCusipPreservesSession() {
        givenExistingBonds(aBond("CSP1"), aBond("CSP2"));
        givenExistingTrades(
                aTrade("CSP1", TradeDirection.BUY, 10_000),
                aTrade("CSP2", TradeDirection.BUY, 10_000));

        page.switchToTrades()
                .getGrid()
                .setFilter("CUSIP", "CSP2");

        page.switchToSecurities("CSP1");

        page.switchToTrades()
                .getGrid()
                .verifyAllValuesInColumn("CUSIP", List.of("CSP2"));
    }

}
