package com.pokemontd;

import java.util.ArrayList;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class Entities {
    private ArrayList<Turret> turrets = new ArrayList<Turret>();
    private ArrayList<Attacker> atkers = new ArrayList<Attacker>();
    final private Group pikas = new Group();
    final private Group turretGroup = new Group();
    final private Group allTrainers = new Group();
    final private Group combatTextGroup = new Group();
    final private Group all = new Group(pikas, turretGroup, allTrainers, combatTextGroup);
    final private ArrayList<Trainer> trainers = new ArrayList<Trainer>();
    final private ArrayList<Group> trainersWballs = new ArrayList<Group>();
    private final int MAX_TOWER_HEALTH = 25;
    private Integer towerHealth = MAX_TOWER_HEALTH;
    private Store store;
    private GameTelemetry telemetry;

    public Entities(Store store) {
        this.store = store;
        pikas.setMouseTransparent(true);
        allTrainers.setMouseTransparent(true);
        combatTextGroup.setMouseTransparent(true);
    }

    public void setTelemetry(GameTelemetry telemetry) {
        this.telemetry = telemetry;
    }

    public void updateAll() {
        for (Attacker a : atkers) {
            a.update();
        }
    }

    public void add(Turret t) {
        turrets.add(t);
        turretGroup.getChildren().add(t.getView());
    }

    public ArrayList<Turret> getTurrets() {
        return turrets;
    }

    public void add(Attacker a) {
        if (a != null) {
            pikas.getChildren().add(a.getImageView());
            atkers.add(a);
        }
    }

    public void remove(Attacker a) {
        if (a != null) {
            pikas.getChildren().remove(a.getImageView());
            atkers.remove(a);
        }
    }

    public ArrayList<Attacker> getAtkers() {
        return atkers;
    }

    public Group getAll() {
        return all;
    }

    public ArrayList<Trainer> getTrainers() {
        return trainers;
    }

    public ArrayList<Group> getTrainersWballs() {
        return trainersWballs;
    }

    public int numAttackers() {
        return atkers.size();
    }

    public void createNewTrainer(int level) {
        createNewTrainer(level, false);
    }

    public void createNewTrainer(int level, boolean isBoss) {
        Trainer newTrainer = isBoss ? createBossTrainer(level) : randomTrainer(level);
        if (telemetry != null) {
            telemetry.recordEnemySpawned(newTrainer);
        }
        trainers.add(newTrainer);
        trainersWballs.add(newTrainer.getGroup());
        newTrainer.update(newTrainer.getGroup());
        allTrainers.getChildren().add(newTrainer.getGroup());
    }

    public Trainer createBossTrainer(int level) {
        int bossTier = (level >= 64) ? 13 : Math.max(1, level / 5);
        String name;
        PokemonType type;
        int hp;

        switch (bossTier) {
            case 1: // Wave 5
                name = "Brock (Capopalestra)";
                type = PokemonType.ROCK;
                hp = 210; // Was 280 (leaked by 28 HP, 210 ensures clean defeat)
                break;
            case 2: // Wave 10
                name = "Misty (Capopalestra)";
                type = PokemonType.WATER;
                hp = 750;
                break;
            case 3: // Wave 15
                name = "Lt. Surge (Capopalestra)";
                type = PokemonType.ELECTRIC;
                hp = 1800;
                break;
            case 4: // Wave 20
                name = "Erika (Capopalestra)";
                type = PokemonType.GRASS;
                hp = 3200;
                break;
            case 5: // Wave 25
                name = "Koga (Capopalestra)";
                type = PokemonType.GRASS;
                hp = 4200;
                break;
            case 6: // Wave 30
                name = "Sabrina (Capopalestra)";
                type = PokemonType.NORMAL;
                hp = 5400;
                break;
            case 7: // Wave 35
                name = "Blaine (Capopalestra)";
                type = PokemonType.FIRE;
                hp = 6800;
                break;
            case 8: // Wave 40
                name = "Giovanni (Capo Rocket)";
                type = PokemonType.ROCK;
                hp = 8400;
                break;
            case 9: // Wave 45
                name = "Lorelei (Superquattro)";
                type = PokemonType.WATER;
                hp = 10500;
                break;
            case 10: // Wave 50
                name = "Bruno (Superquattro)";
                type = PokemonType.ROCK;
                hp = 12500;
                break;
            case 11: // Wave 55
                name = "Agatha (Superquattro)";
                type = PokemonType.GRASS;
                hp = 15000;
                break;
            case 12: // Wave 60
                name = "Lance (Superquattro)";
                type = PokemonType.FIRE;
                hp = 17500;
                break;
            case 13: // Wave 64 Boss Finale
            default:
                name = "Blue (Campione Supremo)";
                type = PokemonType.NORMAL;
                hp = 20000;
                break;
        }

        return new Trainer("/Resource/gymleader.png", 202, 800, hp, name, type, 1, true, level);
    }

    public boolean isLocationOccupied(int x, int y, double minDistance) {
        for (Turret t : turrets) {
            if (Math.hypot(t.getX() - x, t.getY() - y) < minDistance) {
                return true;
            }
        }
        return false;
    }

    public Trainer randomTrainer(int level) {
        int maxTypeIndex = (level == 1) ? 2 : (level == 2 ? 3 : (level == 3 ? 5 : 6));
        int rand = (int) (Math.random() * maxTypeIndex);
        String pic;
        String name;
        PokemonType type;
        int hp;
        int speed = 1;

        switch (rand) {
            case 0:
                pic = "/Resource/trainer1.png";
                name = "Pescatore";
                type = PokemonType.WATER;
                hp = (level == 1) ? 9 : (level == 2 ? 13 : (int) (8 + 5 * Math.pow(level, 1.22)));
                break;
            case 1:
                pic = "/Resource/trainer5.png";
                name = "Fantallenatore";
                type = PokemonType.NORMAL;
                hp = (level == 1) ? 10 : (level == 2 ? 14 : (int) (9 + 6 * Math.pow(level, 1.22)));
                break;
            case 2:
                pic = "/Resource/trainer2.png";
                name = "Centauro";
                type = PokemonType.FIRE;
                hp = (level <= 2) ? 14 : (int) (11 + 7 * Math.pow(level, 1.24));
                break;
            case 3:
                pic = "/Resource/trainer3.png";
                name = "Pigliamosche";
                type = PokemonType.GRASS;
                hp = (int) (6 + 4 * Math.pow(level, 1.18));
                speed = 2; // Fast runner scout!
                break;
            case 4:
                pic = "/Resource/trainer4.png";
                name = "Marinaio";
                type = PokemonType.WATER;
                hp = (int) (14 + 7 * Math.pow(level, 1.25));
                break;
            case 5:
            default:
                pic = "/Resource/trainer5.png";
                name = "Montanaro";
                type = PokemonType.ROCK; // Rock armor resists Rattata!
                hp = (int) (16 + 8 * Math.pow(level, 1.26));
                break;
        }

        return new Trainer(pic, 202, 800, hp, name, type, speed, false, level);
    }

    public void attackTrainers() {
        for (int i = 0; i < turrets.size(); i++) {
            Turret turret = turrets.get(i);
            turret.reload();
            Pokemon poke = turret.getAssignedPokemon();
            if (poke == null) {
                continue;
            }

            for (int j = 0; j < trainers.size(); j++) {
                if (turret.hasShot()) {
                    break;
                }
                Trainer trainer = trainers.get(j);

                // Distance between turret center and trainer center
                double dist = Math.hypot((turret.getX() + 8) - (trainer.getX() + 8),
                                         (turret.getY() + 8) - (trainer.getY() + 8));

                if (turret.shotTimer() >= poke.getCooldown()) {
                    if (dist <= poke.getRange()) {
                        Attacker atker = turret.shoot(trainer);
                        if (atker != null) {
                            add(atker);
                            SoundManager.playShoot();
                            break;
                        }
                    }
                } else {
                    turret.addToShotTimer();
                }
            }
        }
    }

    public void moveTrainers() {
        ArrayList<Trainer> dead = new ArrayList<Trainer>();
        for (int k = 0; k < trainers.size(); k++) {
            Trainer trainer = trainers.get(k);
            trainer.tickStatus();
            trainer.move();
            trainer.update(trainersWballs.get(k));

            if (trainer.getX() < 190 && trainer.getY() < 110) {
                int dmg = trainer.isBoss() ? 3 : 1;
                towerHealth = Math.max(0, towerHealth - dmg);
                if (telemetry != null) {
                    telemetry.recordEnemyLeak(trainer, dmg);
                }
                SoundManager.playHit();
                spawnFloatingText(trainer.getX(), trainer.getY() - 15, "-" + dmg + " ❤️ PALESTRA!", Color.RED);
                dead.add(trainer);
            } else if (trainer.getHealth() < 1) {
                if (telemetry != null) {
                    telemetry.recordEnemyDefeated(trainer);
                }
                dead.add(trainer);
            }
        }
        // Remove dead / breached trainers
        for (Trainer t : dead) {
            allTrainers.getChildren().remove(t.getImageView());
            int idx = trainers.indexOf(t);
            if (idx != -1) {
                Group ballGroup = trainersWballs.get(idx);
                allTrainers.getChildren().remove(ballGroup);
                trainersWballs.remove(idx);
                trainers.remove(idx);
            }
            // Reward bounty only if trainer was defeated
            if (store != null && t.getHealth() < 1) {
                int bounty = t.isBoss() ? (150 + t.getLevel() * 15) : (14 + t.getLevel());
                store.spend(-bounty);
                spawnFloatingText(t.getX(), t.getY() - 10, "+$" + bounty, Color.GOLD);
                if (t.isBoss()) {
                    SoundManager.playWaveStart();
                    spawnFloatingText(t.getX(), t.getY() - 28, "👑 CAPOPALESTRA SCONFITTO!", Color.GOLD);
                }
            }
        }
    }

    public void moveAttackers() {
        ArrayList<Attacker> pokedone = new ArrayList<Attacker>();
        for (Attacker a : atkers) {
            if (a.hasReached()) {
                Pokemon poke = a.getPokemon();
                PokemonType pType = (poke != null) ? poke.getType() : PokemonType.NORMAL;
                Trainer target = a.getTarget();

                DamageResult res = target.takeDamage(a.getAtk(), pType);

                if (telemetry != null) {
                    telemetry.recordDamageDealt(poke != null ? poke.getName() : "Sconosciuto",
                                                res.damage,
                                                res.multiplier > 1.0,
                                                res.appliedParalysis,
                                                res.appliedBurn);
                }

                // Sound & Floating combat text
                if (res.multiplier > 1.0) {
                    SoundManager.playSuperEffective();
                    spawnFloatingText(target.getX(), target.getY() - 10, "-" + res.damage + " (2x!)", Color.GOLD);
                } else if (res.multiplier < 1.0) {
                    SoundManager.playHit();
                    spawnFloatingText(target.getX(), target.getY() - 10, "-" + res.damage + " (0.5x)", Color.LIGHTGRAY);
                } else {
                    SoundManager.playHit();
                    spawnFloatingText(target.getX(), target.getY() - 10, "-" + res.damage, Color.WHITE);
                }

                if (res.appliedParalysis) {
                    spawnFloatingText(target.getX() + 10, target.getY() - 25, "⚡ PARALISI!", Color.YELLOW);
                }
                if (res.appliedBurn) {
                    spawnFloatingText(target.getX() + 10, target.getY() - 25, "🔥 SCOTTATO!", Color.ORANGERED);
                }

                pokedone.add(a);
            } else {
                a.updatePath(a.getTarget());
                a.move();
            }
        }
        for (Attacker a : pokedone) {
            this.remove(a);
        }
    }

    private void spawnFloatingText(double x, double y, String text, Color color) {
        Label lbl = new Label(text);
        lbl.setTextFill(color);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-effect: dropshadow(one-pass-box, black, 2, 1, 0, 0);");
        lbl.setTranslateX(x);
        lbl.setTranslateY(y);
        combatTextGroup.getChildren().add(lbl);

        TranslateTransition tt = new TranslateTransition(Duration.millis(700), lbl);
        tt.setByY(-20);
        FadeTransition ft = new FadeTransition(Duration.millis(700), lbl);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> combatTextGroup.getChildren().remove(lbl));
        pt.play();
    }

    public int getMaxTowerHealth() {
        return MAX_TOWER_HEALTH;
    }

    public Integer getTowerHealth() {
        return towerHealth;
    }

    public void reset() {
        for (Group g : trainersWballs) {
            allTrainers.getChildren().remove(g);
        }
        trainers.clear();
        trainersWballs.clear();

        for (Attacker a : atkers) {
            pikas.getChildren().remove(a.getImageView());
        }
        atkers.clear();

        for (Turret t : turrets) {
            turretGroup.getChildren().remove(t.getView());
        }
        turrets.clear();

        combatTextGroup.getChildren().clear();
        towerHealth = MAX_TOWER_HEALTH;
    }
}