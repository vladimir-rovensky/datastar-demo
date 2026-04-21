package com.bookie.domain.entity;

import java.util.Collections;
import java.util.List;

public interface TradeDAO {

    List<Trade> findAll();

    int getTotalCount();

    Trade findById(Long id);

    void saveAll(List<Trade> trades);

    Trade delete(Long id);

    default void save(Trade trade) {
        this.saveAll(Collections.singletonList(trade));
    }
}
