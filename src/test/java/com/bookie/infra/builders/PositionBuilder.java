package com.bookie.infra.builders;

import com.bookie.domain.entity.Position;
import com.bookie.domain.entity.PositionKey;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

public class PositionBuilder {

    @NonNull
    public static Position aPosition(String cusip, String book, int currentPosition) {
        Position position = new Position(new PositionKey(cusip, book));
        position.setCurrentPosition(BigDecimal.valueOf(currentPosition));
        position.setSettledPosition(BigDecimal.valueOf(currentPosition));
        return position;
    }

}
