package com.pokemontd;

public class Pokemon {
    private String name;
    private String imageFile;
    private PokemonType type;
    private int atk;
    private int price;
    private int range;
    private int cooldown;
    private String description;

    public Pokemon(String name, String imageFile, PokemonType type, int atk, int price, int range, int cooldown, String description) {
        this.name = name;
        this.imageFile = imageFile;
        this.type = type;
        this.atk = atk;
        this.price = price;
        this.range = range;
        this.cooldown = cooldown;
        this.description = description;
    }

    public PokemonType getType() {
        return type;
    }

    public int getPrice() {
        return price;
    }

    public String getLoc() {
        return imageFile;
    }

    public int getAttack() {
        return atk;
    }

    public String getName() {
        return name;
    }

    public int getRange() {
        return range;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " [" + type.getDisplayName() + "]" + (price > 0 ? " - $" + price : " (Base)");
    }
}