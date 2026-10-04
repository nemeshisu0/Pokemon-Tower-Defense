package com.pokemontd;

import java.util.ArrayList;
import java.util.List;

public class Store {
    private ArrayList<Pokemon> inventory = new ArrayList<Pokemon>();
    private ArrayList<Pokemon> unlockedPokemon = new ArrayList<Pokemon>();
    private int money = 200;
    private int turretsPlaced = 0;

    public Store() {
        initStore();
    }

    private void initStore() {
        inventory.clear();
        inventory.add(new Pokemon("Pikachu", "/Resource/pikachu.png", PokemonType.ELECTRIC, 14, 250, 95, 65, "Attacco elettrico rapido. 35% paralisi (-50% vel). Devasta Acqua (2x)!"));
        inventory.add(new Pokemon("Squirtle", "/Resource/squirtle.png", PokemonType.WATER, 32, 500, 115, 90, "Getto d'acqua ad alto impatto. Devasta Fuoco e Roccia (2x)!"));
        inventory.add(new Pokemon("Charizard", "/Resource/charizard.png", PokemonType.FIRE, 75, 1000, 135, 120, "Lanciafiamme devastante ad ampio raggio. 45% scottatura. Devasta Erba (2x)!"));

        unlockedPokemon.clear();
        unlockedPokemon.add(new Pokemon("Rattata", "/Resource/rattata.png", PokemonType.NORMAL, 4, 0, 75, 85, "Attacco rapido base. Buono all'inizio, ma debole (-40%) contro Roccia."));
    }

    public void reset() {
        money = 200;
        turretsPlaced = 0;
        initStore();
    }

    public boolean buyPika(int indPika) {
        if (indPika >= 0 && indPika < inventory.size()) {
            Pokemon p = inventory.get(indPika);
            if (p.getPrice() <= money) {
                spend(p.getPrice());
                inventory.remove(indPika);
                unlockedPokemon.add(p);
                return true;
            }
        }
        return false;
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

    public int getTurretsPlaced() {
        return turretsPlaced;
    }

    public int getTurretPrice() {
        return 100 + (turretsPlaced * 70) + (turretsPlaced * turretsPlaced * 10);
    }

    public Turret buyTurret(Coord turretPos, int level) {
        int price = getTurretPrice();
        if (money >= price) {
            int x = turretPos.getX();
            int y = turretPos.getY();
            spend(price);
            turretsPlaced++;
            return new Turret("/Resource/red_bush.png", x, y, unlockedPokemon);
        }
        return null;
    }
}