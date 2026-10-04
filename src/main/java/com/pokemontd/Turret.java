package com.pokemontd;

import java.util.List;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Turret extends Stationary {
    private List<Pokemon> roster;
    private int assignedIndex = 0;
    private boolean hasShot = false;
    private int shotTimer = 0;
    private Group viewGroup;
    private ImageView badgeView;

    public Turret(String location, int xstart, int ystart, List<Pokemon> roster) {
        super(location, xstart, ystart);
        this.roster = roster;
        if (roster != null && !roster.isEmpty()) {
            this.assignedIndex = roster.size() - 1; // Default to most recently unlocked
        }

        viewGroup = new Group();
        viewGroup.getChildren().add(this.getImageView());

        badgeView = new ImageView();
        badgeView.setFitWidth(18);
        badgeView.setFitHeight(18);
        badgeView.setPreserveRatio(true);
        badgeView.setTranslateX(xstart + 10);
        badgeView.setTranslateY(ystart - 6);
        viewGroup.getChildren().add(badgeView);

        updateBadge();
    }

    public void updateBadge() {
        Pokemon p = getAssignedPokemon();
        if (p != null) {
            badgeView.setImage(new Image(TowerDefense.class.getResourceAsStream(p.getLoc())));
        }
    }

    public Pokemon getAssignedPokemon() {
        if (roster == null || roster.isEmpty()) {
            return null;
        }
        if (assignedIndex >= roster.size()) {
            assignedIndex = roster.size() - 1;
        }
        return roster.get(assignedIndex);
    }

    public void cyclePokemon() {
        if (roster == null || roster.size() <= 1) {
            return;
        }
        assignedIndex = (assignedIndex + 1) % roster.size();
        updateBadge();
    }

    public void setAssignedPokemon(int index) {
        if (roster != null && index >= 0 && index < roster.size()) {
            if (this.assignedIndex != index) {
                assignedIndex = index;
                updateBadge();
            }
        }
    }

    public Group getView() {
        return viewGroup;
    }

    public Attacker shoot(Trainer t) {
        Pokemon poke = getAssignedPokemon();
        if (hasShot || poke == null) {
            return null;
        }
        shotTimer = 0;
        Attacker atker = new Attacker(poke, this.getX(), this.getY(), t);
        hasShot = true;
        return atker;
    }

    public boolean hasShot() {
        return hasShot;
    }

    public void addToShotTimer() {
        shotTimer++;
    }

    public int shotTimer() {
        return shotTimer;
    }

    public void reload() {
        hasShot = false;
    }
}