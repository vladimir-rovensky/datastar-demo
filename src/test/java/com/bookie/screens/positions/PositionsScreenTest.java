package com.bookie.screens.positions;

import com.bookie.TestBase;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

public class PositionsScreenTest extends TestBase {

    @Test
    public void showsPositions() {
        givenExistingBonds(aBond("CSP1"), aBond("CSP2"));
        givenExistingTrades(
                aTrade(1L, "CSP1", TradeDirection.BUY, 300_000).setBook("CREDIT-NY"),
                aTrade(2L, "CSP1", TradeDirection.SELL, 100_000).setBook("CREDIT-NY"),
                aTrade(3L, "CSP1", TradeDirection.BUY, 100_000).setBook("MUNI-WEST"),
                aTrade(3L, "CSP2", TradeDirection.BUY, 100_000).setBook("CREDIT-NY"));

        var page = switchToPositions();

        page.verifyPositionsDisplayed(
                aPosition("CSP1", "CREDIT-NY", 200_000),
                aPosition("CSP1", "MUNI-WEST", 100_000),
                aPosition("CSP2", "CREDIT-NY", 100_000));
    }

    @Test
    public void bookingATradeUpdatesPosition() {
        givenExistingBonds(aBond("CSP1"));
        givenExistingTrades(aTrade(1L, "CSP1", TradeDirection.BUY, 100_000).setBook("CREDIT-NY"));

        switchToTrades().bookBuyTrade(aTrade(null, "CSP1", TradeDirection.BUY, 200_000).setBook("CREDIT-NY"));

        switchToPositions().verifyPositionsDisplayed(aPosition("CSP1", "CREDIT-NY", 300_000));
    }
}
