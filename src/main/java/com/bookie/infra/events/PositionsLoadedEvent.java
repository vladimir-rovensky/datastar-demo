package com.bookie.infra.events;

import com.bookie.domain.entity.Position;

import java.util.List;

public record PositionsLoadedEvent(List<Position> positions) {
}
