package com.bookie;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

public class TradesScreenTest extends BookieE2ETest {

    @Test
    public void tradeRowRendersWithCusipAndDescription() {
        Bond bond = aBond("912828ZT5");
        Trade trade = aTrade(1L, "912828ZT5", TradeDirection.BUY, 1_000_000);

        givenExistingBonds(bond);
        givenExistingTrades(trade);

        var page = switchToTrades();

        page.verifyTradeDisplayed(trade, bond);
    }
}
