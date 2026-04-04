package com.bookie.infra.events;

import com.bookie.domain.entity.Trade;

public class TradeBookedEvent {

    private final Trade trade;

    public TradeBookedEvent(Trade trade) {
        this.trade = trade;
    }

    public Trade getTrade() { return trade; }
}
