package com.bookie.infra.events;

import com.bookie.domain.entity.Position;

public record PositionChangedEvent(Position position) {
}
