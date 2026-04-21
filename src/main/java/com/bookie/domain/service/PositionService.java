package com.bookie.domain.service;

import com.bookie.domain.entity.*;
import com.bookie.infra.EventBus;
import com.bookie.infra.events.PositionChangedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

@Component
public class PositionService {

    private final EventBus eventBus;
    private final Map<PositionKey, Position> positions = new HashMap<>();

    public PositionService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public synchronized List<Position> getPositions() {
        return List.copyOf(positions.values());
    }

    public synchronized void clear() {
        positions.clear();
    }

    public synchronized void onTradeBooked(Trade trade) {
        updatePosition(trade.getPositionKey(), position ->
                getUpdatedPosition(position, null, trade));
    }

    public synchronized void onTradeModified(Trade originalTrade, Trade updateTrade) {
        PositionKey originalKey = originalTrade.getPositionKey();
        PositionKey newKey = updateTrade.getPositionKey();

        if (Objects.equals(originalKey, newKey)) {
            updatePosition(newKey, position ->
                    getUpdatedPosition(position, originalTrade, updateTrade));
        } else {
            updatePosition(originalKey, position ->
                    getUpdatedPosition(position, originalTrade, null));
            updatePosition(newKey, position ->
                    getUpdatedPosition(position, null, updateTrade));
        }
    }

    public synchronized void onTradeDeleted(Trade trade) {
        updatePosition(trade.getPositionKey(), position ->
                getUpdatedPosition(position, trade, null));
    }

    private void updatePosition(PositionKey key, Function<Position, Position> change) {
        var currentPosition = getPosition(key);
        var newPosition = change.apply(currentPosition);

        this.positions.put(newPosition.getKey(), newPosition);

        eventBus.publish(new PositionChangedEvent(currentPosition, newPosition));
    }

    public Position getUpdatedPosition(Position position, Trade originalTrade, Trade currentTrade) {
        if(originalTrade != null) {
            position = removeTradeFromPosition(position, originalTrade);
        }

        if(currentTrade != null) {
            position = addTradeToPosition(position, currentTrade);
        }

        position.setLastActivity(new Date());

        return position;
    }

    private Position addTradeToPosition(Position position, Trade trade) {
        return applyDeltaToPosition(position, trade, 1);
    }

    private Position removeTradeFromPosition(Position position, Trade trade) {
        return applyDeltaToPosition(position, trade, -1);
    }

    private Position applyDeltaToPosition(Position position, Trade trade, int sign) {
        position = new Position(position);

        var today = LocalDate.now();
        var signedQuantity = signedQuantity(trade).multiply(BigDecimal.valueOf(sign));

        if (!trade.getTradeDate().isAfter(today)) {
            position.setCurrentPosition(position.getCurrentPosition().add(signedQuantity));
        }

        if (!trade.getSettleDate().isAfter(today)) {
            position.setSettledPosition(position.getSettledPosition().add(signedQuantity));
        }

        return position;
    }

    private BigDecimal signedQuantity(Trade trade) {
        return trade.getDirection() == TradeDirection.BUY ? trade.getQuantity() : trade.getQuantity().negate();
    }

    public synchronized Position getPosition(PositionKey key) {
        Position position = positions.get(key);
        return Optional.ofNullable(position).orElse(new Position(key));
    }
}
