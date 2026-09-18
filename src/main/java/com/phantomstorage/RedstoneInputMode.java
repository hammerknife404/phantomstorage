package com.phantomstorage;

public enum RedstoneInputMode {
    NONE,
    ACTIVE_HIGH,
    ACTIVE_LOW,
    PULSE_TOGGLE;

    public RedstoneInputMode cycle() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
