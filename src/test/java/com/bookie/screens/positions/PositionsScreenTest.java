package com.bookie.screens.positions;

import com.bookie.TestBase;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.PositionBuilder.aPosition;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class PositionsScreenTest extends TestBase {

    @Test
    public void showsPositions() {
        givenExistingBonds(aBond("CSP1"), aBond("CSP2"));
        givenExistingTrades(
                aTrade("CSP1", TradeDirection.BUY, 300_000).setBook("CREDIT-NY"),
                aTrade("CSP1", TradeDirection.SELL, 100_000).setBook("CREDIT-NY"),
                aTrade("CSP1", TradeDirection.BUY, 100_000).setBook("MUNI-WEST"),
                aTrade("CSP2", TradeDirection.BUY, 100_000).setBook("CREDIT-NY"));

        var positions = page.switchToPositions();

        positions.verifyPositionsDisplayed(
                aPosition("CSP1", "CREDIT-NY", 200_000),
                aPosition("CSP1", "MUNI-WEST", 100_000),
                aPosition("CSP2", "CREDIT-NY", 100_000));
    }

    @Test
    public void bookingATradeUpdatesPosition() {
        givenExistingBonds(aBond("CSP1"));
        givenExistingTrades(aTrade("CSP1", TradeDirection.BUY, 100_000).setBook("CREDIT-NY"));

        page.switchToTrades().bookTrade(aTrade(null, "CSP1", TradeDirection.BUY, 200_000).setBook("CREDIT-NY"));

        page.switchToPositions().verifyPositionsDisplayed(aPosition("CSP1", "CREDIT-NY", 300_000));
    }

    @Test
    public void preventsBookingShortPosition() {
        givenExistingBonds(aBond("CSP1"));
        givenExistingTrades(aTrade("CSP1", TradeDirection.BUY, 100_000).setBook("CREDIT-NY"));

        var trades = page.switchToTrades()
                .bookTrade(
                        aTrade(null, "CSP1", TradeDirection.SELL, 50_000).setBook("CREDIT-NY"),
                        () -> bookTrades(aTrade("CSP1", TradeDirection.SELL, 60_000).setBook("CREDIT-NY")));

        trades.getTradeTicket().getQuantity().verifyError("Trade would result in negative Current Position");
        trades.getTradeTicket().cancel();

        var positions = page.switchToPositions();

        positions.verifyPositionsDisplayed(aPosition("CSP1", "CREDIT-NY", 40_000));
    }

    @Test
    public void handlesModifyTradeAffectingPositionKey() {
        givenExistingBonds(aBond("CSP1"));
        Trade trade = aTrade("CSP1", TradeDirection.BUY, 100_000).setBook("CREDIT-NY");
        givenExistingTrades(trade);

        var modifiedTrade = aTrade("CSP1", TradeDirection.BUY, 100_000).setBook("MUNI-WEST").setId(trade.getId());

        page.switchToTrades().modifyTrade(modifiedTrade);

        page.switchToPositions()
                .verifyPositionsDisplayed(
                        aPosition("CSP1", "MUNI-WEST", 100_000),
                        aPosition("CSP1", "CREDIT-NY", 0))
                .verifyPositionCount(2);
    }

    @Test
    public void preventsShortPositionByTradeCancel() {
        givenExistingBonds(aBond("CSP1"));

        var trade = aTrade("CSP1", TradeDirection.BUY, 100_000).setBook("CREDIT-NY");

        givenExistingTrades(
                trade,
                aTrade("CSP1", TradeDirection.SELL, 80_000).setBook("CREDIT-NY"));

        var trades = page.switchToTrades();

        trades.cancelTrade(trade.getId());

        page.assertErrorShown("Cancelling the trade would result in a short position");
        trades.cancelAction();

        var positions = page.switchToPositions();
        positions.verifyPositionsDisplayed(aPosition("CSP1", "CREDIT-NY", 20_000));
    }
}
