package com.pokemontd;

import java.util.ArrayList;
import java.util.List;

public class Store {
    private ArrayList<Pokemon> inventory = new ArrayList<Pokemon>();
    private ArrayList<Pokemon> unlockedPokemon = new ArrayList<Pokemon>();
    private int money = 200;
    private int turretPrice = 100;

    public Store() {
        initStore();
    }

    private void initStore() {
        inventory.clear();
        inventory.add(new Pokemon("Pikachu", "/Resource/pikachu.png", 15, 500));
        inventory.add(new Pokemon("Squirtle", "/Resource/squirtle.png", 30, 1000));
        inventory.add(new Pokemon("Charizard", "/Resource/charizard.png", 50, 2000));

        unlockedPokemon.clear();
        unlockedPokemon.add(new Pokemon("Rattata", "/Resource/rattata.png", 5, 0));
    }

    public void reset() {
        money = 200;
        turretPrice = 100;
        initStore();
    }

    public void buyPika(int indPika) {
        if (indPika >= 0 && indPika < inventory.size()) {
            Pokemon p = inventory.get(indPika);
            if (p.getPrice() <= money) {
                spend(p.getPrice());
                inventory.remove(indPika);
                unlockedPokemon.add(p);
            }
        }
    }

    public ArrayList<Pokemon> getInventory() {
        return inventory;
    }

    public List<Pokemon> getUnlockedPokemon() {
        return unlockedPokemon;
    }

    public void spend(int amount) {
        money -= amount;
    }

    public Integer getMoney() {
        return money;
    }

    public int getTurretPrice() {
        return turretPrice;
    }

    public Turret buyTurret(Coord turretPos, int level) {
        if (money >= turretPrice) {
            int x = turretPos.getX();
            int y = turretPos.getY();
            spend(turretPrice);
            turretPrice += 50;
            return new Turret("/Resource/red_bush.png", x, y, unlockedPokemon);
        }
        return null;
    }
}