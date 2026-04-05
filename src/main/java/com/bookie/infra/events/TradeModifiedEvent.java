package com.bookie.infra.events;

import com.bookie.domain.entity.Trade;

public class TradeModifiedEvent {

    private final Trade trade;

    public TradeModifiedEvent(Trade trade) {
        this.trade = trade;
    }

    public Trade getTrade() { return trade; }
}
