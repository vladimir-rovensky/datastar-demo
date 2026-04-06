package com.bookie.domain.service;

import com.bookie.domain.entity.Position;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class PositionService {

    public List<Position> compute(List<Trade> trades) {
        var today = LocalDate.now();
        return trades.stream()
                .collect(Collectors.groupingBy(t -> new PositionKey(t.getCusip(), t.getBook())))
                .entrySet().stream()
                .map(e -> {
                    var key = e.getKey();
                    var group = e.getValue();
                    var currentPosition = signedSum(group, t -> !t.getTradeDate().isAfter(today));
                    var settledPosition = signedSum(group, t -> !t.getSettleDate().isAfter(today));
                    var lastActivity = group.stream()
                            .map(Trade::getExecutionTime)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    var position = new Position();
                    position.setCusip(key.cusip());
                    position.setBook(key.book());
                    position.setCurrentPosition(currentPosition);
                    position.setSettledPosition(settledPosition);
                    position.setLastActivity(lastActivity);
                    return position;
                })
                .sorted(Comparator.comparing(Position::getLastActivity))
                .toList();
    }

    private BigDecimal signedSum(List<Trade> group, Predicate<Trade> filter) {
        return group.stream()
                .filter(filter)
                .map(t -> t.getDirection() == TradeDirection.BUY ? t.getQuantity() : t.getQuantity().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record PositionKey(String cusip, String book) {}
}
