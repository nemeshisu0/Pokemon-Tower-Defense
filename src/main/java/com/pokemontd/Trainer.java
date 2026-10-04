package com.pokemontd;

import java.util.ArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.Group;
import javafx.scene.paint.Color;

public class Trainer extends Moveable {
    private int health;
    private final int maxHealth;
    private String trainerName;
    private PokemonType type;
    private ArrayList<Moveable> pokeballs = new ArrayList<Moveable>();
    final private Group trainerwballs;
    final private int MAXBALLS = 6;

    // Status effects
    private int paralyzeTicks = 0;
    private int burnTicks = 0;
    private int burnTimer = 0;
    private Label statusLabel;
    private Label nameLabel;

    public Trainer(String location, int xloc, int yloc, int health, String trainerName, PokemonType type) {
        super(location, xloc, yloc, 1);
        this.health = health;
        this.maxHealth = Math.max(1, health);
        this.trainerName = trainerName;
        this.type = type;

        Coord one = new Coord(202, 586);
        Coord two = new Coord(105, 586);
        Coord three = new Coord(105, 507);
        Coord four = new Coord(202, 507);
        Coord five = new Coord(202, 400);
        Coord six = new Coord(154, 400);
        Coord seven = new Coord(154, 346);
        Coord eight = new Coord(283, 346);
        Coord nine = new Coord(283, 118);
        Coord ten = new Coord(170, 118);
        Coord eleven = new Coord(170, 95);
        Path path = new Path(one, two, three, four, five, six, seven, eight, nine, ten, eleven);
        super.setPath(path);

        trainerwballs = new Group();

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        statusLabel.setTranslateX(-2);
        statusLabel.setTranslateY(-20);

        nameLabel = new Label(trainerName + " [" + type.getDisplayName() + "]");
        nameLabel.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: " + type.getColorHex() + "; -fx-background-color: rgba(255,255,255,0.75); -fx-background-radius: 3; -fx-padding: 0 2;");
        nameLabel.setTranslateX(-15);
        nameLabel.setTranslateY(24);

        updateBalls();
    }

    public DamageResult takeDamage(int baseAtk, PokemonType atkType) {
        double mult = atkType != null ? atkType.getMultiplierAgainst(this.type) : 1.0;
        int finalDamage = Math.max(1, (int) Math.round(baseAtk * mult));
        health -= finalDamage;
        updateBalls();

        boolean appliedParalysis = false;
        boolean appliedBurn = false;

        if (atkType == PokemonType.ELECTRIC && Math.random() < 0.35) {
            paralyzeTicks = Math.max(paralyzeTicks, 140);
            appliedParalysis = true;
        } else if (atkType == PokemonType.FIRE && Math.random() < 0.40) {
            burnTicks = Math.max(burnTicks, 160);
            appliedBurn = true;
        }

        updateStatusLabel();
        return new DamageResult(finalDamage, mult, appliedParalysis, appliedBurn);
    }

    public void tickStatus() {
        if (paralyzeTicks > 0) {
            paralyzeTicks--;
        }
        if (burnTicks > 0) {
            burnTicks--;
            burnTimer++;
            if (burnTimer % 35 == 0) {
                health -= 2;
                updateBalls();
            }
        } else {
            burnTimer = 0;
        }
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        StringBuilder sb = new StringBuilder();
        if (paralyzeTicks > 0) sb.append("⚡PAR ");
        if (burnTicks > 0) sb.append("🔥BRN");
        statusLabel.setText(sb.toString());
        if (paralyzeTicks > 0 && burnTicks > 0) {
            statusLabel.setTextFill(Color.ORANGE);
        } else if (paralyzeTicks > 0) {
            statusLabel.setTextFill(Color.GOLD);
        } else if (burnTicks > 0) {
            statusLabel.setTextFill(Color.RED);
        }
    }

    @Override
    public void move() {
        // If paralyzed, skip every other movement tick (50% speed)
        if (paralyzeTicks > 0 && (paralyzeTicks % 2 == 0)) {
            return;
        }
        super.move();
    }

    public void updateBalls() {
        pokeballs.clear();
        int xpos;
        int ypos;
        trainerwballs.getChildren().clear();
        trainerwballs.getChildren().addAll(this.getImageView(), statusLabel, nameLabel);
        for (int i = 0; i < getNumBalls(); i++) {
            String fileLoc = "/Resource/pokeball.png";
            pokeballs.add(new Moveable(fileLoc, 0, 0, 1));
            xpos = (i < 3 ? 20 - i * 10 : (i - 3) * 10) + 1;
            ypos = (i < 3 ? 0 : -10);
            Moveable pokeball = pokeballs.get(i);
            ImageView pokeballView = pokeball.getImageView();
            pokeballView.setTranslateX(xpos);
            pokeballView.setTranslateY(ypos);
            trainerwballs.getChildren().add(pokeballView);
        }
    }

    public Image getImage() {
        return super.getImage();
    }

    public Image getPokeball(int i) {
        return pokeballs.get(i).getImage();
    }

    public int getNumBalls() {
        Double numBalls = Math.ceil(((double) health) / ((double) maxHealth / MAXBALLS));
        if (numBalls <= MAXBALLS) {
            return Math.max(1, numBalls.intValue());
        } else return MAXBALLS;
    }

    public Group getGroup() {
        return trainerwballs;
    }

    public int getHealth() {
        return health;
    }

    public PokemonType getType() {
        return type;
    }

    public String getTrainerName() {
        return trainerName;
    }

    public boolean isParalyzed() {
        return paralyzeTicks > 0;
    }

    public boolean isBurned() {
        return burnTicks > 0;
    }
}