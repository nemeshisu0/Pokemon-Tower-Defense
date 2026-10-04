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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
    private Stage primaryStage;

    final static Stationary background = new Stationary("/Resource/background.png", 0, 0);
    final static Stationary tower = new Stationary("/Resource/tower.png", 130, 35);
    final static TurretMarker turretMark = new TurretMarker("/Resource/orange_bush.png", -100, 0, 2);

    final private Store store = new Store();
    final private Entities entities = new Entities(store);

    // Core Game Layers
    final private Group gameWindow = new Group();
    final private Group mainMenu = new Group();
    final private Group pokedexMenu = new Group();
    final private Group settingsMenu = new Group();
    final private Group gameOverMenu = new Group();

    // Game Content & Responsive Wrapper
    final private Group gameContent = new Group();
    private Pane centerGamePane;
    private double currentScale = 1.0;

    // Range Indicator
    private Circle rangeIndicator;
    private Turret selectedTurret = null;
    private HBox turretControlBar;
    private Label turretInfoLabel;

    // HUD Elements
    private Label creditLabel = new Label();
    private Label turretCostLabel = new Label();
    private Label towerLife = new Label();
    private Label bank = new Label();
    private Label waveStatusLabel = new Label();
    private Button startWaveButton;
    private Button pauseBtn;
    private Button speedBtn;
    private Button muteBtn;
    private Button settingsBtn;
    private Group backdrop;
    private Rectangle pauseOverlay;
    private Label pauseText;

    // Sidebars Live Info
    private Label leftPanelStatus;
    private Label rightPanelGymHp;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

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

        // Clicking background deselects turret
        background.getImageView().setOnMouseClicked(e -> {
            deselectTurret();
        });

        // Top HUD Bar - 2-tier spacious layout with zero overlap
        VBox topHudBox = new VBox(3);
        topHudBox.setPrefWidth(WIDTH);
        topHudBox.setPadding(new Insets(4, 8, 4, 8));
        topHudBox.setStyle("-fx-background-color: rgba(22, 27, 34, 0.94); -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        topHudBox.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.4)));

        // Row 1: Gym Health & Credits
        HBox hudRow1 = new HBox(12);
        hudRow1.setAlignment(Pos.CENTER_LEFT);
        towerLife.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        creditLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #f1c40f; -fx-font-weight: bold;");
        turretCostLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2ecc71;");
        hudRow1.getChildren().addAll(towerLife, creditLabel, turretCostLabel);

        // Row 2: Wave status & Quick Action Buttons
        HBox hudRow2 = new HBox(8);
        hudRow2.setAlignment(Pos.CENTER_LEFT);
        waveStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #3498db; -fx-font-weight: bold;");

        pauseBtn = new Button("⏸");
        pauseBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #34495e; -fx-text-fill: white;");
        pauseBtn.setOnAction(e -> togglePause());

        speedBtn = new Button("1x");
        speedBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #34495e; -fx-text-fill: white;");
        speedBtn.setOnAction(e -> toggleSpeed());

        muteBtn = new Button("🔊");
        muteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #34495e; -fx-text-fill: white;");
        muteBtn.setOnAction(e -> {
            SoundManager.toggleMute();
            muteBtn.setText(SoundManager.isMuted() ? "🔇" : "🔊");
        });

        settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #34495e; -fx-text-fill: white;");
        settingsBtn.setOnAction(e -> openSettings());

        HBox btnGroup = new HBox(4, pauseBtn, speedBtn, muteBtn, settingsBtn);
        btnGroup.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(btnGroup, Priority.ALWAYS);

        hudRow2.getChildren().addAll(waveStatusLabel, btnGroup);
        topHudBox.getChildren().addAll(hudRow1, hudRow2);

        // Turret Management Bar (Bottom overlay when a turret is selected)
        turretControlBar = new HBox(8);
        turretControlBar.setAlignment(Pos.CENTER);
        turretControlBar.setPadding(new Insets(6));
        turretControlBar.setStyle("-fx-background-color: rgba(22, 27, 34, 0.94); -fx-background-radius: 8; -fx-border-color: #30363d; -fx-border-radius: 8;");
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
        pauseOverlay.setFill(Color.rgb(0, 0, 0, 0.55));
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
            topHudBox,
            turretControlBar,
            pauseOverlay,
            pauseText
        );

        // Main Menu
        Rectangle backblur = new Rectangle(WIDTH, HEIGHT);
        backblur.setFill(Color.rgb(15, 20, 25, 0.65));

        VBox mainMenuCard = new VBox(15);
        mainMenuCard.setAlignment(Pos.CENTER);
        mainMenuCard.setTranslateX(20);
        mainMenuCard.setTranslateY(260);
        mainMenuCard.setPrefWidth(WIDTH - 40);

        Label titleLabel = new Label("POKÉMON\nTOWER DEFENSE");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-alignment: center;");

        startWaveButton = new Button("▶ Inizia Ondata");
        startWaveButton.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 22; -fx-background-color: #27ae60; -fx-text-fill: white;");

        Button pokedexButton = new Button("📖 Pokédex & Negozio");
        pokedexButton.setStyle("-fx-font-size: 12px; -fx-padding: 6 18; -fx-background-color: #2980b9; -fx-text-fill: white;");
        pokedexButton.setOnAction(e -> openPokedex());

        Button menuSettingsBtn = new Button("⚙ Impostazioni");
        menuSettingsBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 18; -fx-background-color: #4b6584; -fx-text-fill: white;");
        menuSettingsBtn.setOnAction(e -> openSettings());

        mainMenuCard.getChildren().addAll(titleLabel, startWaveButton, pokedexButton, menuSettingsBtn);
        mainMenu.getChildren().addAll(backblur, mainMenuCard);

        // Build Menus
        buildPokedexMenu();
        buildSettingsMenu();
        buildGameOverMenu();

        // Wave Start handler
        startWaveButton.setOnAction(e -> startWave());

        // Setup Main Scalable Container
        gameContent.getChildren().addAll(gameWindow, mainMenu, pokedexMenu, settingsMenu, gameOverMenu);
        gameContent.setManaged(false); // Unmanaged so its scaled size does not cause parent resize loops

        centerGamePane = new Pane(gameContent) {
            @Override
            protected void layoutChildren() {
                double w = getWidth();
                double h = getHeight();
                if (w > 0 && h > 0) {
                    double scale = Math.min(w / WIDTH, h / HEIGHT);
                    scale = Math.max(0.2, scale);
                    if (Math.abs(scale - currentScale) > 0.001) {
                        currentScale = scale;
                        gameContent.setScaleX(scale);
                        gameContent.setScaleY(scale);
                    }
                    // Perfect centering
                    gameContent.setLayoutX((w - WIDTH) / 2.0);
                    gameContent.setLayoutY((h - HEIGHT) / 2.0);
                }
            }
        };
        centerGamePane.setStyle("-fx-background-color: #0b0e14;");
        centerGamePane.setMinSize(0, 0);

        // Build Lateral Sidebars for PC Widescreen
        VBox leftSidebar = buildLeftSidebar();
        VBox rightSidebar = buildRightSidebar();

        BorderPane rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: #0d1117;");
        rootPane.setCenter(centerGamePane);
        rootPane.setLeft(leftSidebar);
        rootPane.setRight(rightSidebar);

        // Responsive Sidebar visibility: show sidebars only on wide screens
        leftSidebar.visibleProperty().bind(rootPane.widthProperty().greaterThan(880));
        leftSidebar.managedProperty().bind(rootPane.widthProperty().greaterThan(880));
        rightSidebar.visibleProperty().bind(rootPane.widthProperty().greaterThan(880));
        rightSidebar.managedProperty().bind(rootPane.widthProperty().greaterThan(880));

        updateLabels();

        // 1280x760 Default PC Window (Spacious HD)
        Scene scene = new Scene(rootPane, 1280, 760);
        primaryStage.setTitle("Pokemon Tower Defense");

        // F11 Fullscreen shortcut
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });

        try {
            primaryStage.getIcons().add(new Image(TowerDefense.class.getResourceAsStream("/Resource/app_icon.png")));
        } catch (Exception ignored) {}

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox buildLeftSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        sidebar.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("POKÉMON TD");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        // Type Effectiveness Card
        VBox typesCard = new VBox(6);
        typesCard.setPadding(new Insets(10));
        typesCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");
        Label typeTitle = new Label("EFFETTIVITÀ TIPI (2x):");
        typeTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Label el1 = new Label("⚡ Elettro ➔ 💧 Acqua");
        el1.setStyle("-fx-font-size: 11px; -fx-text-fill: #f1c40f;");
        Label el2 = new Label("💧 Acqua ➔ 🔥 Fuoco");
        el2.setStyle("-fx-font-size: 11px; -fx-text-fill: #3498db;");
        Label el3 = new Label("🔥 Fuoco ➔ 🌿 Erba");
        el3.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        Label el4 = new Label("⚪ Normale: Danno base");
        el4.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        typesCard.getChildren().addAll(typeTitle, el1, el2, el3, el4);

        // Status Effects Card
        VBox statusCard = new VBox(6);
        statusCard.setPadding(new Insets(10));
        statusCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");
        Label statusTitle = new Label("EFFETTI DI STATO:");
        statusTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Label st1 = new Label("⚡ Paralisi: -50% velocità");
        st1.setStyle("-fx-font-size: 10px; -fx-text-fill: #f39c12;");
        Label st2 = new Label("🔥 Scottatura: Danno continuo");
        st2.setStyle("-fx-font-size: 10px; -fx-text-fill: #e67e22;");

        statusCard.getChildren().addAll(statusTitle, st1, st2);

        // Sidebar Footer placeholder
        leftPanelStatus = new Label("Pronto all'azione");
        leftPanelStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #7d8590;");

        sidebar.getChildren().addAll(header, typesCard, statusCard, leftPanelStatus);
        return sidebar;
    }

    private VBox buildRightSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(250);
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");
        sidebar.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("COMANDI & STATI");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        // Controls Card
        VBox controlsCard = new VBox(6);
        controlsCard.setPadding(new Insets(10));
        controlsCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");

        Label ctrlTitle = new Label("SCORCIATOIE:");
        ctrlTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Label c1 = new Label("⌨ F11: Schermo Intero");
        c1.setStyle("-fx-font-size: 10px; -fx-text-fill: #c9d1d9;");
        Label c2 = new Label("🖱 Click Erba: Piazza Torretta");
        c2.setStyle("-fx-font-size: 10px; -fx-text-fill: #c9d1d9;");
        Label c3 = new Label("🖱 Click Cespuglio: Info / Cambia");
        c3.setStyle("-fx-font-size: 10px; -fx-text-fill: #c9d1d9;");

        controlsCard.getChildren().addAll(ctrlTitle, c1, c2, c3);

        // Gym Leader Live Status Card
        VBox gymCard = new VBox(6);
        gymCard.setPadding(new Insets(10));
        gymCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");

        Label gymTitle = new Label("DIFESA PALESTRA:");
        gymTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        rightPanelGymHp = new Label("❤️ Salute: 1000 / 1000");
        rightPanelGymHp.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        gymCard.getChildren().addAll(gymTitle, rightPanelGymHp);

        sidebar.getChildren().addAll(header, controlsCard, gymCard);
        return sidebar;
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

    private void buildSettingsMenu() {
        Rectangle whiteback = new Rectangle(WIDTH, HEIGHT);
        whiteback.setFill(Color.rgb(15, 20, 25, 0.92));

        VBox settingsCard = new VBox(12);
        settingsCard.setPadding(new Insets(20));
        settingsCard.setAlignment(Pos.CENTER);
        settingsCard.setPrefWidth(WIDTH);

        Label title = new Label("⚙ IMPOSTAZIONI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        // Resolution Section
        Label resHeader = new Label("RISOLUZIONE E SCHERMO:");
        resHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Button btnFs = new Button("⛶ Schermo Intero (F11)");
        btnFs.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #238636; -fx-text-fill: white;");
        btnFs.setOnAction(e -> {
            primaryStage.setFullScreen(!primaryStage.isFullScreen());
        });

        Button btnFhd = new Button("🗖 Finestra Full HD (1920 x 1080)");
        btnFhd.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");
        btnFhd.setOnAction(e -> {
            primaryStage.setFullScreen(false);
            primaryStage.setWidth(1920);
            primaryStage.setHeight(1080);
            primaryStage.centerOnScreen();
        });

        Button btnHd = new Button("🗖 Finestra HD (1280 x 720)");
        btnHd.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");
        btnHd.setOnAction(e -> {
            primaryStage.setFullScreen(false);
            primaryStage.setWidth(1280);
            primaryStage.setHeight(720);
            primaryStage.centerOnScreen();
        });

        Button btnCompact = new Button("📱 Finestra Compatta (400 x 780)");
        btnCompact.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");
        btnCompact.setOnAction(e -> {
            primaryStage.setFullScreen(false);
            primaryStage.setWidth(400);
            primaryStage.setHeight(780);
            primaryStage.centerOnScreen();
        });

        // Audio & Speed Toggles
        Label gameplayHeader = new Label("PREFERENZE:");
        gameplayHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Button audioToggleBtn = new Button("Effetti Audio: " + (SoundManager.isMuted() ? "Disattivi 🔇" : "Attivi 🔊"));
        audioToggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");
        audioToggleBtn.setOnAction(e -> {
            SoundManager.toggleMute();
            muteBtn.setText(SoundManager.isMuted() ? "🔇" : "🔊");
            audioToggleBtn.setText("Effetti Audio: " + (SoundManager.isMuted() ? "Disattivi 🔇" : "Attivi 🔊"));
        });

        Button closeBtn = new Button("❮ Torna Indietro");
        closeBtn.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-color: #34495e; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> closeSettings());

        settingsCard.getChildren().addAll(
            title,
            resHeader,
            btnFs,
            btnFhd,
            btnHd,
            btnCompact,
            gameplayHeader,
            audioToggleBtn,
            closeBtn
        );

        settingsMenu.getChildren().addAll(whiteback, settingsCard);
        settingsMenu.setVisible(false);
    }

    private void openSettings() {
        settingsMenu.setVisible(true);
        if (gameOn && !isPaused) {
            togglePause();
        }
    }

    private void closeSettings() {
        settingsMenu.setVisible(false);
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
            pokedexMenu.setVisible(false);
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
        pokedexMenu.setVisible(false);
    }

    private void openPokedex() {
        BorderPane bp = (BorderPane) pokedexMenu.getChildren().get(1);
        HBox hbox = (HBox) bp.getCenter();
        @SuppressWarnings("unchecked")
        ListView<Pokemon> listDex = (ListView<Pokemon>) hbox.getChildren().get(0);
        refreshDex(listDex);
        bank.setText("Fondi: $" + store.getMoney());
        pokedexMenu.setVisible(true);
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
            gameOverMenu.setVisible(false);
            restartGame();
        });

        gameOverBox.getChildren().addAll(gameOverTitle, waveReachedLabel, restartButton);
        gameOverMenu.getChildren().addAll(gameOverBackdrop, gameOverBox);
        gameOverMenu.setVisible(false);
    }

    private void startWave() {
        gameOn = true;
        isPaused = false;
        pauseBtn.setText("⏸");
        pauseOverlay.setVisible(false);
        pauseText.setVisible(false);
        deselectTurret();

        mainMenu.setVisible(false);
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
                            mainMenu.setVisible(true);
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
                            gameOverMenu.setVisible(true);
                        }
                    }
                }));
    }

    public void updateLabels() {
        creditLabel.setText("💰 $" + store.getMoney());
        turretCostLabel.setText("🌿 Cespuglio: $" + store.getTurretPrice());
        bank.setText("Fondi: $" + store.getMoney());
        towerLife.setText("❤️ " + entities.getTowerHealth());
        int alive = entities.getTrainers().size();
        waveStatusLabel.setText("Ondata " + level + " (" + alive + "/" + level + ")");

        if (rightPanelGymHp != null) {
            rightPanelGymHp.setText("❤️ Salute: " + entities.getTowerHealth() + " / 1000");
        }
        if (leftPanelStatus != null) {
            leftPanelStatus.setText(gameOn ? "Battaglia in corso..." : "In attesa dell'ondata");
        }
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
        towerLife.setTextFill(Color.rgb(231, 76, 60));
        startWaveButton.setText("▶ Inizia Ondata 1");
        updateLabels();

        mainMenu.setVisible(true);
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