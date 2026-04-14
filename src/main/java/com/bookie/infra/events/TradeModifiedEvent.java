package com.bookie.infra.events;

import com.bookie.domain.entity.Trade;

public record TradeModifiedEvent(Trade originalTrade, Trade updatedTrade) {
}
