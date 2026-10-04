package com.pokemontd;

public enum PokemonType {
    NORMAL("Normale", "#95a5a6"),
    FIRE("Fuoco", "#e74c3c"),
    WATER("Acqua", "#3498db"),
    ELECTRIC("Elettro", "#f1c40f"),
    GRASS("Erba", "#2ecc71");

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
                if (target == FIRE) return 2.0;
                if (target == WATER || target == GRASS) return 0.5;
                break;
            case FIRE:
                if (target == GRASS) return 2.0;
                if (target == WATER || target == FIRE) return 0.5;
                break;
            case ELECTRIC:
                if (target == WATER) return 2.0;
                if (target == ELECTRIC || target == GRASS) return 0.5;
                break;
            case GRASS:
                if (target == WATER) return 2.0;
                if (target == FIRE || target == GRASS) return 0.5;
                break;
            case NORMAL:
            default:
                return 1.0;
        }
        return 1.0;
    }
}
