package com.bookie.domain.entity;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultTradeDAO implements TradeDAO {

    private final Map<Long, Trade> trades = new LinkedHashMap<>();

    @Override
    public List<Trade> findAll() {
        return new ArrayList<>(trades.values());
    }

    @Override
    public int getTotalCount() {
        return trades.size();
    }

    @Override
    public Trade findById(Long id) {
        return trades.get(id);
    }

    @Override
    public void saveAll(List<Trade> trades) {
        trades.forEach(trade -> this.trades.put(trade.getId(), trade));
    }

    @Override
    public Trade delete(Long id) {
        return trades.remove(id);
    }
}
