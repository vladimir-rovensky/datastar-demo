package com.bookie.screens;

import com.bookie.TestBase;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.PositionBuilder.aPosition;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class CommonBehaviorTest extends TestBase {

    @Test
    public void canProcessLargeNumberOfUpdates() {
        givenExistingBonds(aBond("CSP1"));

        var positions = page.switchToPositions();

        IntStream.range(0, 10000).forEach(_ -> bookTrades(aTrade("CSP1", TradeDirection.BUY, 10_000).setBook("MUNI-WEST")));

        positions.verifyPositionsDisplayed(aPosition("CSP1", "MUNI-WEST", 100_000_000));
    }

    @Test
    public void sendsRealtimeUpdatesToOtherTabs() {
        var bond = aBond("CSP1");
        givenExistingBonds(bond);

        var trades = page.switchToTrades();

        var otherTab = openNewTab();
        var otherTrades = otherTab.switchToTrades();

        Trade trade = aTrade("CSP1", TradeDirection.BUY, 10_000).setTradeDate(LocalDate.now());
        trades.bookTrade(trade);

        otherTrades.verifyTradeDisplayed(0, trade, bond);
    }

}
