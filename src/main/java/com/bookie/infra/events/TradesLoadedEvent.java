package com.bookie.infra.events;

import com.bookie.domain.entity.Trade;

import java.util.List;

public record TradesLoadedEvent(List<Trade> getTrades) {
}
