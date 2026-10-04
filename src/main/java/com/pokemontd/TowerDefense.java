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
import javafx.scene.control.Slider;
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
import javafx.scene.transform.Scale;

public class TowerDefense extends Application {

    private Timeline timeline;
    static double gamespeed = 10.0;
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
    final private Group victoryMenu = new Group();
    private Label victoryStatsLabel;

    // Game Content & Responsive Wrapper
    final private Group gameContent = new Group();
    private final Scale gameScale = new Scale(1.0, 1.0, 0, 0);
    private Pane centerGamePane;
    private double currentScale = 1.0;
    private boolean pausedBySettings = false;

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

    // Autonomous AI Bot & Telemetry
    private final GameTelemetry telemetry = new GameTelemetry();
    private GameBot gameBot;
    private Button botHudBtn;
    private Button rightPanelBotToggleBtn;
    private Button rightPanelSpeedBtn;
    private Label botStatusLabel;
    private Label botDpsLabel;
    private Label botWavesLabel;
    private Label botDamageLabel;
    private Label botBalanceLabel;
    private Label botExportLabel;
    private VBox botLogBox;

    // Sidebars Live Info
    private Label leftPanelStatus;
    private Label rightPanelGymHp;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        this.entities.setTelemetry(telemetry);
        this.gameBot = new GameBot(this, store, entities, telemetry);

        // GameWindow Backdrop
        backdrop = new Group(background.getImageView(), tower.getImageView());
        backdrop.setEffect(new BoxBlur());
        tower.getImageView().setMouseTransparent(true);

        // Range Circle (mouse-transparent so it never steals hover/clicks)
        rangeIndicator = new Circle();
        rangeIndicator.setFill(Color.rgb(41, 128, 185, 0.22));
        rangeIndicator.setStroke(Color.rgb(41, 128, 185, 0.85));
        rangeIndicator.setStrokeWidth(1.5);
        rangeIndicator.setVisible(false);
        rangeIndicator.setMouseTransparent(true);

        // Click to place turret on grass, or deselect if clicking outside
        background.getImageView().setOnMouseClicked(e -> {
            if (turretMark.isHovering()) {
                Coord turretPos = turretMark.getGrassCoord();
                placeTurret(turretPos);
            } else {
                deselectTurret();
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

        // Mouse exited background: cleanly reset hover preview
        background.getImageView().setOnMouseExited(e -> {
            turretMark.hover(-9999, -9999);
            turretMark.update();
            if (selectedTurret == null) {
                rangeIndicator.setVisible(false);
            }
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

        botHudBtn = new Button("🤖 AI: OFF");
        botHudBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #2c3e50; -fx-text-fill: #bdc3c7; -fx-font-weight: bold;");
        botHudBtn.setOnAction(e -> toggleBot());

        muteBtn = new Button("🔊");
        muteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #34495e; -fx-text-fill: white;");
        muteBtn.setOnAction(e -> {
            SoundManager.toggleMute();
            muteBtn.setText(SoundManager.isMuted() ? "🔇" : "🔊");
        });

        settingsBtn = new Button("⚙");
        settingsBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #34495e; -fx-text-fill: white;");
        settingsBtn.setOnAction(e -> openSettings());

        HBox btnGroup = new HBox(4, pauseBtn, speedBtn, botHudBtn, muteBtn, settingsBtn);
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
        pauseOverlay.setOnMouseClicked(e -> togglePause());

        pauseText = new Label("IN PAUSA\n(Clicca per riprendere)");
        pauseText.setTextFill(Color.WHITE);
        pauseText.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-alignment: center;");
        pauseText.setTranslateX(WIDTH / 2.0 - 110);
        pauseText.setTranslateY(HEIGHT / 2.0 - 25);
        pauseText.setVisible(false);
        pauseText.setOnMouseClicked(e -> togglePause());

        gameWindow.getChildren().addAll(
            backdrop,
            rangeIndicator,
            turretMark.getImageView(),
            entities.getAll(),
            pauseOverlay,
            pauseText,
            topHudBox,
            turretControlBar
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
        buildVictoryMenu();

        // Wave Start handler
        startWaveButton.setOnAction(e -> startWave());

        // Setup Main Scalable Container with fixed-origin scaling
        gameContent.getChildren().addAll(gameWindow, mainMenu, pokedexMenu, settingsMenu, gameOverMenu, victoryMenu);
        gameContent.getTransforms().add(gameScale);
        gameContent.setClip(new Rectangle(0, 0, WIDTH, HEIGHT));
        gameContent.setManaged(false); // Unmanaged so its scaled size does not cause parent resize loops

        centerGamePane = new Pane(gameContent) {
            @Override
            protected void layoutChildren() {
                double w = getWidth();
                double h = getHeight();
                if (w > 0 && h > 0) {
                    double scale = Math.max(0.2, Math.min(w / WIDTH, h / HEIGHT));
                    if (Math.abs(scale - currentScale) > 0.0001) {
                        currentScale = scale;
                        gameScale.setX(scale);
                        gameScale.setY(scale);
                    }
                    double contentW = WIDTH * scale;
                    double contentH = HEIGHT * scale;
                    gameContent.setLayoutX(Math.floor((w - contentW) / 2.0));
                    gameContent.setLayoutY(Math.floor((h - contentH) / 2.0));
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

        // Keyboard shortcuts: F11 Fullscreen, Space or P to Pause/Resume
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            } else if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.P) {
                togglePause();
            }
        });

        try {
            primaryStage.getIcons().add(new Image(TowerDefense.class.getResourceAsStream("/Resource/app_icon.png")));
        } catch (Exception ignored) {}

        primaryStage.setScene(scene);
        primaryStage.show();

        initTimeline();
        timeline.play();
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
        Label el2 = new Label("💧 Acqua ➔ 🔥 Fuoco & 🪨 Roccia");
        el2.setStyle("-fx-font-size: 11px; -fx-text-fill: #3498db;");
        Label el3 = new Label("🔥 Fuoco ➔ 🌿 Erba");
        el3.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
        Label el4 = new Label("⚪ Normale: Base (Debole vs 🪨)");
        el4.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        typesCard.getChildren().addAll(typeTitle, el1, el2, el3, el4);

        // Status Effects Card
        VBox statusCard = new VBox(6);
        statusCard.setPadding(new Insets(10));
        statusCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");
        Label statusTitle = new Label("EFFETTI & CAPIPALESTRA:");
        statusTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Label st1 = new Label("⚡ Paralisi: -50% velocità");
        st1.setStyle("-fx-font-size: 10px; -fx-text-fill: #f39c12;");
        Label st2 = new Label("🔥 Scottatura: Danno continuo");
        st2.setStyle("-fx-font-size: 10px; -fx-text-fill: #e67e22;");
        Label st3 = new Label("👑 Boss ogni 5 ondate (-3 ❤️)");
        st3.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f1c40f;");

        statusCard.getChildren().addAll(statusTitle, st1, st2, st3);

        // Sidebar Footer placeholder
        leftPanelStatus = new Label("Pronto all'azione");
        leftPanelStatus.setStyle("-fx-font-size: 10px; -fx-text-fill: #7d8590;");

        sidebar.getChildren().addAll(header, typesCard, statusCard, leftPanelStatus);
        return sidebar;
    }

    private VBox buildRightSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(265);
        sidebar.setPadding(new Insets(14, 12, 14, 12));
        sidebar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");
        sidebar.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("AI BOT & BILANCIAMENTO");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        // Bot Card
        VBox botCard = new VBox(6);
        botCard.setPadding(new Insets(8));
        botCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");

        rightPanelBotToggleBtn = new Button("🤖 Attiva Bot AI (Auto-Play)");
        rightPanelBotToggleBtn.setMaxWidth(Double.MAX_VALUE);
        rightPanelBotToggleBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10;");
        rightPanelBotToggleBtn.setOnAction(e -> toggleBot());

        rightPanelSpeedBtn = new Button("⏩ Velocità: 1x (Normale)");
        rightPanelSpeedBtn.setMaxWidth(Double.MAX_VALUE);
        rightPanelSpeedBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d; -fx-padding: 4 8;");
        rightPanelSpeedBtn.setOnAction(e -> toggleSpeed());

        botStatusLabel = new Label("Stato: Bot in standby");
        botStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #8b949e;");
        botStatusLabel.setWrapText(true);

        botDpsLabel = new Label("⚡ DPS Stimato: ~0");
        botDpsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #f1c40f; -fx-font-weight: bold;");

        botWavesLabel = new Label("⭐ Ondate Perfette: 0");
        botWavesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #2ecc71;");

        botDamageLabel = new Label("❤️ Danni Subiti Palestra: 0");
        botDamageLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c;");

        botBalanceLabel = new Label("⚖️ Da valutare");
        botBalanceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #58a6ff; -fx-font-weight: bold;");

        Label logsTitle = new Label("LOG DECISIONI BOT:");
        logsTitle.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #7d8590; -fx-padding: 4 0 0 0;");

        botLogBox = new VBox(2);
        Label initLog = new Label("AI in standby");
        initLog.setStyle("-fx-font-size: 9px; -fx-text-fill: #8b949e; -fx-font-family: monospace;");
        botLogBox.getChildren().add(initLog);

        Button exportReportBtn = new Button("📄 Esporta Report (.md/.json)");
        exportReportBtn.setMaxWidth(Double.MAX_VALUE);
        exportReportBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #21262d; -fx-text-fill: #58a6ff; -fx-border-color: #30363d; -fx-padding: 4 8; -fx-font-weight: bold;");
        exportReportBtn.setOnAction(e -> {
            telemetry.exportReports(entities.getTowerHealth(), entities.getMaxTowerHealth(), gameOn ? "IN_CORSO" : "ATTESA");
            updateLabels();
        });

        botExportLabel = new Label("📁 Report: balance_report.md");
        botExportLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7d8590;");
        botExportLabel.setWrapText(true);

        botCard.getChildren().addAll(
            rightPanelBotToggleBtn,
            rightPanelSpeedBtn,
            botStatusLabel,
            botDpsLabel,
            botWavesLabel,
            botDamageLabel,
            botBalanceLabel,
            exportReportBtn,
            botExportLabel,
            logsTitle,
            botLogBox
        );

        // Gym Leader Live Status Card
        VBox gymCard = new VBox(6);
        gymCard.setPadding(new Insets(8));
        gymCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");

        Label gymTitle = new Label("DIFESA PALESTRA:");
        gymTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        rightPanelGymHp = new Label("❤️ Salute: 25 / 25");
        rightPanelGymHp.setStyle("-fx-font-size: 12px; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        gymCard.getChildren().addAll(gymTitle, rightPanelGymHp);

        // Shortcuts Card
        VBox controlsCard = new VBox(3);
        controlsCard.setPadding(new Insets(8));
        controlsCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 6; -fx-border-color: #30363d; -fx-border-radius: 6;");

        Label ctrlTitle = new Label("COMANDI RAPIDI:");
        ctrlTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Label c1 = new Label("⌨ F11: Schermo Intero");
        c1.setStyle("-fx-font-size: 9px; -fx-text-fill: #c9d1d9;");
        Label c2 = new Label("⌨ Spazio / P: Pausa");
        c2.setStyle("-fx-font-size: 9px; -fx-text-fill: #c9d1d9;");
        Label c3 = new Label("🖱 Click Erba: Piazza Torretta");
        c3.setStyle("-fx-font-size: 9px; -fx-text-fill: #c9d1d9;");
        Label c4 = new Label("🖱 Click Cespuglio: Info / Cambia");
        c4.setStyle("-fx-font-size: 9px; -fx-text-fill: #c9d1d9;");

        controlsCard.getChildren().addAll(ctrlTitle, c1, c2, c3, c4);

        sidebar.getChildren().addAll(header, botCard, gymCard, controlsCard);
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

    public boolean placeTurret(Coord turretPos) {
        if (entities.isLocationOccupied(turretPos.getX(), turretPos.getY(), 16.0)) {
            return false;
        }
        Turret turret = store.buyTurret(turretPos, 1);
        if (turret != null) {
            SoundManager.playBuy();
            turret.getView().setOnMouseClicked(evt -> {
                selectTurret(turret);
                evt.consume();
            });
            entities.add(turret);
            if (gameBot == null || !gameBot.isEnabled()) {
                selectTurret(turret);
            }
            updateLabels();
            return true;
        }
        return false;
    }

    public void toggleBot() {
        if (gameBot == null) return;
        gameBot.toggle();
        boolean on = gameBot.isEnabled();
        if (on) {
            botHudBtn.setText("🤖 AI: ON");
            botHudBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            if (rightPanelBotToggleBtn != null) {
                rightPanelBotToggleBtn.setText("⏹ Disattiva Bot AI");
                rightPanelBotToggleBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10;");
            }
        } else {
            botHudBtn.setText("🤖 AI: OFF");
            botHudBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: #2c3e50; -fx-text-fill: #bdc3c7; -fx-font-weight: bold;");
            if (rightPanelBotToggleBtn != null) {
                rightPanelBotToggleBtn.setText("🤖 Attiva Bot AI (Auto-Play)");
                rightPanelBotToggleBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10;");
            }
        }
        updateLabels();
    }

    public double getGameSpeed() {
        return gamespeed;
    }

    public boolean isGameOn() {
        return gameOn;
    }

    public void startWaveFromBot() {
        if (!gameOn && mainMenu.isVisible()) {
            startWave();
        }
    }

    private void togglePause() {
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

    public void toggleSpeed() {
        if (gamespeed == 10) {
            gamespeed = 5;
            speedBtn.setText("2x");
        } else if (gamespeed == 5) {
            gamespeed = 2.5;
            speedBtn.setText("4x");
        } else {
            gamespeed = 10;
            speedBtn.setText("1x");
        }
        if (rightPanelSpeedBtn != null) {
            rightPanelSpeedBtn.setText("⏩ Velocità: " + (gamespeed == 2.5 ? "4x Turbo" : (gamespeed == 5 ? "2x" : "1x")));
        }
        if (timeline != null) {
            timeline.stop();
            initTimeline();
            if (!isPaused) {
                timeline.play();
            }
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

        // Audio & Volume Section
        Label audioHeader = new Label("🔊 AUDIO & VOLUME:");
        audioHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8b949e;");

        Label volValueLabel = new Label("Volume: " + SoundManager.getVolumePercent() + "%");
        volValueLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Slider volSlider = new Slider(0, 100, SoundManager.getVolumePercent());
        volSlider.setPrefWidth(190);
        volSlider.setShowTickMarks(false);

        Button volDownBtn = new Button("➖");
        volDownBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");

        Button volUpBtn = new Button("➕");
        volUpBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");

        Button audioToggleBtn = new Button(SoundManager.isMuted() ? "🔇 Muto" : "🔊 Audio Attivo");
        audioToggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 14; -fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-border-color: #30363d;");

        Runnable updateVolumeUI = () -> {
            int pct = SoundManager.getVolumePercent();
            volValueLabel.setText("Volume: " + pct + "%");
            audioToggleBtn.setText(SoundManager.isMuted() ? "🔇 Muto" : "🔊 Audio Attivo");
            muteBtn.setText(SoundManager.isMuted() ? "🔇" : "🔊");
        };

        volSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            SoundManager.setVolume(newVal.doubleValue() / 100.0);
            updateVolumeUI.run();
        });

        volSlider.setOnMouseReleased(e -> SoundManager.playPreviewBeep());

        volDownBtn.setOnAction(e -> {
            double newV = Math.max(0.0, SoundManager.getVolume() - 0.10);
            SoundManager.setVolume(newV);
            volSlider.setValue(newV * 100);
            updateVolumeUI.run();
            SoundManager.playPreviewBeep();
        });

        volUpBtn.setOnAction(e -> {
            double newV = Math.min(1.0, SoundManager.getVolume() + 0.10);
            SoundManager.setVolume(newV);
            volSlider.setValue(newV * 100);
            updateVolumeUI.run();
            SoundManager.playPreviewBeep();
        });

        audioToggleBtn.setOnAction(e -> {
            SoundManager.toggleMute();
            volSlider.setValue(SoundManager.getVolumePercent());
            updateVolumeUI.run();
            if (!SoundManager.isMuted()) {
                SoundManager.playPreviewBeep();
            }
        });

        HBox sliderRow = new HBox(6, volDownBtn, volSlider, volUpBtn);
        sliderRow.setAlignment(Pos.CENTER);

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
            audioHeader,
            volValueLabel,
            sliderRow,
            audioToggleBtn,
            closeBtn
        );

        settingsMenu.getChildren().addAll(whiteback, settingsCard);
        settingsMenu.setVisible(false);
    }

    private void openSettings() {
        settingsMenu.setVisible(true);
        if (gameOn && !isPaused) {
            pausedBySettings = true;
            togglePause();
        }
    }

    private void closeSettings() {
        settingsMenu.setVisible(false);
        if (gameOn && isPaused && pausedBySettings) {
            pausedBySettings = false;
            togglePause();
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

    private void buildVictoryMenu() {
        Rectangle victoryBackdrop = new Rectangle(WIDTH, HEIGHT);
        victoryBackdrop.setFill(Color.rgb(0, 0, 0, 0.85));

        VBox victoryBox = new VBox(12);
        victoryBox.setAlignment(Pos.CENTER);
        victoryBox.setTranslateX(WIDTH / 4.0 - 50);
        victoryBox.setTranslateY(HEIGHT / 4.0 - 20);
        victoryBox.setPrefWidth(WIDTH / 2.0 + 100);
        victoryBox.setStyle("-fx-background-color: #161b22; -fx-padding: 22; -fx-background-radius: 12; "
                          + "-fx-border-color: #f1c40f; -fx-border-width: 2.5; -fx-border-radius: 12; "
                          + "-fx-effect: dropshadow(three-pass-box, rgba(241,196,15,0.4), 16, 0, 0, 4);");

        Label crownLabel = new Label("🏆 CAMPIONE DELLA LEGA POKÉMON 🏆");
        crownLabel.setTextFill(Color.rgb(241, 196, 15));
        crownLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label victorySub = new Label("Hai sconfitto il Campione Blu e completato tutte le 64 Ondate!\nLivello massimo 65 raggiunto con successo!");
        victorySub.setTextFill(Color.rgb(224, 230, 237));
        victorySub.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
        victorySub.setWrapText(true);

        victoryStatsLabel = new Label();
        victoryStatsLabel.setTextFill(Color.rgb(46, 204, 113));
        victoryStatsLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-alignment: center; -fx-line-spacing: 4px;");

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER);

        Button restartButton = new Button("Rigioca dall'Inizio ↻");
        restartButton.setStyle("-fx-font-size: 13px; -fx-padding: 8 16; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        restartButton.setOnAction(e -> {
            victoryMenu.setVisible(false);
            restartGame();
        });

        Button exportBtn = new Button("Esporta Report 📊");
        exportBtn.setStyle("-fx-font-size: 13px; -fx-padding: 8 16; -fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        exportBtn.setOnAction(e -> {
            telemetry.exportReports(entities.getTowerHealth(), entities.getMaxTowerHealth(), "VITTORIA_CAMPIONE");
            updateLabels();
        });

        btnBox.getChildren().addAll(restartButton, exportBtn);

        victoryBox.getChildren().addAll(crownLabel, victorySub, victoryStatsLabel, btnBox);
        victoryMenu.getChildren().addAll(victoryBackdrop, victoryBox);
        victoryMenu.setVisible(false);
    }

    private void showVictoryMenu() {
        if (timeline != null) {
            timeline.stop();
        }
        backdrop.setEffect(new BoxBlur());
        mainMenu.setVisible(false);
        gameOverMenu.setVisible(false);
        int hp = entities.getTowerHealth();
        int maxHp = entities.getMaxTowerHealth();
        int turretsCount = entities.getTurrets().size();
        int money = store.getMoney();
        int cleanWaves = gameBot != null ? gameBot.getCleanWavesCount() : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("❤️ Salute Palestra Finale: ").append(hp).append(" / ").append(maxHp).append("\n");
        sb.append("💰 Fondi Accumulati: $").append(money).append("\n");
        sb.append("🌿 Difensori Schierati: ").append(turretsCount).append("\n");
        sb.append("⭐ Ondate Senza Danni: ").append(cleanWaves).append(" / 64\n");
        if (hp >= 5) {
            sb.append("✅ Obiettivo Superato: Sopravvivenza con ≥ 5 HP (Residui: ").append(hp).append(" HP)!");
        } else {
            sb.append("⚠️ Vittoria al limite della resistenza!");
        }
        victoryStatsLabel.setText(sb.toString());
        victoryMenu.setVisible(true);
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

        spawned = 0;
        waveTime = 0;
        SoundManager.playWaveStart();

        boolean isBoss = (level % 5 == 0) || (level == 64);
        String bossName = "";
        if (isBoss) {
            Trainer b = entities.createBossTrainer(level);
            bossName = b.getTrainerName();
        }
        telemetry.onWaveStarted(level,
                                store.getMoney(),
                                entities.getTurrets().size(),
                                gameBot != null ? gameBot.getRosterSummary() : "N/D",
                                gameBot != null ? gameBot.calculateTotalDps() : 0.0,
                                isBoss,
                                bossName);

        updateLabels();
    }

    private void initTimeline() {
        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(gamespeed),
                new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent event) {
                        wave();

                        if (gameOn) {
                            // Wave completed condition
                            boolean isBossWave = (level % 5 == 0) || (level == 64);
                            int totalEnemies = isBossWave ? (3 + Math.min(18, level / 3)) : (level == 1 ? 2 : (level == 2 ? 3 : Math.min(26, 2 + level * 2)));
                            if (spawned >= totalEnemies && entities.getTrainers().isEmpty()) {
                                gameOn = false;
                                int waveBonus = 35 + level * 15;
                                store.spend(-waveBonus);
                                if (gameBot != null) {
                                    gameBot.onWaveCompleted(level);
                                }
                                telemetry.onWaveCompleted(level, store.getMoney(), entities.getTowerHealth(), entities.getMaxTowerHealth());

                                if (level >= 64) {
                                    level = 65; // Livello massimo 65 raggiunto: VITTORIA!
                                    SoundManager.playVictoryFanfare();
                                    telemetry.exportReports(entities.getTowerHealth(), entities.getMaxTowerHealth(), "VITTORIA_CAMPIONE");
                                    showVictoryMenu();
                                    updateLabels();
                                    return;
                                }

                                SoundManager.playWaveStart();
                                level++;
                                spawned = 0;
                                waveTime = 0;
                                startWaveButton.setText("▶ Inizia Ondata " + level + " (+$" + waveBonus + " Bonus!)");
                                backdrop.setEffect(new BoxBlur());
                                mainMenu.setVisible(true);
                                updateLabels();
                            }

                            // Defeat condition
                            if (entities.getTowerHealth() < 1) {
                                gameOn = false;
                                SoundManager.playGameOver();
                                towerLife.setText("Palestra Caduta! (0/" + entities.getMaxTowerHealth() + ")");
                                towerLife.setTextFill(Color.RED);
                                backdrop.setEffect(new BoxBlur());
                                telemetry.onGameOver(level, store.getMoney(), entities.getMaxTowerHealth());
                                VBox box = (VBox) gameOverMenu.getChildren().get(1);
                                Label waveLbl = (Label) box.getChildren().get(1);
                                waveLbl.setText("Ondata raggiunta: " + level);
                                gameOverMenu.setVisible(true);
                                updateLabels();
                            }
                        }
                    }
                }));
    }

    public void updateLabels() {
        creditLabel.setText("💰 $" + store.getMoney());
        turretCostLabel.setText("🌿 Cespuglio: $" + store.getTurretPrice());
        bank.setText("Fondi: $" + store.getMoney());

        int hp = entities.getTowerHealth();
        int maxHp = entities.getMaxTowerHealth();
        towerLife.setText("❤️ " + hp + " / " + maxHp);
        if (hp > 15) {
            towerLife.setTextFill(Color.rgb(46, 204, 113));
        } else if (hp > 7) {
            towerLife.setTextFill(Color.rgb(243, 156, 18));
        } else {
            towerLife.setTextFill(Color.rgb(231, 76, 60));
        }

        int alive = entities.getTrainers().size();
        boolean isBossWave = (level % 5 == 0) || (level == 64);
        int totalEnemies = isBossWave ? (3 + Math.min(18, level / 3)) : (level == 1 ? 2 : (level == 2 ? 3 : Math.min(26, 2 + level * 2)));
        if (level >= 65) {
            waveStatusLabel.setText("🏆 CAMPIONE (Lv. 65)");
            waveStatusLabel.setTextFill(Color.rgb(46, 204, 113));
        } else if (isBossWave) {
            waveStatusLabel.setText("👑 BOSS " + level + " (" + alive + "/" + totalEnemies + ")");
            waveStatusLabel.setTextFill(Color.rgb(241, 196, 15));
        } else {
            waveStatusLabel.setText("Ondata " + level + " (" + alive + "/" + totalEnemies + ")");
            waveStatusLabel.setTextFill(Color.rgb(52, 152, 219));
        }

        if (rightPanelGymHp != null) {
            rightPanelGymHp.setText("❤️ Salute: " + hp + " / " + maxHp);
        }
        if (leftPanelStatus != null) {
            if (level >= 65) {
                leftPanelStatus.setText("🏆 Vittoria! Campione di Kanto");
            } else if (gameOn) {
                leftPanelStatus.setText(isBossWave ? "👑 BATTAGLIA CAPOPALESTRA!" : "Battaglia in corso...");
            } else {
                leftPanelStatus.setText("In attesa dell'ondata " + level);
            }
        }

        if (botExportLabel != null) {
            botExportLabel.setText("📁 " + telemetry.getLastExportStatus());
        }

        if (gameBot != null) {
            if (botStatusLabel != null) {
                botStatusLabel.setText("Stato: " + gameBot.getCurrentStatus());
            }
            if (botDpsLabel != null) {
                botDpsLabel.setText("⚡ DPS Stimato: ~" + gameBot.calculateTotalDps());
            }
            if (botWavesLabel != null) {
                botWavesLabel.setText("⭐ Ondate Perfette: " + gameBot.getCleanWavesCount());
            }
            if (botDamageLabel != null) {
                botDamageLabel.setText("❤️ Danni Subiti Palestra: " + gameBot.getTotalDamageSustained());
            }
            if (botBalanceLabel != null) {
                botBalanceLabel.setText("⚖️ " + gameBot.getBalanceEvaluation(level));
            }
            if (botLogBox != null) {
                botLogBox.getChildren().clear();
                for (String log : gameBot.getRecentLogs()) {
                    Label l = new Label(log);
                    l.setStyle("-fx-font-size: 9px; -fx-text-fill: #8b949e; -fx-font-family: monospace;");
                    botLogBox.getChildren().add(l);
                }
            }
        }
    }

    public void restartGame() {
        gameOn = false;
        isPaused = false;
        pausedBySettings = false;
        pauseBtn.setText("⏸");
        pauseOverlay.setVisible(false);
        pauseText.setVisible(false);
        deselectTurret();
        gameOverMenu.setVisible(false);
        victoryMenu.setVisible(false);

        level = 1;
        spawned = 0;
        waveTime = 0;
        entities.reset();
        store.reset();
        telemetry.reset();
        if (gameBot != null) {
            gameBot.reset();
        }
        towerLife.setTextFill(Color.rgb(46, 204, 113));
        startWaveButton.setText("▶ Inizia Ondata 1");
        updateLabels();

        mainMenu.setVisible(true);
        backdrop.setEffect(new BoxBlur());

        if (timeline != null) {
            timeline.stop();
        }
        initTimeline();
        timeline.play();
    }

    public void wave() {
        telemetry.tick();

        if (!gameOn) {
            if (gameBot != null) {
                gameBot.tick(false, level);
            }
            updateLabels();
            return;
        }

        waveTime++;
        updateLabels();

        boolean isBossWave = (level % 5 == 0) || (level == 64);
        int totalEnemies = isBossWave ? (3 + Math.min(18, level / 3)) : (level == 1 ? 2 : (level == 2 ? 3 : Math.min(26, 2 + level * 2)));
        int spawnInterval = Math.max(70, 200 - level * 6);

        // Bot AI decision during combat
        if (gameBot != null) {
            gameBot.tick(true, level);
        }

        // Spawn new trainer
        if (waveTime % spawnInterval == 0 && spawned < totalEnemies) {
            spawned++;
            boolean spawnBossNow = (isBossWave && spawned == totalEnemies);
            entities.createNewTrainer(level, spawnBossNow);
        }

        // Move trainers
        entities.moveTrainers();

        // Attack trainers
        entities.attackTrainers();

        // Move attackers
        entities.moveAttackers();

        entities.updateAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}