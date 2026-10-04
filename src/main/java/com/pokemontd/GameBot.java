package com.pokemontd;

import java.util.*;

/**
 * GameBot: Autonomous AI agent for gameplay automation, power system testing,
 * and game balance telemetry.
 */
public class GameBot {
    private final TowerDefense game;
    private final Store store;
    private final Entities entities;
    private final GrassMap grassMap;

    private boolean enabled = false;
    private int tickCounter = 0;
    private int waveWaitTicks = 0;
    private int previousHealth = 25;
    private int cleanWavesCount = 0;
    private int totalDamageSustained = 0;
    private int highestWaveReached = 1;
    private String currentStatus = "Bot Disattivo";
    private final List<String> recentLogs = new ArrayList<>();

    // Strategic positions along the enemy path ordered by tactical value
    private static final Coord[] STRATEGIC_TARGETS = {
        new Coord(128, 609), // Grass 1: lower-left bend (Covers turn 1)
        new Coord(112, 609), // Grass 1: lower corner
        new Coord(208, 481), // Grass 2: mid lane vertical corridor
        new Coord(272, 337), // Grass 3: upper bend right
        new Coord(176, 209), // Grass 4: gym approach corridor
        new Coord(240, 481), // Grass 2: mid lane right
        new Coord(288, 337), // Grass 3: upper bend secondary
        new Coord(144, 609), // Grass 1: entrance defense
        new Coord(208, 209), // Grass 4: gym gate defense
        new Coord(96, 625),  // Grass 1: bottom-left wing
        new Coord(224, 497), // Grass 2: lower-mid
        new Coord(160, 209)  // Grass 4: top-left
    };

    private final GameTelemetry telemetry;

    public GameBot(TowerDefense game, Store store, Entities entities, GameTelemetry telemetry) {
        this.game = game;
        this.store = store;
        this.entities = entities;
        this.telemetry = telemetry;
        this.grassMap = new GrassMap();
        this.previousHealth = entities.getTowerHealth();
        addLog("Bot AI inizializzato. Pronto all'uso.");
    }

    public GameTelemetry getTelemetry() {
        return telemetry;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (telemetry != null) {
            telemetry.setBotControlled(enabled);
        }
        if (enabled) {
            currentStatus = "AI Attiva - In analisi tattica...";
            addLog("🤖 Bot AI ATTIVATO.");
        } else {
            currentStatus = "Bot Disattivo";
            addLog("⏸ Bot AI DISATTIVATO.");
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void reset() {
        tickCounter = 0;
        waveWaitTicks = 0;
        cleanWavesCount = 0;
        totalDamageSustained = 0;
        highestWaveReached = 1;
        previousHealth = 25;
        if (telemetry != null) {
            telemetry.reset();
        }
        recentLogs.clear();
        addLog("Sistema riavviato. Statistiche resettate.");
    }

    public String getRosterSummary() {
        Map<String, Integer> counts = new HashMap<>();
        for (Turret t : entities.getTurrets()) {
            Pokemon p = t.getAssignedPokemon();
            if (p != null) {
                counts.put(p.getName(), counts.getOrDefault(p.getName(), 0) + 1);
            }
        }
        if (counts.isEmpty()) return "Nessuna";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append(" x").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Main AI tick invoked by the game loop.
     */
    public void tick(boolean isGameOn, int currentLevel) {
        if (!enabled) {
            return;
        }

        tickCounter++;
        highestWaveReached = Math.max(highestWaveReached, currentLevel);

        // Check health loss in real time
        int currentHp = entities.getTowerHealth();
        if (currentHp < previousHealth) {
            int lost = previousHealth - currentHp;
            totalDamageSustained += lost;
            previousHealth = currentHp;
            addLog("[W" + currentLevel + "] ⚠️ Danno subito alla Palestra: -" + lost + " HP!");
        }

        // Run decision logic every 6 ticks (smooth reaction without hogging CPU)
        if (tickCounter % 6 == 0) {
            if (!isGameOn) {
                handleInterWave(currentLevel);
            } else {
                handleInWaveCombat(currentLevel);
            }
        }
    }

    /**
     * Between-waves decision phase: upgrade Pokédex, place turrets, then auto-start wave.
     */
    private void handleInterWave(int currentLevel) {
        currentStatus = "Inter-ondata: Gestione economia & potenziamenti...";

        // 1. Try to unlock Pokémon from Pokédex
        checkPokedexPurchases(currentLevel);

        // 2. Try to place turrets strategically
        checkTurretPlacement(currentLevel);

        // 3. Optimize Pokémon on turrets
        optimizeTurretAssignments(null);

        // 4. Auto start wave after short delay (approx. 0.4s to 1s)
        waveWaitTicks++;
        int requiredWait = (game.getGameSpeed() < 8) ? 12 : 25;
        if (waveWaitTicks >= requiredWait) {
            waveWaitTicks = 0;
            currentStatus = "Avvio Ondata " + currentLevel + "...";
            addLog("[W" + currentLevel + "] ▶ Avvio automatico Ondata " + currentLevel);
            game.startWaveFromBot();
        }
    }

    /**
     * Combat phase: adapt turret Pokémon dynamically to counter incoming enemy types.
     */
    private void handleInWaveCombat(int currentLevel) {
        currentStatus = "Combattimento W" + currentLevel + " - Countering attivo";

        ArrayList<Trainer> trainers = entities.getTrainers();
        if (trainers.isEmpty()) {
            return;
        }

        // Dynamically assign best counter Pokémon to each turret
        for (Turret turret : entities.getTurrets()) {
            Trainer targetTrainer = findPriorityTarget(turret, trainers);
            if (targetTrainer != null) {
                int bestPokeIndex = findBestCounterPokemon(targetTrainer.getType());
                if (bestPokeIndex != -1 && turret.getAssignedPokemon() != store.getUnlockedPokemon().get(bestPokeIndex)) {
                    turret.setAssignedPokemon(bestPokeIndex);
                }
            } else {
                // If no enemy in immediate range, default to highest base DPS
                int highestDpsIndex = store.getUnlockedPokemon().size() - 1;
                turret.setAssignedPokemon(highestDpsIndex);
            }
        }

        // Also check if we have enough spare funds to place an extra turret mid-wave!
        if (store.getMoney() >= store.getTurretPrice()) {
            int turretsCount = entities.getTurrets().size();
            // In late waves or when wealthy, place reinforcements immediately
            if (turretsCount >= 3 || currentLevel >= 5) {
                placeNextBestTurret(currentLevel);
            }
        }
    }

    /**
     * Checks if we should purchase Pokémon from the Pokédex based on priority.
     */
    private void checkPokedexPurchases(int currentLevel) {
        ArrayList<Pokemon> inv = store.getInventory();
        int money = store.getMoney();

        // Check Pikachu ($250)
        for (int i = 0; i < inv.size(); i++) {
            Pokemon p = inv.get(i);
            if (p.getName().equalsIgnoreCase("Pikachu") && money >= p.getPrice()) {
                if (store.buyPika(i)) {
                    addLog("[W" + currentLevel + "] ⚡ Sbloccato Pikachu ($250)! Counter Acqua!");
                    game.updateLabels();
                    return;
                }
            }
        }

        // Check Squirtle ($500)
        for (int i = 0; i < inv.size(); i++) {
            Pokemon p = inv.get(i);
            if (p.getName().equalsIgnoreCase("Squirtle") && money >= p.getPrice()) {
                if (store.buyPika(i)) {
                    addLog("[W" + currentLevel + "] 💧 Sbloccato Squirtle ($500)! Counter Roccia/Fuoco!");
                    game.updateLabels();
                    return;
                }
            }
        }

        // Check Charizard ($1000)
        for (int i = 0; i < inv.size(); i++) {
            Pokemon p = inv.get(i);
            if (p.getName().equalsIgnoreCase("Charizard") && money >= p.getPrice()) {
                if (store.buyPika(i)) {
                    addLog("[W" + currentLevel + "] 🔥 Sbloccato Charizard ($1000)! Dominio Erba & DPS!");
                    game.updateLabels();
                    return;
                }
            }
        }
    }

    /**
     * Decides whether to buy a new turret or save for Pokédex unlocks.
     */
    private void checkTurretPlacement(int currentLevel) {
        int turretsCount = entities.getTurrets().size();
        int price = store.getTurretPrice();
        int money = store.getMoney();

        if (money < price) {
            return;
        }

        // Early game priority:
        // Turret 1 & 2 are essential for survival!
        if (turretsCount < 2) {
            placeNextBestTurret(currentLevel);
            return;
        }

        // If we have 2 turrets, save for Pikachu ($250) unless we have ample excess gold
        boolean hasPikachu = hasUnlocked("Pikachu");
        if (!hasPikachu) {
            if (money >= price + 250) {
                placeNextBestTurret(currentLevel);
            }
            return;
        }

        // If we have 3 turrets, save for Squirtle ($500)
        boolean hasSquirtle = hasUnlocked("Squirtle");
        if (turretsCount >= 3 && !hasSquirtle) {
            if (money >= price + 500) {
                placeNextBestTurret(currentLevel);
            }
            return;
        }

        // Beyond 4 turrets, save for Charizard ($1000)
        boolean hasCharizard = hasUnlocked("Charizard");
        if (turretsCount >= 4 && !hasCharizard) {
            if (money >= price + 1000) {
                placeNextBestTurret(currentLevel);
            }
            return;
        }

        // Otherwise place turret
        placeNextBestTurret(currentLevel);
    }

    private boolean placeNextBestTurret(int currentLevel) {
        // First check strategic priority targets
        for (Coord target : STRATEGIC_TARGETS) {
            Coord validTile = grassMap.isPlaceable(target);
            if (validTile.getX() > 0) {
                // Ensure no existing turret is placed at this tile (within 16px)
                if (!entities.isLocationOccupied(validTile.getX(), validTile.getY(), 16.0)) {
                    if (game.placeTurret(validTile)) {
                        int num = entities.getTurrets().size();
                        addLog("[W" + currentLevel + "] 🌿 Piazzata Torretta #" + num + " a (" + validTile.getX() + "," + validTile.getY() + ")");
                        return true;
                    }
                }
            }
        }

        // When strategic targets are full, expand across the rest of the grass map!
        for (Coord tile : grassMap.getAllPlaceableTiles()) {
            if (!entities.isLocationOccupied(tile.getX(), tile.getY(), 16.0)) {
                if (game.placeTurret(tile)) {
                    int num = entities.getTurrets().size();
                    addLog("[W" + currentLevel + "] 🌿 Espansione Torretta #" + num + " a (" + tile.getX() + "," + tile.getY() + ")");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasUnlocked(String name) {
        for (Pokemon p : store.getUnlockedPokemon()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the most threatening trainer in range of this turret.
     */
    private Trainer findPriorityTarget(Turret turret, ArrayList<Trainer> trainers) {
        Pokemon currentPoke = turret.getAssignedPokemon();
        double range = (currentPoke != null) ? currentPoke.getRange() : 85.0;

        Trainer bestTarget = null;
        double minDistanceToGym = Double.MAX_VALUE;

        for (Trainer t : trainers) {
            double distToTurret = Math.hypot((turret.getX() + 8) - (t.getX() + 8),
                                             (turret.getY() + 8) - (t.getY() + 8));
            if (distToTurret <= range + 10) { // nearby or in range
                // Bosses always get highest priority
                if (t.isBoss()) {
                    return t;
                }
                // Otherwise target enemy furthest along track (closest to gym coords 170, 95)
                double distToGym = Math.hypot(t.getX() - 170, t.getY() - 95);
                if (distToGym < minDistanceToGym) {
                    minDistanceToGym = distToGym;
                    bestTarget = t;
                }
            }
        }
        return bestTarget;
    }

    /**
     * Determines which unlocked Pokémon provides the best damage against the given enemy type.
     */
    private int findBestCounterPokemon(PokemonType enemyType) {
        List<Pokemon> unlocked = store.getUnlockedPokemon();
        if (unlocked.isEmpty()) {
            return -1;
        }

        int bestIndex = unlocked.size() - 1;
        double highestEstimatedDps = -1;

        for (int i = 0; i < unlocked.size(); i++) {
            Pokemon p = unlocked.get(i);
            double mult = p.getType().getMultiplierAgainst(enemyType);
            // Effective DPS = (base damage * multiplier) / cooldown
            double effectiveDps = (p.getAttack() * mult) / (double) Math.max(1, p.getCooldown());

            // Bonus weight for status effects (Paralysis on Electric, Burn on Fire)
            if (p.getType() == PokemonType.ELECTRIC) effectiveDps *= 1.15;
            if (p.getType() == PokemonType.FIRE) effectiveDps *= 1.10;

            if (effectiveDps > highestEstimatedDps) {
                highestEstimatedDps = effectiveDps;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private void optimizeTurretAssignments(PokemonType upcomingType) {
        for (Turret t : entities.getTurrets()) {
            if (upcomingType != null) {
                int counterIdx = findBestCounterPokemon(upcomingType);
                if (counterIdx != -1) {
                    t.setAssignedPokemon(counterIdx);
                    continue;
                }
            }
            // By default assign the strongest unlocked Pokémon
            t.setAssignedPokemon(store.getUnlockedPokemon().size() - 1);
        }
    }

    public void onWaveCompleted(int completedLevel) {
        int currentHp = entities.getTowerHealth();
        if (currentHp == previousHealth) {
            cleanWavesCount++;
            addLog("[W" + completedLevel + "] ⭐ ONDATA PERFETTA! (0 Danni subiti)");
        }
        previousHealth = currentHp;
        waveWaitTicks = 0;
    }

    public void addLog(String log) {
        if (recentLogs.size() >= 5) {
            recentLogs.remove(0);
        }
        recentLogs.add(log);
    }

    public List<String> getRecentLogs() {
        return recentLogs;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public int getCleanWavesCount() {
        return cleanWavesCount;
    }

    public int getTotalDamageSustained() {
        return totalDamageSustained;
    }

    public double calculateTotalDps() {
        double dps = 0;
        for (Turret t : entities.getTurrets()) {
            Pokemon p = t.getAssignedPokemon();
            if (p != null) {
                dps += (p.getAttack() * 100.0) / (double) Math.max(1, p.getCooldown());
            }
        }
        return Math.round(dps * 10.0) / 10.0;
    }

    public String getBalanceEvaluation(int currentLevel) {
        if (totalDamageSustained == 0) {
            if (currentLevel >= 5) {
                return "🌟 ECCELLENTE: Difesa impenetrabile";
            }
            return "✅ OTTIMO: Nessun danno subito";
        } else if (totalDamageSustained <= 5) {
            return "⚖️ BILANCIATO: Sfida moderata";
        } else if (totalDamageSustained <= 15) {
            return "⚠️ DIFFICILE: Perdite sensibili";
        } else {
            return "🚨 CRITICO: Difese sotto pressione";
        }
    }
}
