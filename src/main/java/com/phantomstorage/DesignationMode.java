package com.phantomstorage;

public enum DesignationMode {
    OUTPUT,
    INPUT;

    public DesignationMode cycle() {
        return this == OUTPUT ? INPUT : OUTPUT;
    }
}
