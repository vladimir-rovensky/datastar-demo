package com.bookie.domain.entity;

public enum TradeDirection {
    BUY("Buy"),
    SELL("Sell");

    private final String label;

    TradeDirection(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}