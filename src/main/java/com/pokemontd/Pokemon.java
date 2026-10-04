package com.pokemontd;

public class Pokemon {
    private String name;
    private String imageFile;
    private int atk;
    private int price;
    private int range;
    private int cooldown;
    private String description;

    public Pokemon(String name, String imageFile, int atk, int price, int range, int cooldown, String description) {
        this.name = name;
        this.imageFile = imageFile;
        this.atk = atk;
        this.price = price;
        this.range = range;
        this.cooldown = cooldown;
        this.description = description;
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
        return name + (price > 0 ? " - $" + price : " (Base)");
    }
}