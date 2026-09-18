package com.phantomstorage;

public enum ComparatorMode {
    NONE,
    PRESENCE,
    FULLNESS;

    public ComparatorMode cycle() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
