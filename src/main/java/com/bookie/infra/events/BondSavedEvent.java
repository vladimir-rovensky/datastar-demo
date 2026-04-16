package com.bookie.infra.events;

import com.bookie.domain.entity.Bond;

public class BondSavedEvent {

    private final Bond bond;

    public BondSavedEvent(Bond bond) {
        this.bond = bond;
    }

    public Bond getBond() { return bond; }
}
