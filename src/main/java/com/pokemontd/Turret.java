package com.pokemontd;

import java.util.List;

public class Turret extends Stationary {
    private List<Pokemon> roster;
    private boolean hasShot = false;
    private int shotTimer = 0;

    public Turret(String location, int xstart, int ystart, List<Pokemon> roster) {
        super(location, xstart, ystart);
        this.roster = roster;
    }

    public Attacker shoot(Trainer t) {
        if (hasShot || roster == null || roster.isEmpty()) {
            return null;
        }
        shotTimer = 0;
        int rand = (int) (Math.random() * roster.size());
        Pokemon poke = roster.get(rand);
        Attacker atker = new Attacker(poke, this.getX(), this.getY(), t);
        hasShot = true;
        return atker;
    }

    public boolean hasShot() {
        return hasShot;
    }

    public void addToShotTimer() {
        shotTimer++;
    }

    public int shotTimer() {
        return shotTimer;
    }

    public void reload() {
        hasShot = false;
    }
}