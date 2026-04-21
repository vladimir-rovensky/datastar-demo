package com.bookie.infra.builders;

import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TradeBuilder {

    @NonNull
    public static Trade aTrade(Long id, String cusip, TradeDirection tradeDirection, int quantity) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setCusip(cusip);
        trade.setDirection(tradeDirection);
        trade.setQuantity(BigDecimal.valueOf(quantity));
        trade.setAccruedInterest(BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(0.1)));
        trade.setBook("CREDIT-NY");
        trade.setCounterparty("GOLDMAN");
        trade.setTradeDate(LocalDate.of(2026, 1, 15));
        trade.setSettleDate(LocalDate.of(2026, 1, 17));
        return trade;
    }
}
