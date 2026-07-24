package com.dhruv.microloan_platform.entity;

public enum KycLevel {
    NONE,
    BASIC,
    FULL;

    public boolean atLeast(KycLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
