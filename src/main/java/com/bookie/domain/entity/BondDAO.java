package com.bookie.domain.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface BondDAO {

    List<Bond> findAll();

    Map<String, Bond> findByCusips(Collection<String> cusips);

    void saveAll(List<Bond> bonds);

    default Bond findByCusip(String cusip) {
        return findByCusips(List.of(cusip)).get(cusip);
    }

    default void save(Bond bond) {
        saveAll(Collections.singletonList(bond));
    }
}
