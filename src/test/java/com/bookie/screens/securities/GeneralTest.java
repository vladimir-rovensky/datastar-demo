package com.bookie.screens.securities;

import com.bookie.TestBase;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class GeneralTest extends TestBase {

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

    @Test
    public void handlesLoadingInvalidCusip() {
        givenExistingBonds(aBond("CSP1").setDescription("Desc 1"));
        var securities = page
                .switchToSecurities("CSP1")
                .switchToEditMode();

        page.verifyURL("**/security/CSP1**");

        securities.startLoadingSecurity("unknown");

        page.assertErrorShown("This CUSIP does not exist in the system");
        page.verifyURL("**/security/CSP1**");
        securities.getDescription()
                .verifyValue("Desc 1")
                .verifyEnablement(true);
    }

    @Test
    public void loadingCusipUpdatesURLCorrectly() {
        givenExistingBonds(
                aBond("CSP1").setDescription("Desc 1"),
                aBond("CSP2").setDescription("Desc 2"));

        getBondDAO().setLoadDelay(20); //This is necessary because the update channel is dead for a brief moment on back/forward

        var securities = page.switchToSecurities("CSP1");
        page.verifyURL("**/security/CSP1**");
        securities.getDescription().verifyValue("Desc 1");

        securities.loadCusip("CSP2");
        page.verifyURL("**/security/CSP2**");
        securities.getDescription().verifyValue("Desc 2");

        page.back();
        page.verifyURL("**/security/CSP1**");
        securities.getDescription().verifyValue("Desc 1");

        page.forward();
        page.verifyURL("**/security/CSP2**");
        securities.getDescription().verifyValue("Desc 2");

    }
}
