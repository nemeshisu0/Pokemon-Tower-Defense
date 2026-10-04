package com.pokemontd;

import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import java.util.ArrayList;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.Group;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;

public class TowerDefense extends Application {

    private Timeline timeline;
    static int gamespeed = 10;
    final static int WIDTH = 384;
    final static int HEIGHT = 738;

    private Integer waveTime = 0;
    private Integer level = 1;
    private boolean gameOn = false;
    private int spawned = 0;

    final static Stationary background = new Stationary("/Resource/background.png", 0, 0);
    final static Stationary tower = new Stationary("/Resource/tower.png", 130, 35);
    final static TurretMarker turretMark = new TurretMarker("/Resource/orange_bush.png", -100, 0, 2);

    final private Store store = new Store();
    final private Entities entities = new Entities(store);

    final private Group gameWindow = new Group();
    final private Group mainMenu = new Group();
    final private Group pokedexMenu = new Group();
    final private Group gameOverMenu = new Group();
    final private Group root = new Group(gameWindow, mainMenu);

    private Label creditLabel = new Label("Welcome!");
    private Label towerLife;
    private Label endText = new Label("");
    private Label bank = new Label();
    private Button startWaveButton;
    private Group backdrop;

    @Override
    public void start(Stage primaryStage) throws Exception {
        towerLife = new Label("Gym Leader health: " + entities.getTowerHealth().toString());

        // GameWindow Backdrop
        backdrop = new Group(background.getImageView(), tower.getImageView());
        backdrop.setEffect(new BoxBlur());

        turretMark.getImageView().setOnMouseClicked(e -> {
            if (turretMark.isHovering()) {
                Coord turretPos = turretMark.getGrassCoord();
                Turret turret = store.buyTurret(turretPos, 1);
                if (turret != null) {
                    entities.add(turret);
                    updateLabels();
                }
            }
        });

        background.getImageView().setOnMouseMoved(e -> {
            if (gameOn) {
                int x = (int) e.getX();
                int y = (int) e.getY();
                turretMark.hover(x, y);
                turretMark.update();
            }
        });

        Rectangle rect = new Rectangle(380, 30);
        rect.setFill(Color.WHITE);
        HBox stats = new HBox(30);
        stats.getChildren().addAll(creditLabel, towerLife);
        gameWindow.getChildren().addAll(backdrop, turretMark.getImageView(), entities.getAll(), rect, stats);

        // MainMenu
        startWaveButton = new Button("Start Game");
        Button pokedexButton = new Button("Pokedex");
        Button fasterButton = new Button("Speed up");
        Button slowerButton = new Button("Slow down");
        Rectangle backblur = new Rectangle(WIDTH, HEIGHT);
        backblur.setOpacity(0.5);

        startWaveButton.setTranslateX(140);
        startWaveButton.setTranslateY(300);
        pokedexButton.setTranslateX(140);
        pokedexButton.setTranslateY(350);

        fasterButton.setOnAction(e -> {
            gamespeed = 5;
            mainMenu.getChildren().remove(fasterButton);
            if (!mainMenu.getChildren().contains(slowerButton)) {
                mainMenu.getChildren().add(slowerButton);
            }
        });
        slowerButton.setOnAction(e -> {
            gamespeed = 10;
            mainMenu.getChildren().remove(slowerButton);
            if (!mainMenu.getChildren().contains(fasterButton)) {
                mainMenu.getChildren().add(fasterButton);
            }
        });
        fasterButton.setTranslateX(290);
        slowerButton.setTranslateX(290);
        fasterButton.setTranslateY(20);
        slowerButton.setTranslateY(20);

        mainMenu.getChildren().addAll(backblur, startWaveButton, pokedexButton, fasterButton, endText);

        // Pokedex Menu
        Button returnButton = new Button("Return to game");
        Button buyButton = new Button("Buy");
        Rectangle whiteback = new Rectangle(WIDTH, 500);
        whiteback.setFill(Color.WHITE);
        BorderPane border = new BorderPane();
        VBox leftPane = new VBox(10);
        HBox topPane = new HBox(50);
        ListView<Pokemon> listDex = new ListView<Pokemon>();

        pokedexButton.setOnAction(e -> {
            ObservableList<Pokemon> currentDex = FXCollections.observableArrayList(store.getInventory());
            listDex.setItems(currentDex);
            if (!currentDex.isEmpty()) {
                listDex.getSelectionModel().select(0);
            }
            bank.setText("$" + store.getMoney());
            if (!root.getChildren().contains(pokedexMenu)) {
                root.getChildren().add(pokedexMenu);
            }
        });

        returnButton.setOnAction(e -> {
            root.getChildren().remove(pokedexMenu);
            updateLabels();
        });

        buyButton.setOnAction(e -> {
            int indPika = listDex.getSelectionModel().getSelectedIndex();
            store.buyPika(indPika);
            ObservableList<Pokemon> updatedDex = FXCollections.observableArrayList(store.getInventory());
            listDex.setItems(updatedDex);
            if (!updatedDex.isEmpty()) {
                listDex.getSelectionModel().select(0);
            }
            updateLabels();
        });

        listDex.setMaxWidth(160);
        leftPane.getChildren().add(listDex);
        border.setLeft(leftPane);
        topPane.getChildren().addAll(returnButton, bank, buyButton);
        border.setTop(topPane);
        pokedexMenu.getChildren().addAll(whiteback, border);

        // Game Over Menu
        Rectangle gameOverBackdrop = new Rectangle(WIDTH, HEIGHT);
        gameOverBackdrop.setFill(Color.rgb(0, 0, 0, 0.75));
        VBox gameOverBox = new VBox(20);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setTranslateX(WIDTH / 4.0);
        gameOverBox.setTranslateY(HEIGHT / 3.0);

        Label gameOverTitle = new Label("GAME OVER");
        gameOverTitle.setTextFill(Color.RED);
        gameOverTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label waveReachedLabel = new Label();
        waveReachedLabel.setTextFill(Color.WHITE);
        waveReachedLabel.setStyle("-fx-font-size: 16px;");

        Button restartButton = new Button("Play Again");
        restartButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 16;");
        restartButton.setOnAction(e -> {
            root.getChildren().remove(gameOverMenu);
            restartGame();
        });

        gameOverBox.getChildren().addAll(gameOverTitle, waveReachedLabel, restartButton);
        gameOverMenu.getChildren().addAll(gameOverBackdrop, gameOverBox);

        // Wave Start
        startWaveButton.setOnAction(e -> {
            gameOn = true;
            root.getChildren().remove(mainMenu);
            backdrop.setEffect(null);

            if (timeline != null) {
                timeline.stop();
            }
            waveTime = 0;
            timeline = new Timeline();
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(gamespeed),
                    new EventHandler<ActionEvent>() {
                        public void handle(ActionEvent event) {
                            wave();

                            // End of wave condition
                            if (spawned == level && entities.getTrainers().isEmpty()) {
                                System.out.println("END OF WAVE " + level);
                                level++;
                                spawned = 0;
                                startWaveButton.setText("Begin wave " + level + "!");
                                backdrop.setEffect(new BoxBlur());
                                if (!root.getChildren().contains(mainMenu)) {
                                    root.getChildren().add(mainMenu);
                                }
                                if (timeline != null) {
                                    timeline.stop();
                                }
                            }

                            // Defeat condition
                            if (entities.getTowerHealth() < 1) {
                                towerLife.setText("GAME OVER");
                                towerLife.setTextFill(Color.RED);
                                backdrop.setEffect(new BoxBlur());
                                if (timeline != null) {
                                    timeline.stop();
                                }
                                waveReachedLabel.setText("Wave reached: " + level);
                                if (!root.getChildren().contains(gameOverMenu)) {
                                    root.getChildren().add(gameOverMenu);
                                }
                            }
                        }
                    }));
            timeline.playFromStart();
        });

        updateLabels();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setTitle("Pokemon Tower Defense");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void updateLabels() {
        creditLabel.setText("Credit: $" + store.getMoney() + "\nSpawn point cost: $" + store.getTurretPrice());
        bank.setText("$" + store.getMoney());
        towerLife.setText("Gym Leader health: " + entities.getTowerHealth());
    }

    public void restartGame() {
        if (timeline != null) {
            timeline.stop();
        }
        gameOn = false;
        level = 1;
        spawned = 0;
        waveTime = 0;
        entities.reset();
        store.reset();
        towerLife.setTextFill(Color.BLACK);
        startWaveButton.setText("Start Game");
        updateLabels();

        if (!root.getChildren().contains(mainMenu)) {
            root.getChildren().add(mainMenu);
        }
        backdrop.setEffect(new BoxBlur());
    }

    public void wave() {
        waveTime++;
        updateLabels();

        // Spawn new trainer
        if (waveTime % (100 + 500 / level) == 0 && level > entities.getTrainers().size() && spawned < level) {
            spawned++;
            entities.createNewTrainer(level);
        }

        // Move trainers
        entities.moveTrainers();

        // Attack trainers
        entities.attackTrainers();

        // Move attackers (Single call, duplicate removed)
        entities.moveAttackers();

        // Passive income over time (+1 every 20 ticks)
        if (waveTime % 20 == 0) {
            store.spend(-1);
        }

        entities.updateAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}