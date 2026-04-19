package com.bookie.screens.trades;

import com.bookie.TestBase;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TradesScreenTest extends TestBase {

    @Test
    public void tradeRowRendersWithCusipAndDescription() {
        Bond bond = aBond("912828ZT5");
        Trade trade = aTrade(1L, "912828ZT5", TradeDirection.BUY, 1_000_000);

        givenExistingBonds(bond);
        givenExistingTrades(trade);

        var page = switchToTrades();

        page.verifyTradeDisplayed(0, trade, bond);
    }

    @Test
    public void booksNewTrade() {
        Bond bond = aBond("912828ZT5");
        givenExistingBonds(bond);

        Trade trade = aTrade(null, "912828ZT5", TradeDirection.BUY, 1_000_000);
        trade.setTradeDate(LocalDate.now());
        trade.setSettleDate(LocalDate.now().plusDays(2));
        trade.setAccruedInterest(new BigDecimal(100_000));

        var page = switchToTrades();

        page.bookBuyTrade(trade);

        page.verifyTradeDisplayed(0, trade, bond);
    }
}
