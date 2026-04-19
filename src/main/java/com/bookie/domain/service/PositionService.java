package com.bookie.domain.service;

import com.bookie.domain.entity.*;
import com.bookie.infra.EventBus;
import com.bookie.infra.events.PositionChangedEvent;
import com.bookie.infra.events.PositionsLoadedEvent;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import com.bookie.infra.events.TradesLoadedEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PositionService {

    private final EventBus eventBus;
    private final Map<PositionKey, Position> positions = new HashMap<>();
    private final List<Runnable> eventSubscriptions = new ArrayList<>();

    public PositionService(TradeRepository tradeRepository, EventBus eventBus) {
        this.eventBus = eventBus;
        eventSubscriptions.add(eventBus.subscribe(TradesLoadedEvent.class, this::onTradesLoaded));
        eventSubscriptions.add(eventBus.subscribe(TradeBookedEvent.class, this::onTradeBooked));
        eventSubscriptions.add(eventBus.subscribe(TradeModifiedEvent.class, this::onTradeModified));
        eventSubscriptions.add(eventBus.subscribe(TradeDeletedEvent.class, this::onTradeDeleted));

        processTrades(tradeRepository.getAllTrades());
    }

    @PreDestroy
    public void dispose() {
        eventSubscriptions.reversed().forEach(Runnable::run);
    }

    public synchronized List<Position> getPositions() {
        return List.copyOf(positions.values());
    }

    public synchronized void clear() {
        positions.clear();
    }

    private synchronized Position getPosition(PositionKey key) {
        return positions.get(key);
    }

    private synchronized void onTradesLoaded(TradesLoadedEvent event) {
        processTrades(event.getTrades());
    }

    private void processTrades(List<Trade> trades) {
        positions.clear();
        for (Trade trade : trades) {
            addTradeToPosition(trade);
        }
        eventBus.publish(new PositionsLoadedEvent(List.copyOf(positions.values())));
    }

    private synchronized void onTradeBooked(TradeBookedEvent event) {
        addTradeToPosition(event.getTrade());
        eventBus.publish(new PositionChangedEvent(getPosition(event.getTrade().getPositionKey())));
    }

    private synchronized void onTradeModified(TradeModifiedEvent event) {
        removeTradeFromPosition(event.originalTrade());
        addTradeToPosition(event.updatedTrade());
        eventBus.publish(new PositionChangedEvent(getPosition(event.updatedTrade().getPositionKey())));
    }

    private synchronized void onTradeDeleted(TradeDeletedEvent event) {
        removeTradeFromPosition(event.deletedTrade());
        getPosition(event.deletedTrade().getPositionKey()).setLastActivity(new Date());
        eventBus.publish(new PositionChangedEvent(getPosition(event.deletedTrade().getPositionKey())));
    }

    private void addTradeToPosition(Trade trade) {
        applyDeltaToPosition(trade, 1);
    }

    private void removeTradeFromPosition(Trade trade) {
        applyDeltaToPosition(trade, -1);
    }

    private void applyDeltaToPosition(Trade trade, int sign) {
        var key = trade.getPositionKey();
        var position = positions.computeIfAbsent(key, Position::new);

        var today = LocalDate.now();
        var signedQuantity = signedQuantity(trade).multiply(BigDecimal.valueOf(sign));

        if (!trade.getTradeDate().isAfter(today)) {
            position.setCurrentPosition(position.getCurrentPosition().add(signedQuantity));
        }

        if (!trade.getSettleDate().isAfter(today)) {
            position.setSettledPosition(position.getSettledPosition().add(signedQuantity));
        }

        Date executionTime = trade.getExecutionTime();
        if (position.getLastActivity() == null || executionTime.after(position.getLastActivity())) {
            position.setLastActivity(executionTime);
        }
    }

    private BigDecimal signedQuantity(Trade trade) {
        return trade.getDirection() == TradeDirection.BUY ? trade.getQuantity() : trade.getQuantity().negate();
    }
}
