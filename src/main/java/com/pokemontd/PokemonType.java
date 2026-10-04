package com.pokemontd;

public enum PokemonType {
    NORMAL("Normale", "#95a5a6"),
    FIRE("Fuoco", "#e74c3c"),
    WATER("Acqua", "#3498db"),
    ELECTRIC("Elettro", "#f1c40f"),
    GRASS("Erba", "#2ecc71"),
    ROCK("Roccia", "#b3886b");

    private final String displayName;
    private final String colorHex;

    PokemonType(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public double getMultiplierAgainst(PokemonType target) {
        if (target == null) return 1.0;
        switch (this) {
            case WATER:
                if (target == FIRE || target == ROCK) return 2.0;
                if (target == WATER || target == GRASS) return 0.5;
                break;
            case FIRE:
                if (target == GRASS) return 2.0;
                if (target == WATER || target == FIRE || target == ROCK) return 0.5;
                break;
            case ELECTRIC:
                if (target == WATER) return 2.0;
                if (target == ELECTRIC || target == GRASS || target == ROCK) return 0.5;
                break;
            case GRASS:
                if (target == WATER || target == ROCK) return 2.0;
                if (target == FIRE || target == GRASS) return 0.5;
                break;
            case NORMAL:
                if (target == ROCK) return 0.6; // Rock armor resists basic normal attacks!
                return 1.0;
            case ROCK:
                if (target == FIRE) return 2.0;
                if (target == WATER || target == GRASS) return 0.5;
                break;
        }
        return 1.0;
    }
}
