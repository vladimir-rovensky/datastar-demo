package com.bookie.screens.trades;

import com.bookie.TestBase;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.TradeBuilder.aTrade;

public class TradesScreenTest extends TestBase {

    @Test
    public void tradeRowRendersWithCusipAndDescription() {
        Bond bond = aBond("912828ZT5");
        Trade trade = aTrade("912828ZT5", TradeDirection.BUY, 1_000_000);

        givenExistingBonds(bond);
        givenExistingTrades(trade);

        var trades = page.switchToTrades();

        trades.verifyTradeDisplayed(0, trade, bond);
    }

    @Test
    public void booksNewTrade() {
        Bond bond = aBond("912828ZT5");
        givenExistingBonds(bond);

        Trade trade = aTrade("912828ZT5", TradeDirection.BUY, 1_000_000);
        trade.setTradeDate(LocalDate.now());
        trade.setSettleDate(LocalDate.now().plusDays(2));

        var trades = page.switchToTrades();

        trades.bookTrade(trade);

        trades.verifyTradeDisplayed(0, trade, bond);
    }

    @Test
    public void modifiesExistingTrade() {
        Bond bond = aBond("912828ZT5");
        Trade trade = aTrade("912828ZT5", TradeDirection.BUY, 1_000_000);

        givenExistingBonds(bond);
        givenExistingTrades(trade);

        var trades = page.switchToTrades();

        var modifiedTrade = aTrade("912828ZT5", TradeDirection.BUY, 1_000_000);
        modifiedTrade.setQuantity(BigDecimal.valueOf(2_000_000));
        trades.modifyTrade(trade);

        trades.verifyTradeDisplayed(0, trade, bond);
    }

    @Test
    public void cancelsTrade() {
        givenExistingBonds(aBond("912828ZT5"));
        Trade trade = aTrade("912828ZT5", TradeDirection.BUY, 1_000_000);
        givenExistingTrades(trade);

        var trades = page.switchToTrades();

        trades.cancelTrade(trade.getId());

        trades.verifyNoTradesDisplayed();
    }

}
