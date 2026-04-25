package com.bookie.domain.entity;

import com.bookie.infra.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FakeBondDAO implements BondDAO {

    private final Map<String, Bond> bonds = new LinkedHashMap<>();

    private int loadDelay = 0;

    public void reset() {
        bonds.clear();
        loadDelay = 0;
    }

    @Override
    public List<Bond> findAll() {
        return new ArrayList<>(bonds.values());
    }

    @Override
    public Map<String, Bond> findByCusips(Collection<String> cusips) {
        if(loadDelay > 0) {
            Util.sleep(loadDelay);
        }

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

    public FakeBondDAO setLoadDelay(int delay) {
        this.loadDelay = delay;
        return this;
    }
}
