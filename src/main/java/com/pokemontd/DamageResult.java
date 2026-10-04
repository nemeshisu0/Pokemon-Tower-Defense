package com.pokemontd;

public class DamageResult {
    public final int damage;
    public final double multiplier;
    public final boolean appliedParalysis;
    public final boolean appliedBurn;

    public DamageResult(int damage, double multiplier, boolean appliedParalysis, boolean appliedBurn) {
        this.damage = damage;
        this.multiplier = multiplier;
        this.appliedParalysis = appliedParalysis;
        this.appliedBurn = appliedBurn;
    }
}
