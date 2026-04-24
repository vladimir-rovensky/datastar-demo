package com.bookie.domain.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class FakeTradeDAO implements TradeDAO {

    private final Map<Long, Trade> trades = new LinkedHashMap<>();
    private CompletableFuture<Void> bookAvailable = CompletableFuture.completedFuture(null);

    public void reset() {
        trades.clear();
        bookAvailable.complete(null);
        bookAvailable = CompletableFuture.completedFuture(null);
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
            this.bookAvailable.join();
            trades.put(trade.getId(), trade.clone());
        }
    }

    @Override
    public Trade delete(Long id) {
        return trades.remove(id);
    }

    public void setBookAvailableSignal(CompletableFuture<Void> signal) {
        this.bookAvailable = signal;
    }
}
