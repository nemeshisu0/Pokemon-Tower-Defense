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
    private Integer towerHealth = 1000;
    private Store store;

    public Entities(Store store) {
        this.store = store;
        pikas.setMouseTransparent(true);
        allTrainers.setMouseTransparent(true);
        combatTextGroup.setMouseTransparent(true);
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
        Trainer newTrainer = randomTrainer(level);
        trainers.add(newTrainer);
        trainersWballs.add(newTrainer.getGroup());
        newTrainer.update(newTrainer.getGroup());
        allTrainers.getChildren().add(newTrainer.getGroup());
    }

    public Trainer randomTrainer(int level) {
        // 5 defined trainer archetypes with elemental types
        int rand = (int) (Math.random() * 5);
        String pic;
        String name;
        PokemonType type;
        int hp;

        switch (rand) {
            case 0:
                pic = "/Resource/trainer1.png";
                name = "Pescatore";
                type = PokemonType.WATER;
                hp = Math.max(8, 11 * level - 3);
                break;
            case 1:
                pic = "/Resource/trainer2.png";
                name = "Centauro";
                type = PokemonType.FIRE;
                hp = Math.max(10, 13 * level - 2);
                break;
            case 2:
                pic = "/Resource/trainer3.png";
                name = "Pigliamosche";
                type = PokemonType.GRASS;
                hp = Math.max(6, 9 * level - 2);
                break;
            case 3:
                pic = "/Resource/trainer4.png";
                name = "Marinaio";
                type = PokemonType.WATER;
                hp = Math.max(12, 14 * level);
                break;
            case 4:
            default:
                pic = "/Resource/trainer5.png";
                name = "Fantallenatore";
                type = PokemonType.NORMAL;
                hp = Math.max(15, 16 * level + 5);
                break;
        }

        return new Trainer(pic, 202, 800, hp, name, type);
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
                towerHealth--;
            }
            if (trainer.getHealth() < 1) {
                dead.add(trainer);
            }
        }
        // Kill trainers
        for (Trainer t : dead) {
            allTrainers.getChildren().remove(t.getImageView());
            int idx = trainers.indexOf(t);
            if (idx != -1) {
                Group ballGroup = trainersWballs.get(idx);
                allTrainers.getChildren().remove(ballGroup);
                trainersWballs.remove(idx);
                trainers.remove(idx);
            }
            if (store != null) {
                store.spend(-50);
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
        towerHealth = 1000;
    }
}