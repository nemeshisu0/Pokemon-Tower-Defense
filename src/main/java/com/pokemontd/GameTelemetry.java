package com.pokemontd;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GameTelemetry: Collects granular telemetry during gameplay and exports
 * comprehensive balance reports (Markdown and JSON) for game balancing and AI analysis.
 */
public class GameTelemetry {

    public static class WaveSummary {
        public int waveNumber;
        public String waveTypeLabel;
        public boolean isBossWave;
        public String bossName;
        public int spawnedCount = 0;
        public int killedCount = 0;
        public int leakedCount = 0;
        public int damageTakenGym = 0;
        public long durationTicks = 0;
        public int startGold = 0;
        public int endGold = 0;
        public int turretCount = 0;
        public String activeRoster = "";
        public double estimatedDps = 0.0;
        public List<String> leakDetails = new ArrayList<>();
        public String outcome = "IN_CORSO";
        public String balanceNote = "";
    }

    public static class PokemonStats {
        public String name;
        public long totalDamage = 0;
        public int superEffectiveHits = 0;
        public int paralysisApplied = 0;
        public int burnApplied = 0;
        public int shotsFired = 0;

        public PokemonStats(String name) {
            this.name = name;
        }
    }

    public static class EnemyStats {
        public String name;
        public PokemonType type;
        public int totalSpawned = 0;
        public int totalKilled = 0;
        public int totalLeaks = 0;

        public EnemyStats(String name, PokemonType type) {
            this.name = name;
            this.type = type;
        }
    }

    private final List<WaveSummary> waveHistory = new ArrayList<>();
    private final Map<String, PokemonStats> pokemonStatsMap = new LinkedHashMap<>();
    private final Map<String, EnemyStats> enemyStatsMap = new LinkedHashMap<>();

    private WaveSummary currentWaveSummary = null;
    private long waveStartTick = 0;
    private long globalTicks = 0;
    private final String sessionTimestamp;
    private boolean isBotControlled = false;
    private int cleanWavesTotal = 0;
    private int totalDamageGym = 0;
    private int totalGoldEarned = 0;
    private String lastExportStatus = "Nessun report esportato";

    public GameTelemetry() {
        this.sessionTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        initDefaultStats();
    }

    private void initDefaultStats() {
        pokemonStatsMap.put("Rattata", new PokemonStats("Rattata"));
        pokemonStatsMap.put("Pikachu", new PokemonStats("Pikachu"));
        pokemonStatsMap.put("Squirtle", new PokemonStats("Squirtle"));
        pokemonStatsMap.put("Charizard", new PokemonStats("Charizard"));
    }

    public void setBotControlled(boolean botControlled) {
        this.isBotControlled = botControlled;
    }

    public void tick() {
        globalTicks++;
        if (currentWaveSummary != null) {
            currentWaveSummary.durationTicks++;
        }
    }

    public void onWaveStarted(int wave, int startingGold, int turretCount, String activeRoster, double dps, boolean isBoss, String bossName) {
        currentWaveSummary = new WaveSummary();
        currentWaveSummary.waveNumber = wave;
        currentWaveSummary.startGold = startingGold;
        currentWaveSummary.turretCount = turretCount;
        currentWaveSummary.activeRoster = activeRoster;
        currentWaveSummary.estimatedDps = dps;
        currentWaveSummary.isBossWave = isBoss;
        currentWaveSummary.bossName = bossName;
        currentWaveSummary.waveTypeLabel = isBoss ? ("👑 " + bossName) : ("Ondata " + wave);
        waveStartTick = globalTicks;
    }

    public void recordEnemySpawned(Trainer t) {
        if (currentWaveSummary != null) {
            currentWaveSummary.spawnedCount++;
        }
        EnemyStats es = enemyStatsMap.computeIfAbsent(t.getTrainerName(), k -> new EnemyStats(t.getTrainerName(), t.getType()));
        es.totalSpawned++;
    }

    public void recordDamageDealt(String pokemonName, int damage, boolean superEffective, boolean paralysis, boolean burn) {
        PokemonStats ps = pokemonStatsMap.computeIfAbsent(pokemonName, PokemonStats::new);
        ps.totalDamage += damage;
        ps.shotsFired++;
        if (superEffective) ps.superEffectiveHits++;
        if (paralysis) ps.paralysisApplied++;
        if (burn) ps.burnApplied++;
    }

    public void recordEnemyDefeated(Trainer t) {
        if (currentWaveSummary != null) {
            currentWaveSummary.killedCount++;
        }
        EnemyStats es = enemyStatsMap.computeIfAbsent(t.getTrainerName(), k -> new EnemyStats(t.getTrainerName(), t.getType()));
        es.totalKilled++;
    }

    public void recordEnemyLeak(Trainer t, int dmg) {
        if (currentWaveSummary != null) {
            currentWaveSummary.leakedCount++;
            currentWaveSummary.damageTakenGym += dmg;
            currentWaveSummary.leakDetails.add(t.getTrainerName() + " (HP residui: " + t.getHealth() + ")");
        }
        totalDamageGym += dmg;
        EnemyStats es = enemyStatsMap.computeIfAbsent(t.getTrainerName(), k -> new EnemyStats(t.getTrainerName(), t.getType()));
        es.totalLeaks++;
    }

    public void onWaveCompleted(int wave, int endingGold, int gymHp, int maxGymHp) {
        if (currentWaveSummary == null) return;

        currentWaveSummary.endGold = endingGold;
        if (currentWaveSummary.damageTakenGym == 0) {
            currentWaveSummary.outcome = "⭐ PERFETTA";
            cleanWavesTotal++;
        } else {
            currentWaveSummary.outcome = "⚠️ " + currentWaveSummary.damageTakenGym + " LEAK";
        }

        // Automatic Wave Diagnosis
        if (currentWaveSummary.damageTakenGym == 0) {
            currentWaveSummary.balanceNote = "Difesa solida: tutti i nemici neutralizzati tempestivamente.";
        } else {
            currentWaveSummary.balanceNote = "Falla difensiva: " + String.join(", ", currentWaveSummary.leakDetails);
        }

        waveHistory.add(currentWaveSummary);
        currentWaveSummary = null;

        // Auto-export reports after each wave
        exportReports(gymHp, maxGymHp, "IN_CORSO");
    }

    public void onGameOver(int wave, int finalGold, int maxGymHp) {
        if (currentWaveSummary != null) {
            currentWaveSummary.endGold = finalGold;
            currentWaveSummary.outcome = "🚨 PALESTRA CADUTA";
            currentWaveSummary.balanceNote = "Sconfitta al livello " + wave + ". I nemici hanno superato la soglia di DPS.";
            waveHistory.add(currentWaveSummary);
            currentWaveSummary = null;
        }
        exportReports(0, maxGymHp, "SCONFITTA");
    }

    public void reset() {
        waveHistory.clear();
        pokemonStatsMap.clear();
        enemyStatsMap.clear();
        initDefaultStats();
        currentWaveSummary = null;
        globalTicks = 0;
        cleanWavesTotal = 0;
        totalDamageGym = 0;
        totalGoldEarned = 0;
    }

    /**
     * Safely exports both balance_report.md and balance_telemetry.json to current workspace root.
     */
    public synchronized boolean exportReports(int currentGymHp, int maxGymHp, String matchStatus) {
        try {
            File mdFile = new File("balance_report.md");
            File jsonFile = new File("balance_telemetry.json");

            exportMarkdown(mdFile, currentGymHp, maxGymHp, matchStatus);
            exportJson(jsonFile, currentGymHp, maxGymHp, matchStatus);

            lastExportStatus = "Salvato: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " (W" + (waveHistory.size()) + ")";
            return true;
        } catch (Exception ex) {
            lastExportStatus = "Errore salvataggio: " + ex.getMessage();
            ex.printStackTrace();
            return false;
        }
    }

    private void exportMarkdown(File targetFile, int gymHp, int maxGymHp, String status) throws Exception {
        long totalDamageAll = 0;
        for (PokemonStats ps : pokemonStatsMap.values()) {
            totalDamageAll += ps.totalDamage;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            writer.println("# 📊 Rapporto Telemetria e Bilanciamento - Pokémon Tower Defense");
            writer.println();
            writer.println("* **Data Sessione**: `" + sessionTimestamp + "`");
            writer.println("* **Modalità di Gioco**: `" + (isBotControlled ? "🤖 Bot AI Autonomo (Auto-Play)" : "👤 Giocatore Manuale") + "`");
            writer.println("* **Stato Partita**: `" + status + "`");
            writer.println("* **Ondata Massima Raggiunta**: `Ondata " + waveHistory.size() + "`");
            writer.println("* **Salute Palestra**: `" + gymHp + " / " + maxGymHp + "`");
            writer.println("* **Danni Subiti Totali**: `-" + totalDamageGym + " HP`");
            int totalWaves = waveHistory.size();
            double perfectRate = totalWaves > 0 ? (cleanWavesTotal * 100.0 / totalWaves) : 0;
            writer.println("* **Ondate Perfette (0 Danni)**: `" + cleanWavesTotal + " / " + totalWaves + " (" + String.format(Locale.US, "%.1f", perfectRate) + "%)`");
            writer.println();
            writer.println("---");
            writer.println();

            // Table 1: Per-Wave Breakdown
            writer.println("## 📈 Dettaglio Ondata per Ondata");
            writer.println();
            writer.println("| Ondata | Tipologia | Nemici | Uccisi | Leak | Danno Palestra | Durata (s) | Oro Fine | Torrette | DPS Stimato | Esito | Note Bilanciamento |");
            writer.println("|:------:|:---------:|:------:|:------:|:----:|:--------------:|:----------:|:--------:|:--------:|:-----------:|:-----:|:-------------------|");

            for (WaveSummary ws : waveHistory) {
                double durationSec = ws.durationTicks * 0.01; // based on 10ms ticks approx
                writer.println(String.format(Locale.US,
                    "| %d | %s | %d | %d | %d | %s%d | %.1fs | $%d | %d | %.1f | %s | %s |",
                    ws.waveNumber,
                    ws.waveTypeLabel,
                    ws.spawnedCount,
                    ws.killedCount,
                    ws.leakedCount,
                    ws.damageTakenGym > 0 ? "-" : "",
                    ws.damageTakenGym,
                    durationSec,
                    ws.endGold,
                    ws.turretCount,
                    ws.estimatedDps,
                    ws.outcome,
                    ws.balanceNote
                ));
            }
            writer.println();
            writer.println("---");
            writer.println();

            // Table 2: Pokémon Performance
            writer.println("## ⚡ Efficacia e Quota Danno Pokémon");
            writer.println();
            writer.println("| Pokémon | Danno Totale | Quota sul Totale | Colpi Sparati | Colpi Super-Efficaci (2x) | Paralisi Inflitte | Scottature Inflitte |");
            writer.println("|:-------:|:------------:|:----------------:|:-------------:|:-------------------------:|:-----------------:|:-------------------:|");

            for (PokemonStats ps : pokemonStatsMap.values()) {
                double pct = totalDamageAll > 0 ? (ps.totalDamage * 100.0 / totalDamageAll) : 0.0;
                writer.println(String.format(Locale.US,
                    "| %s | %,d | %.1f%% | %,d | %,d | %,d | %,d |",
                    ps.name,
                    ps.totalDamage,
                    pct,
                    ps.shotsFired,
                    ps.superEffectiveHits,
                    ps.paralysisApplied,
                    ps.burnApplied
                ));
            }
            writer.println();
            writer.println("---");
            writer.println();

            // Table 3: Enemy Threat Analysis
            writer.println("## 👾 Analisi Minaccia e Letalità Nemici");
            writer.println();
            writer.println("| Nemico | Tipo | Generati | Uccisi | Leak a Segno | Tasso di Minaccia (%) |");
            writer.println("|:------:|:----:|:--------:|:------:|:-------------:|:---------------------:|");

            for (EnemyStats es : enemyStatsMap.values()) {
                double leakRate = es.totalSpawned > 0 ? (es.totalLeaks * 100.0 / es.totalSpawned) : 0.0;
                writer.println(String.format(Locale.US,
                    "| %s | %s | %d | %d | %d | %.1f%% |",
                    es.name,
                    es.type != null ? es.type.getDisplayName() : "Normale",
                    es.totalSpawned,
                    es.totalKilled,
                    es.totalLeaks,
                    leakRate
                ));
            }
            writer.println();
            writer.println("---");
            writer.println();

            // Section 4: Automated Balancing Diagnosis
            writer.println("## 💡 Diagnosi di Bilanciamento & Raccomandazioni");
            writer.println();
            generateBalancingAdvice(writer, totalWaves);
        }
    }

    private void generateBalancingAdvice(PrintWriter writer, int totalWaves) {
        boolean earlyLeak = false;
        boolean bossProblem = false;
        String worstEnemy = "";
        int highestLeaks = 0;

        for (WaveSummary ws : waveHistory) {
            if (ws.waveNumber <= 2 && ws.damageTakenGym > 0) {
                earlyLeak = true;
            }
            if (ws.isBossWave && ws.damageTakenGym > 0) {
                bossProblem = true;
            }
        }

        for (EnemyStats es : enemyStatsMap.values()) {
            if (es.totalLeaks > highestLeaks) {
                highestLeaks = es.totalLeaks;
                worstEnemy = es.name;
            }
        }

        if (totalWaves == 0) {
            writer.println("* ℹ️ *Nessuna ondata completata finora. Avvia o fai giocare il bot per raccogliere dati.*");
            return;
        }

        if (!earlyLeak) {
            writer.println("* ✅ **Early Game (Ondata 1 & 2)**: Perfettamente equilibrato. 0 perdite con Rattata ben posizionato.");
        } else {
            writer.println("* 🔴 **Attenzione Early Game**: Registrato danno nelle prime 2 ondate. Verificare HP o cooldown di Rattata.");
        }

        if (highestLeaks > 0) {
            writer.println("* ⚠️ **Nemico più insidioso**: `" + worstEnemy + "` con " + highestLeaks + " leak causati. Suggerimento: verificare se l'elemento di contrasto o il DPS dell'ondata sono tempestivi.");
        } else {
            writer.println("* 🌟 **Difesa Impenetrabile**: Nessun nemico è riuscito a violare la palestra nelle ondate giocate finora.");
        }

        if (bossProblem) {
            writer.println("* 👑 **Criticità Boss**: Almeno un Capopalestra ha inflitto danno. Si raccomanda di verificare che lo sblocco di Squirtle (per Brock) o Pikachu (per Misty) sia accessibile economicamente.");
        } else if (totalWaves >= 5) {
            writer.println("* ✅ **Capipalestra Superati**: I boss incontrati finora sono stati sconfitti regolarmente.");
        }

        if (totalWaves >= 4) {
            PokemonStats pika = pokemonStatsMap.get("Pikachu");
            PokemonStats squirtle = pokemonStatsMap.get("Squirtle");
            if (pika != null && pika.totalDamage > 0) {
                writer.println("* ⚡ **Power System Elettrico**: Pikachu è stato integrato con successo con " + pika.totalDamage + " danni inflitti.");
            }
            if (squirtle != null && squirtle.totalDamage > 0) {
                writer.println("* 💧 **Power System Idrico**: Squirtle ha contribuito con " + squirtle.totalDamage + " danni (super-efficace contro Roccia/Fuoco).");
            }
        }
    }

    private void exportJson(File targetFile, int gymHp, int maxGymHp, String status) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"sessionTimestamp\": \"").append(sessionTimestamp).append("\",\n");
        sb.append("  \"mode\": \"").append(isBotControlled ? "BOT_AI" : "MANUAL").append("\",\n");
        sb.append("  \"status\": \"").append(status).append("\",\n");
        sb.append("  \"highestWave\": ").append(waveHistory.size()).append(",\n");
        sb.append("  \"gymHealth\": ").append(gymHp).append(",\n");
        sb.append("  \"maxGymHealth\": ").append(maxGymHp).append(",\n");
        sb.append("  \"totalDamageGym\": ").append(totalDamageGym).append(",\n");
        sb.append("  \"cleanWaves\": ").append(cleanWavesTotal).append(",\n");

        // Waves Array
        sb.append("  \"waves\": [\n");
        for (int i = 0; i < waveHistory.size(); i++) {
            WaveSummary ws = waveHistory.get(i);
            sb.append("    {\n");
            sb.append("      \"wave\": ").append(ws.waveNumber).append(",\n");
            sb.append("      \"isBoss\": ").append(ws.isBossWave).append(",\n");
            sb.append("      \"spawned\": ").append(ws.spawnedCount).append(",\n");
            sb.append("      \"killed\": ").append(ws.killedCount).append(",\n");
            sb.append("      \"leaked\": ").append(ws.leakedCount).append(",\n");
            sb.append("      \"damageGym\": ").append(ws.damageTakenGym).append(",\n");
            sb.append("      \"durationTicks\": ").append(ws.durationTicks).append(",\n");
            sb.append("      \"endGold\": ").append(ws.endGold).append(",\n");
            sb.append("      \"turrets\": ").append(ws.turretCount).append(",\n");
            sb.append("      \"dps\": ").append(ws.estimatedDps).append(",\n");
            sb.append("      \"outcome\": \"").append(ws.outcome).append("\"\n");
            sb.append("    }").append(i < waveHistory.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // Pokemon Stats
        sb.append("  \"pokemonStats\": {\n");
        int pIdx = 0;
        int pSize = pokemonStatsMap.size();
        for (PokemonStats ps : pokemonStatsMap.values()) {
            pIdx++;
            sb.append("    \"").append(ps.name).append("\": {\n");
            sb.append("      \"damage\": ").append(ps.totalDamage).append(",\n");
            sb.append("      \"shots\": ").append(ps.shotsFired).append(",\n");
            sb.append("      \"superEffective\": ").append(ps.superEffectiveHits).append(",\n");
            sb.append("      \"paralysis\": ").append(ps.paralysisApplied).append(",\n");
            sb.append("      \"burn\": ").append(ps.burnApplied).append("\n");
            sb.append("    }").append(pIdx < pSize ? "," : "").append("\n");
        }
        sb.append("  },\n");

        // Enemy Stats
        sb.append("  \"enemyStats\": {\n");
        int eIdx = 0;
        int eSize = enemyStatsMap.size();
        for (EnemyStats es : enemyStatsMap.values()) {
            eIdx++;
            sb.append("    \"").append(es.name).append("\": {\n");
            sb.append("      \"spawned\": ").append(es.totalSpawned).append(",\n");
            sb.append("      \"killed\": ").append(es.totalKilled).append(",\n");
            sb.append("      \"leaked\": ").append(es.totalLeaks).append("\n");
            sb.append("    }").append(eIdx < eSize ? "," : "").append("\n");
        }
        sb.append("  }\n");
        sb.append("}\n");

        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            pw.print(sb.toString());
        }
    }

    public String getLastExportStatus() {
        return lastExportStatus;
    }
}
