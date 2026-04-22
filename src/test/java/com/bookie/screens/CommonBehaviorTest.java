package com.bookie.screens;

import com.bookie.TestBase;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.PositionBuilder.aPosition;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class CommonBehaviorTest extends TestBase {

    @Test
    public void canProcessLargeNumberOfUpdates() {
        givenExistingBonds(aBond("CSP1"));

        var positions = switchToPositions();

        IntStream.range(0, 10000).forEach(_ -> bookTrades(aTrade("CSP1", TradeDirection.BUY, 10_000).setBook("MUNI-WEST")));

        positions.verifyPositionsDisplayed(aPosition("CSP1", "MUNI-WEST", 100_000_000));
    }
}
