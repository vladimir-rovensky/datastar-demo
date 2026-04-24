package com.bookie.screens;

import com.bookie.TestBase;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import com.bookie.infra.Util;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    @Test
    public void handlesRequestOrdering() {
        givenExistingBonds(aBond("CSP1"));

        var securitites = page.switchToSecurities("CSP1");
        securitites.switchToEditMode();
        securitites.switchToRedemption();

        var signalMap = Map.of(
                1, new CompletableFuture<Void>(),
                2, new CompletableFuture<Void>(),
                3, new CompletableFuture<Void>());

        page.blockRoute("**/securities/input/issueSize", signalMap::get);

        securitites.getIssueSize().setValue(10);
        securitites.getIssueSize().setValue(100);
        securitites.getIssueSize().setValue(1000);

        signalMap.get(3).complete(null);
        Util.sleep(10);
        signalMap.get(2).complete(null);
        Util.sleep(10);
        signalMap.get(1).complete(null);
        Util.sleep(10);

        securitites.getIssueSize().verifyValue(1000);
    }
}
