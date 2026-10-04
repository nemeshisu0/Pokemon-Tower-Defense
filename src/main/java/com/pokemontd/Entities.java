package com.pokemontd;

import java.util.ArrayList;
import javafx.scene.Group;

public class Entities {
    private ArrayList<Turret> turrets = new ArrayList<Turret>();
    private ArrayList<Attacker> atkers = new ArrayList<Attacker>();
    final private Group pikas = new Group();
    final private Group turretGroup = new Group();
    final private Group allTrainers = new Group();
    final private Group all = new Group(pikas, turretGroup, allTrainers);
    final private ArrayList<Trainer> trainers = new ArrayList<Trainer>();
    final private ArrayList<Group> trainersWballs = new ArrayList<Group>();
    private Integer towerHealth = 1000;
    private Store store;

    public Entities(Store store) {
        this.store = store;
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
        ArrayList<String> pics = new ArrayList<String>();
        pics.add("/Resource/trainer1.png");
        pics.add("/Resource/trainer2.png");
        pics.add("/Resource/trainer3.png");
        pics.add("/Resource/trainer4.png");
        pics.add("/Resource/trainer5.png");
        int rand = (int) (Math.random() * pics.size());
        int health = Math.max(5, 12 * level - 5);
        Trainer trainer = new Trainer(pics.get(rand), 202, 800, health);
        return trainer;
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
                        Attacker pika = turret.shoot(trainer);
                        if (pika != null) {
                            add(pika);
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
                a.getTarget().damage(a.getAtk());
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

        towerHealth = 1000;
    }
}