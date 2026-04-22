package com.bookie.domain.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FakeTradeDAO implements TradeDAO {

    private final Map<Long, Trade> trades = new LinkedHashMap<>();

    public void clear() {
        trades.clear();
    }

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
    public void saveAll(List<Trade> tradesToSave) {
        for (Trade trade : tradesToSave) {
            trades.put(trade.getId(), trade.clone());
        }
    }

    @Override
    public Trade delete(Long id) {
        return trades.remove(id);
    }
}
