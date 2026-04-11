package com.bookie.screens.securities;

public enum BondSection {

    GENERAL("general", "General"),
    INCOME("income", "Income"),
    REDEMPTION("redemption", "Redemption");

    private final String path;
    private final String label;

    BondSection(String path, String label) {
        this.path = path;
        this.label = label;
    }

    public String getPath() { return path; }

    public String getLabel() { return label; }

    public static BondSection fromPath(String path) {
        for (var section : values()) {
            if (section.path.equals(path)) {
                return section;
            }
        }
        return GENERAL;
    }
}
