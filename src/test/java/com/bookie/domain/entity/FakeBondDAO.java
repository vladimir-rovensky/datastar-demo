package com.bookie.domain.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FakeBondDAO implements BondDAO {

    private final Map<String, Bond> bonds = new LinkedHashMap<>();

    public void reset() {
        bonds.clear();
    }

    @Override
    public List<Bond> findAll() {
        return new ArrayList<>(bonds.values());
    }

    @Override
    public Map<String, Bond> findByCusips(Collection<String> cusips) {
        return cusips.stream()
            .filter(bonds::containsKey)
            .collect(Collectors.toMap(cusip -> cusip, cusip -> bonds.get(cusip).clone()));
    }

    @Override
    public void saveAll(List<Bond> bondsToSave) {
        for (Bond bond : bondsToSave) {
            bonds.put(bond.getCusip(), bond);
        }
    }
}
