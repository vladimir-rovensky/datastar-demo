package com.bookie.infra.events;

public class TradeDeletedEvent {

    private final Long tradeId;

    public TradeDeletedEvent(Long tradeId) {
        this.tradeId = tradeId;
    }

    public Long getTradeId() { return tradeId; }
}
