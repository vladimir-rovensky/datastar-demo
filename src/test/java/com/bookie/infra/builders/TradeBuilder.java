package com.bookie.infra.builders;

import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

public class TradeBuilder {

    public static Trade aTrade(String cusip, TradeDirection tradeDirection, int quantity) {
        return aTrade(null, cusip, tradeDirection, quantity);
    }

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
        trade.setExecutionTime(new Date());
        return trade;
    }
}
