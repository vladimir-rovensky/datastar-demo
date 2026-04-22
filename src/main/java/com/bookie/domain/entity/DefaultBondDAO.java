package com.bookie.domain.entity;

import com.bookie.infra.Util;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultBondDAO implements BondDAO {

    private final Map<String, Bond> bonds;

    public DefaultBondDAO() {
        bonds = new LinkedHashMap<>();
    }

    @Override
    public List<Bond> findAll() {
        return new ArrayList<>(bonds.values());
    }

    @Override
    public Map<String, Bond> findByCusips(Collection<String> cusips) {
        Util.sleep(1500);

        var result = new HashMap<String, Bond>();
        var cusipSet = Set.copyOf(cusips);
        bonds.entrySet().stream()
                .filter(e -> cusipSet.contains(e.getKey()))
                .forEach(e -> result.put(e.getKey(), e.getValue().clone()));

        return result;
    }

    @Override
    public void saveAll(List<Bond> bonds) {
        bonds.forEach(bond -> {
            this.bonds.put(bond.getCusip(), bond);
        });
    }

}
