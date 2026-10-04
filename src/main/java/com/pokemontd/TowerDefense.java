package com.pokemontd;

import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

public class TowerDefense extends Application {

    private Timeline timeline;
    static int gamespeed = 10;
    final static int WIDTH = 384;
    final static int HEIGHT = 738;

    private Integer waveTime = 0;
    private Integer level = 1;
    private boolean gameOn = false;
    private boolean isPaused = false;
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

    // Range Indicator
    private Circle rangeIndicator;
    private Turret selectedTurret = null;
    private HBox turretControlBar;
    private Label turretInfoLabel;

    // HUD Elements
    private Label creditLabel = new Label();
    private Label towerLife = new Label();
    private Label bank = new Label();
    private Label waveStatusLabel = new Label();
    private Button startWaveButton;
    private Button pauseBtn;
    private Button speedBtn;
    private Button muteBtn;
    private Group backdrop;
    private Rectangle pauseOverlay;
    private Label pauseText;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // GameWindow Backdrop
        backdrop = new Group(background.getImageView(), tower.getImageView());
        backdrop.setEffect(new BoxBlur());

        // Range Circle
        rangeIndicator = new Circle();
        rangeIndicator.setFill(Color.rgb(41, 128, 185, 0.22));
        rangeIndicator.setStroke(Color.rgb(41, 128, 185, 0.85));
        rangeIndicator.setStrokeWidth(1.5);
        rangeIndicator.setVisible(false);

        // Click to place turret
        turretMark.getImageView().setOnMouseClicked(e -> {
            if (turretMark.isHovering()) {
                Coord turretPos = turretMark.getGrassCoord();
                Turret turret = store.buyTurret(turretPos, 1);
                if (turret != null) {
                    SoundManager.playBuy();
                    turret.getView().setOnMouseClicked(evt -> {
                        selectTurret(turret);
                        evt.consume();
                    });
                    entities.add(turret);
                    selectTurret(turret);
                    updateLabels();
                }
            }
        });

        // Hover to preview turret placement and range
        background.getImageView().setOnMouseMoved(e -> {
            if (gameOn && !isPaused) {
                int x = (int) e.getX();
                int y = (int) e.getY();
                turretMark.hover(x, y);
                turretMark.update();

                if (turretMark.isHovering()) {
                    Coord grass = turretMark.getGrassCoord();
                    rangeIndicator.setCenterX(grass.getX() + 8);
                    rangeIndicator.setCenterY(grass.getY() + 8);
                    Pokemon previewPoke = store.getUnlockedPokemon().get(store.getUnlockedPokemon().size() - 1);
                    rangeIndicator.setRadius(previewPoke.getRange());
                    rangeIndicator.setVisible(true);
                } else if (selectedTurret == null) {
                    rangeIndicator.setVisible(false);
                }
            }
        });

        // Clicking outside deselects turret
        background.getImageView().setOnMouseClicked(e -> {
            deselectTurret();
        });

        // Top HUD Bar
        Rectangle hudBackground = new Rectangle(WIDTH, 44);
        hudBackground.setFill(Color.rgb(255, 255, 255, 0.94));
        hudBackground.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.25)));

        towerLife.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        creditLabel.setStyle("-fx-font-size: 10px;");
        waveStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2980b9; -fx-font-weight: bold;");

        pauseBtn = new Button("⏸");
        pauseBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");
        pauseBtn.setOnAction(e -> togglePause());

        speedBtn = new Button("1x");
        speedBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");
        speedBtn.setOnAction(e -> toggleSpeed());

        muteBtn = new Button("🔊");
        muteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");
        muteBtn.setOnAction(e -> {
            SoundManager.toggleMute();
            muteBtn.setText(SoundManager.isMuted() ? "🔇" : "🔊");
        });

        HBox topStatsBox = new HBox(6);
        topStatsBox.setPadding(new Insets(4, 6, 4, 6));
        topStatsBox.setAlignment(Pos.CENTER_LEFT);
        topStatsBox.getChildren().addAll(creditLabel, towerLife, pauseBtn, speedBtn, muteBtn);

        // Turret Management Bar (Bottom overlay when a turret is selected)
        turretControlBar = new HBox(8);
        turretControlBar.setAlignment(Pos.CENTER);
        turretControlBar.setPadding(new Insets(6));
        turretControlBar.setStyle("-fx-background-color: rgba(30, 30, 30, 0.90); -fx-background-radius: 8;");
        turretControlBar.setTranslateX(10);
        turretControlBar.setTranslateY(HEIGHT - 65);
        turretControlBar.setPrefWidth(WIDTH - 20);
        turretControlBar.setVisible(false);

        turretInfoLabel = new Label();
        turretInfoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");

        Button cyclePokemonBtn = new Button("Cambia ❯");
        cyclePokemonBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 3 6;");
        cyclePokemonBtn.setOnAction(e -> {
            if (selectedTurret != null) {
                selectedTurret.cyclePokemon();
                updateTurretSelectionView();
            }
        });

        Button closeTurretBarBtn = new Button("✕");
        closeTurretBarBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 3 6;");
        closeTurretBarBtn.setOnAction(e -> deselectTurret());

        turretControlBar.getChildren().addAll(turretInfoLabel, cyclePokemonBtn, closeTurretBarBtn);

        // Pause visual overlay
        pauseOverlay = new Rectangle(WIDTH, HEIGHT);
        pauseOverlay.setFill(Color.rgb(0, 0, 0, 0.45));
        pauseOverlay.setVisible(false);

        pauseText = new Label("IN PAUSA");
        pauseText.setTextFill(Color.WHITE);
        pauseText.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        pauseText.setTranslateX(WIDTH / 2.0 - 65);
        pauseText.setTranslateY(HEIGHT / 2.0 - 20);
        pauseText.setVisible(false);

        gameWindow.getChildren().addAll(
            backdrop,
            rangeIndicator,
            turretMark.getImageView(),
            entities.getAll(),
            hudBackground,
            topStatsBox,
            turretControlBar,
            pauseOverlay,
            pauseText
        );

        // Main Menu
        startWaveButton = new Button("Start Game");
        Button pokedexButton = new Button("Pokédex & Negozio");
        startWaveButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-color: #27ae60; -fx-text-fill: white;");
        pokedexButton.setStyle("-fx-font-size: 13px; -fx-padding: 6 16; -fx-background-color: #2980b9; -fx-text-fill: white;");

        Rectangle backblur = new Rectangle(WIDTH, HEIGHT);
        backblur.setOpacity(0.5);

        startWaveButton.setTranslateX(120);
        startWaveButton.setTranslateY(320);
        pokedexButton.setTranslateX(115);
        pokedexButton.setTranslateY(375);

        pokedexButton.setOnAction(e -> openPokedex());
        mainMenu.getChildren().addAll(backblur, startWaveButton, pokedexButton);

        // Build Pokedex / Store View
        buildPokedexMenu();

        // Build Game Over View
        buildGameOverMenu();

        // Wave Start handler
        startWaveButton.setOnAction(e -> startWave());

        updateLabels();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setTitle("Pokemon Tower Defense");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void selectTurret(Turret t) {
        selectedTurret = t;
        rangeIndicator.setCenterX(t.getX() + 8);
        rangeIndicator.setCenterY(t.getY() + 8);
        Pokemon p = t.getAssignedPokemon();
        if (p != null) {
            rangeIndicator.setRadius(p.getRange());
        }
        rangeIndicator.setVisible(true);
        updateTurretSelectionView();
        turretControlBar.setVisible(true);
    }

    private void updateTurretSelectionView() {
        if (selectedTurret != null) {
            Pokemon p = selectedTurret.getAssignedPokemon();
            if (p != null) {
                rangeIndicator.setRadius(p.getRange());
                turretInfoLabel.setText(p.getName() + " [" + p.getType().getDisplayName() + "] Atk:" + p.getAttack() + " Rng:" + p.getRange());
            }
        }
    }

    private void deselectTurret() {
        selectedTurret = null;
        turretControlBar.setVisible(false);
        if (!turretMark.isHovering()) {
            rangeIndicator.setVisible(false);
        }
    }

    private void togglePause() {
        if (!gameOn) return;
        isPaused = !isPaused;
        if (isPaused) {
            if (timeline != null) timeline.pause();
            pauseBtn.setText("▶");
            pauseOverlay.setVisible(true);
            pauseText.setVisible(true);
        } else {
            if (timeline != null) timeline.play();
            pauseBtn.setText("⏸");
            pauseOverlay.setVisible(false);
            pauseText.setVisible(false);
        }
    }

    private void toggleSpeed() {
        if (gamespeed == 10) {
            gamespeed = 5;
            speedBtn.setText("2x");
        } else {
            gamespeed = 10;
            speedBtn.setText("1x");
        }
        if (gameOn && !isPaused && timeline != null) {
            timeline.stop();
            initTimeline();
            timeline.play();
        }
    }

    private void buildPokedexMenu() {
        Rectangle whiteback = new Rectangle(WIDTH, HEIGHT);
        whiteback.setFill(Color.rgb(245, 247, 250));

        BorderPane border = new BorderPane();
        border.setPrefSize(WIDTH, HEIGHT);
        border.setPadding(new Insets(12));

        // Top Header
        HBox topPane = new HBox(20);
        topPane.setAlignment(Pos.CENTER_LEFT);
        Button returnButton = new Button("❮ Torna al Gioco");
        returnButton.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white;");
        returnButton.setOnAction(e -> {
            root.getChildren().remove(pokedexMenu);
            updateLabels();
        });
        bank.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        topPane.getChildren().addAll(returnButton, bank);
        border.setTop(topPane);

        // Center: List + Details
        HBox centerBox = new HBox(10);
        centerBox.setPadding(new Insets(10, 0, 10, 0));

        ListView<Pokemon> listDex = new ListView<Pokemon>();
        listDex.setPrefWidth(135);

        // Details Card
        VBox detailsCard = new VBox(8);
        detailsCard.setPrefWidth(WIDTH - 165);
        detailsCard.setPadding(new Insets(10));
        detailsCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 5, 0, 0, 1);");
        detailsCard.setAlignment(Pos.TOP_CENTER);

        ImageView pokeImage = new ImageView();
        pokeImage.setFitWidth(50);
        pokeImage.setFitHeight(50);
        pokeImage.setPreserveRatio(true);

        Label pokeName = new Label();
        pokeName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label typePill = new Label();
        typePill.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold;");

        Label pokeDesc = new Label();
        pokeDesc.setWrapText(true);
        pokeDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

        Label atkLabel = new Label();
        Label rngLabel = new Label();
        Label spdLabel = new Label();
        Label advantageLabel = new Label();
        atkLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        rngLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        spdLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        advantageLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Button buyButton = new Button("Acquista");
        buyButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 14;");

        detailsCard.getChildren().addAll(pokeImage, pokeName, typePill, pokeDesc, atkLabel, rngLabel, spdLabel, advantageLabel, buyButton);

        // Selection Listener
        listDex.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                pokeImage.setImage(new Image(TowerDefense.class.getResourceAsStream(selected.getLoc())));
                pokeName.setText(selected.getName());
                typePill.setText(selected.getType().getDisplayName());
                typePill.setStyle("-fx-background-color: " + selected.getType().getColorHex() + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-weight: bold;");
                pokeDesc.setText(selected.getDescription());
                atkLabel.setText("⚔️ Attacco: " + selected.getAttack());
                rngLabel.setText("🎯 Gittata: " + selected.getRange() + " px");
                spdLabel.setText("⚡ Frequenza: " + (selected.getCooldown() * 10) + " ms");

                switch (selected.getType()) {
                    case ELECTRIC:
                        advantageLabel.setText("⚡ Vantaggio: 2x vs Acqua | Paralisi!");
                        break;
                    case WATER:
                        advantageLabel.setText("💧 Vantaggio: 2x vs Fuoco");
                        break;
                    case FIRE:
                        advantageLabel.setText("🔥 Vantaggio: 2x vs Erba | Bruciatura!");
                        break;
                    default:
                        advantageLabel.setText("⚪ Tipo Normale: Danno bilanciato");
                        break;
                }

                boolean isUnlocked = store.getUnlockedPokemon().stream().anyMatch(p -> p.getName().equals(selected.getName()));
                if (isUnlocked) {
                    buyButton.setText("Sbloccato ✓");
                    buyButton.setDisable(true);
                    buyButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                } else {
                    buyButton.setText("Acquista ($" + selected.getPrice() + ")");
                    buyButton.setDisable(store.getMoney() < selected.getPrice());
                    buyButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            }
        });

        buyButton.setOnAction(e -> {
            int sel = listDex.getSelectionModel().getSelectedIndex();
            if (store.buyPika(sel)) {
                SoundManager.playBuy();
                refreshDex(listDex);
                updateLabels();
            }
        });

        centerBox.getChildren().addAll(listDex, detailsCard);
        border.setCenter(centerBox);

        pokedexMenu.getChildren().addAll(whiteback, border);
    }

    private void openPokedex() {
        BorderPane bp = (BorderPane) pokedexMenu.getChildren().get(1);
        HBox hbox = (HBox) bp.getCenter();
        @SuppressWarnings("unchecked")
        ListView<Pokemon> listDex = (ListView<Pokemon>) hbox.getChildren().get(0);
        refreshDex(listDex);
        bank.setText("Fondi: $" + store.getMoney());

        if (!root.getChildren().contains(pokedexMenu)) {
            root.getChildren().add(pokedexMenu);
        }
    }

    private void refreshDex(ListView<Pokemon> listDex) {
        ObservableList<Pokemon> all = FXCollections.observableArrayList();
        all.addAll(store.getUnlockedPokemon());
        all.addAll(store.getInventory());
        listDex.setItems(all);
        if (!all.isEmpty()) {
            listDex.getSelectionModel().select(0);
        }
    }

    private void buildGameOverMenu() {
        Rectangle gameOverBackdrop = new Rectangle(WIDTH, HEIGHT);
        gameOverBackdrop.setFill(Color.rgb(0, 0, 0, 0.75));
        VBox gameOverBox = new VBox(15);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setTranslateX(WIDTH / 4.0 - 20);
        gameOverBox.setTranslateY(HEIGHT / 3.0);
        gameOverBox.setPrefWidth(WIDTH / 2.0 + 40);

        Label gameOverTitle = new Label("GAME OVER");
        gameOverTitle.setTextFill(Color.RED);
        gameOverTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label waveReachedLabel = new Label();
        waveReachedLabel.setTextFill(Color.WHITE);
        waveReachedLabel.setStyle("-fx-font-size: 15px;");

        Button restartButton = new Button("Rigioca ↻");
        restartButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 18; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        restartButton.setOnAction(e -> {
            root.getChildren().remove(gameOverMenu);
            restartGame();
        });

        gameOverBox.getChildren().addAll(gameOverTitle, waveReachedLabel, restartButton);
        gameOverMenu.getChildren().addAll(gameOverBackdrop, gameOverBox);
    }

    private void startWave() {
        gameOn = true;
        isPaused = false;
        pauseBtn.setText("⏸");
        pauseOverlay.setVisible(false);
        pauseText.setVisible(false);
        deselectTurret();

        root.getChildren().remove(mainMenu);
        backdrop.setEffect(null);

        if (timeline != null) {
            timeline.stop();
        }
        waveTime = 0;
        SoundManager.playWaveStart();
        initTimeline();
        timeline.playFromStart();
    }

    private void initTimeline() {
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(gamespeed),
                new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                        wave();

                        // Wave completed condition
                        if (spawned == level && entities.getTrainers().isEmpty()) {
                            level++;
                            spawned = 0;
                            startWaveButton.setText("Inizia Ondata " + level + " ❯");
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
                            SoundManager.playGameOver();
                            towerLife.setText("Palestra Caduta!");
                            towerLife.setTextFill(Color.RED);
                            backdrop.setEffect(new BoxBlur());
                            if (timeline != null) {
                                timeline.stop();
                            }
                            VBox box = (VBox) gameOverMenu.getChildren().get(1);
                            Label waveLbl = (Label) box.getChildren().get(1);
                            waveLbl.setText("Ondata raggiunta: " + level);
                            if (!root.getChildren().contains(gameOverMenu)) {
                                root.getChildren().add(gameOverMenu);
                            }
                        }
                    }
                }));
    }

    public void updateLabels() {
        creditLabel.setText("Crediti: $" + store.getMoney() + " | Cespuglio: $" + store.getTurretPrice());
        bank.setText("Fondi: $" + store.getMoney());
        towerLife.setText("❤️ Palestra: " + entities.getTowerHealth());
        int alive = entities.getTrainers().size();
        waveStatusLabel.setText("Ondata " + level + " (" + alive + "/" + level + ")");
    }

    public void restartGame() {
        if (timeline != null) {
            timeline.stop();
        }
        gameOn = false;
        isPaused = false;
        pauseBtn.setText("⏸");
        pauseOverlay.setVisible(false);
        pauseText.setVisible(false);
        deselectTurret();

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

        // Move attackers
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