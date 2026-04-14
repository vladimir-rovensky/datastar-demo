package com.bookie.infra.events;

import com.bookie.domain.entity.Trade;

public record TradeDeletedEvent(Trade deletedTrade) {
}
