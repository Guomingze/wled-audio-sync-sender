package local.wled.app;

import javafx.application.Platform;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Slider;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import local.wled.core.AudioDeviceCatalog;
import local.wled.core.DdpColorPalette;
import local.wled.core.DdpLayoutMode;
import local.wled.core.OutputMode;
import local.wled.core.SenderConfig;
import local.wled.core.SenderController;
import local.wled.core.SenderMetrics;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class WledAudioSyncSenderFxController {
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final Preferences FX_PREFS = Preferences.userNodeForPackage(WledAudioSyncSenderFxController.class);
  private static final String PREF_THEME = "javafx.theme";
  private static final String PREF_SPECTRUM_STYLE = "javafx.spectrumStyle";
  private static final String PREF_LOG_VISIBLE = "javafx.logVisible";
  private static final String PREF_EFFECT_INTENSITY = "javafx.effectIntensity";
  private static final String PREF_WINDOW_WIDTH = "javafx.windowWidth";
  private static final String PREF_WINDOW_HEIGHT = "javafx.windowHeight";

  private static final double SPECTRUM_HEIGHT_WITH_LOG = 178;
  private static final double SPECTRUM_HEIGHT_NO_LOG = 228;
  private static final int DISPLAY_SPECTRUM_BANDS = 32;
  private static final double SPECTRUM_GAMMA = 0.82;
  private static final double BAR_DECAY_STEP = 0.052;
  private static final double PEAK_DECAY_STEP = 0.02;
  private static final int PEAK_HOLD_TICKS = 6;
  private static final long UI_FRAME_INTERVAL_NANOS = 33_000_000L;
  private static final int STATS_UPDATE_INTERVAL = 3;

  private static final String THEME_CYBER_BLUE = "赛博蓝 (Cyber Blue)";
  private static final String THEME_ELECTRO_GREEN = "电子绿 (Electro Green)";
  private static final String THEME_WARM_AMBER = "暖橙 (Warm Amber)";
  private static final String THEME_NOIR_GRID = "暗栅 (Noir Grid)";
  private static final String THEME_PAPER_DAYLIGHT = "纸境 (Paper Daylight)";
  private static final String THEME_ARCADE_POP = "街机 (Arcade Pop)";

  private static final String SPECTRUM_STYLE_SMOOTH = "柔和 (Smooth)";
  private static final String SPECTRUM_STYLE_PUNCH = "冲击 (Punch)";
  private static final String SPECTRUM_STYLE_NEON = "霓虹 (Neon)";
  private static final String SPECTRUM_STYLE_CRYSTAL = "晶体 (Crystal)";
  private static final String SPECTRUM_STYLE_RETRO = "复古 (Retro)";
  private static final String SPECTRUM_STYLE_MECHA = "机械 (Mecha)";
  private static final String SPECTRUM_STYLE_PRISM = "棱镜 (Prism)";
  private static final String SPECTRUM_STYLE_WAVE = "波浪 (Wave)";
  private static final String SPECTRUM_STYLE_FUSION = "熔核 (Fusion)";

  private static final String STYLE_THEME_CYBER = "theme-cyber";
  private static final String STYLE_THEME_ELECTRO = "theme-electro";
  private static final String STYLE_THEME_AMBER = "theme-amber";
  private static final String STYLE_THEME_NOIR = "theme-noir";
  private static final String STYLE_THEME_PAPER = "theme-paper";
  private static final String STYLE_THEME_ARCADE = "theme-arcade";
  private static final String STYLE_SPECTRUM_SMOOTH = "spectrum-style-smooth";
  private static final String STYLE_SPECTRUM_PUNCH = "spectrum-style-punch";
  private static final String STYLE_SPECTRUM_NEON = "spectrum-style-neon";
  private static final String STYLE_SPECTRUM_CRYSTAL = "spectrum-style-crystal";
  private static final String STYLE_SPECTRUM_RETRO = "spectrum-style-retro";
  private static final String STYLE_SPECTRUM_MECHA = "spectrum-style-mecha";
  private static final String STYLE_SPECTRUM_PRISM = "spectrum-style-prism";
  private static final String STYLE_SPECTRUM_WAVE = "spectrum-style-wave";
  private static final String STYLE_SPECTRUM_FUSION = "spectrum-style-fusion";
  private static final String STYLE_ENERGY_SOFT = "spectrum-energy-soft";
  private static final String STYLE_ENERGY_MID = "spectrum-energy-mid";
  private static final String STYLE_ENERGY_HARD = "spectrum-energy-hard";
  private static final String REFRESH_BUTTON_INLINE_STYLE =
      "-fx-background-color: -app-input-bg;"
          + "-fx-background-insets: 0;"
          + "-fx-background-radius: 12;"
          + "-fx-border-color: -app-input-border;"
          + "-fx-border-width: 1;"
          + "-fx-border-radius: 12;"
          + "-fx-text-fill: -app-heading;"
          + "-fx-padding: 0;"
          + "-fx-font-size: 16;"
          + "-fx-font-weight: 900;"
          + "-fx-alignment: center;";
  private static final String LOG_AREA_INLINE_STYLE =
      "-fx-control-inner-background: -app-log-bg;"
          + "-fx-background-color: -app-log-bg;"
          + "-fx-text-fill: -app-log-text;"
          + "-fx-highlight-fill: -app-mark;"
          + "-fx-highlight-text-fill: -app-button-start-text;"
          + "-fx-border-color: -app-input-border;"
          + "-fx-border-radius: 14;"
          + "-fx-background-radius: 14;"
          + "-fx-faint-focus-color: transparent;";
  private static final String LOG_AREA_SURFACE_STYLE =
      "-fx-background-color: -app-log-bg;"
          + "-fx-background-insets: 0;"
          + "-fx-background-radius: 14;";
  private static final String LOG_SCROLLBAR_BASE_STYLE = "-fx-background-color: transparent; -fx-padding: 2 2 2 0;";
  private static final String LOG_SCROLLBAR_TRACK_STYLE = "-fx-background-color: -app-log-scroll-track; -fx-background-radius: 8;";
  private static final String LOG_SCROLLBAR_THUMB_STYLE = "-fx-background-color: -app-log-scroll-thumb; -fx-background-radius: 8;";
  private static final String LOG_SCROLLBAR_BUTTON_STYLE = "-fx-background-color: transparent; -fx-padding: 0;";
  private static final String LOG_SCROLLBAR_ARROW_STYLE = "-fx-background-color: transparent; -fx-padding: 0;";

  @FXML
  private BorderPane rootPane;
  @FXML
  private VBox consoleCard;
  @FXML
  private VBox mainColumn;
  @FXML
  private VBox logCard;
  @FXML
  private StackPane spectrumFrame;
  @FXML
  private Canvas spectrumCanvas;

  @FXML
  private Label titleLabel;
  @FXML
  private Label subtitleLabel;
  @FXML
  private Label consoleTitleLabel;
  @FXML
  private Label meterTitleLabel;
  @FXML
  private Label logTitleLabel;
  @FXML
  private Label themeLabel;
  @FXML
  private Label spectrumStyleLabel;
  @FXML
  private Label statsLabel;
  @FXML
  private Label effectIntensityValueLabel;

  @FXML
  private Button refreshButton;
  @FXML
  private Label refreshStatusLabel;

  @FXML
  private ComboBox<String> inputBox;
  @FXML
  private ComboBox<String> themeBox;
  @FXML
  private ComboBox<String> outputModeBox;
  @FXML
  private ComboBox<String> ddpLayoutBox;
  @FXML
  private ComboBox<String> ddpPaletteBox;
  @FXML
  private ComboBox<String> spectrumStyleBox;
  @FXML
  private Slider effectIntensitySlider;
  @FXML
  private StackPane effectIntensitySliderWrap;

  @FXML
  private TextField destField;
  @FXML
  private TextField portField;
  @FXML
  private TextField pixelCountField;
  @FXML
  private TextField rateField;
  @FXML
  private TextField channelsField;
  @FXML
  private TextField fftField;
  @FXML
  private TextField fpsField;

  @FXML
  private CheckBox verboseCheck;
  @FXML
  private CheckBox logVisibleCheck;
  @FXML
  private ToggleButton masterSwitchCheck;
  @FXML
  private ToggleButton captureSwitchCheck;
  @FXML
  private ToggleButton pushSwitchCheck;

  @FXML
  private TextArea logArea;

  private SenderController sender;
  private GraphicsContext spectrumGraphics;
  private double[] smoothedBands;
  private double[] peakBands;
  private int[] peakHoldLeft;
  private String activeTheme = THEME_CYBER_BLUE;
  private String activeSpectrumStyle = SPECTRUM_STYLE_SMOOTH;
  private SpectrumPalette activeSpectrumPalette = SpectrumPalette.forStyle(SPECTRUM_STYLE_SMOOTH, THEME_CYBER_BLUE);
  private double activeBarGap = 5.0;
  private double activeBarCorner = 6.0;
  private double activePeakThickness = 2.0;
  private double effectIntensity = 0.62;
  private int activeEnergyLevel = -1;
  private int statsUpdateCountdown;
  private final Object metricsRenderLock = new Object();
  private SenderMetrics pendingMetrics;
  private boolean renderScheduled;
  private long lastUiRenderAtNanos;
  private boolean syncingMasterSwitch;
  private Timeline refreshFeedbackTimeline;
  private Timeline refreshResultTimeline;
  private final Tooltip refreshTooltip = new Tooltip("刷新输入设备列表");
  private final Map<ToggleButton, SwitchVisual> switchVisuals = new HashMap<>();

  private Stage stage;
  private double initialWindowWidth = 980.0;
  private double initialWindowHeight = 660.0;

  void setup(Args args, Stage stage) {
    this.stage = stage;
    Args effectiveArgs = args;
    if (effectiveArgs == null) {
      effectiveArgs = Args.parse(new String[0]);
    }

    String savedTheme = sanitizeTheme(FX_PREFS.get(PREF_THEME, THEME_CYBER_BLUE));
    String savedSpectrumStyle = sanitizeSpectrumStyle(FX_PREFS.get(PREF_SPECTRUM_STYLE, SPECTRUM_STYLE_SMOOTH));
    boolean savedLogVisible = FX_PREFS.getBoolean(PREF_LOG_VISIBLE, false);
    effectIntensity = clampWindowSize(FX_PREFS.getDouble(PREF_EFFECT_INTENSITY, 0.62), 0.0, 1.0);
    initialWindowWidth = clampWindowSize(FX_PREFS.getDouble(PREF_WINDOW_WIDTH, 980.0), 840.0, 1960.0);
    initialWindowHeight = clampWindowSize(FX_PREFS.getDouble(PREF_WINDOW_HEIGHT, 660.0), 520.0, 1280.0);

    inputBox.setEditable(true);
    themeBox.getItems().setAll(
        THEME_CYBER_BLUE,
        THEME_ELECTRO_GREEN,
        THEME_WARM_AMBER,
        THEME_NOIR_GRID,
        THEME_PAPER_DAYLIGHT,
        THEME_ARCADE_POP
    );
    themeBox.setValue(savedTheme);
    outputModeBox.getItems().setAll(OutputMode.AUDIO_SYNC_V2.uiLabel(), OutputMode.DDP.uiLabel());
    outputModeBox.setValue(effectiveArgs.outputMode.uiLabel());
    ddpLayoutBox.getItems().setAll(DdpLayoutMode.STRETCH.uiLabel(), DdpLayoutMode.REPEAT.uiLabel(), DdpLayoutMode.MIRROR.uiLabel());
    ddpLayoutBox.setValue(effectiveArgs.ddpLayoutMode.uiLabel());
    ddpPaletteBox.getItems().setAll(
        DdpColorPalette.AURORA.uiLabel(),
        DdpColorPalette.SUNSET.uiLabel(),
        DdpColorPalette.FIRE.uiLabel(),
        DdpColorPalette.OCEAN.uiLabel(),
        DdpColorPalette.CANDY.uiLabel()
    );
    ddpPaletteBox.setValue(effectiveArgs.ddpColorPalette.uiLabel());
    ddpLayoutBox.valueProperty().addListener((obs, oldValue, newValue) -> applyLiveDdpRuntimeSettings());
    ddpPaletteBox.valueProperty().addListener((obs, oldValue, newValue) -> applyLiveDdpRuntimeSettings());
    spectrumStyleBox.getItems().setAll(
        SPECTRUM_STYLE_SMOOTH,
        SPECTRUM_STYLE_PUNCH,
        SPECTRUM_STYLE_NEON,
        SPECTRUM_STYLE_CRYSTAL,
        SPECTRUM_STYLE_RETRO,
        SPECTRUM_STYLE_MECHA,
        SPECTRUM_STYLE_PRISM,
        SPECTRUM_STYLE_WAVE,
        SPECTRUM_STYLE_FUSION
    );
    spectrumStyleBox.setValue(savedSpectrumStyle);
    effectIntensitySlider.setMinWidth(72.0);
    effectIntensitySlider.setPrefWidth(72.0);
    effectIntensitySlider.setMaxWidth(72.0);
    effectIntensityValueLabel.setMinWidth(48.0);
    effectIntensityValueLabel.setPrefWidth(48.0);
    effectIntensityValueLabel.setMaxWidth(48.0);
    effectIntensityValueLabel.setTextOverrun(OverrunStyle.CLIP);
    installBoundsClip(effectIntensitySliderWrap);
    effectIntensitySlider.setValue(effectIntensity * 100.0);
    updateEffectIntensityLabel();
    effectIntensitySlider.valueProperty().addListener((obs, oldValue, newValue) -> {
      effectIntensity = clampWindowSize(newValue.doubleValue() / 100.0, 0.0, 1.0);
      FX_PREFS.putDouble(PREF_EFFECT_INTENSITY, effectIntensity);
      updateEffectIntensityLabel();
      drawSpectrumCanvas();
    });

    refreshDevices(effectiveArgs.inputDeviceQuery);
    destField.setText(effectiveArgs.dest);
    portField.setText(String.valueOf(effectiveArgs.port));
    pixelCountField.setText(String.valueOf(effectiveArgs.ddpPixelCount));
    rateField.setText(String.valueOf(effectiveArgs.sampleRate));
    channelsField.setText(String.valueOf(effectiveArgs.channels));
    fftField.setText(String.valueOf(effectiveArgs.fftSize));
    fpsField.setText(String.valueOf(effectiveArgs.fps));
    verboseCheck.setSelected(effectiveArgs.verbose);
    verboseCheck.selectedProperty().addListener((obs, oldValue, newValue) -> {
      if (Boolean.TRUE.equals(newValue) && !logVisibleCheck.isSelected()) {
        logVisibleCheck.setSelected(true);
      }
      if (sender != null && sender.isRunning()) {
        sender.setVerboseEnabled(newValue);
      }
    });
    logVisibleCheck.setSelected(savedLogVisible);
    masterSwitchCheck.setSelected(false);
    captureSwitchCheck.setSelected(true);
    pushSwitchCheck.setSelected(false);
    installAnimatedSwitch(masterSwitchCheck);
    installAnimatedSwitch(captureSwitchCheck);
    installAnimatedSwitch(pushSwitchCheck);
    refreshButton.getStyleClass().add("refresh-button");
    refreshButton.setStyle(REFRESH_BUTTON_INLINE_STYLE);
    refreshButton.setMinSize(30, 28);
    refreshButton.setPrefSize(30, 28);
    refreshButton.setMaxSize(30, 28);
    refreshButton.setTooltip(refreshTooltip);
    refreshStatusLabel.setText("");
    refreshStatusLabel.setManaged(false);
    refreshStatusLabel.setVisible(false);
    logArea.getStyleClass().add("log-area");
    logArea.setStyle(LOG_AREA_INLINE_STYLE);
    logArea.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(this::applyLogAreaSkinStyles));
    Platform.runLater(this::applyLogAreaSkinStyles);
    refreshOutputModeUiState(true);

    logCard.visibleProperty().bind(logVisibleCheck.selectedProperty());
    logCard.managedProperty().bind(logVisibleCheck.selectedProperty());
    logVisibleCheck.selectedProperty().addListener((obs, oldValue, newValue) -> {
      FX_PREFS.putBoolean(PREF_LOG_VISIBLE, newValue);
      updateSpectrumRowSizing(newValue);
    });
    updateSpectrumRowSizing(savedLogVisible);

    buildSpectrumCanvas();
    applyTheme(savedTheme);
    applySpectrumStyle(savedSpectrumStyle);
    resetSpectrumView();

    stage.setMinWidth(840);
    stage.setMinHeight(520);
    stage.setTitle(WledAudioSyncSender.APP_NAME);
    stage.setOnCloseRequest(e -> shutdown());
  }

  double getInitialWindowWidth() {
    return initialWindowWidth;
  }

  double getInitialWindowHeight() {
    return initialWindowHeight;
  }

  void shutdown() {
    persistWindowSize();
    stopSender();
  }

  @FXML
  private void onRefreshDevices() {
    int deviceCount = refreshDevices(selectedInput());
    playRefreshFeedback(deviceCount);
  }

  @FXML
  private void onClearLog() {
    if (logArea != null) {
      logArea.clear();
    }
  }

  private void playRefreshFeedback(int deviceCount) {
    if (refreshButton == null) {
      return;
    }

    if (refreshFeedbackTimeline != null) {
      refreshFeedbackTimeline.stop();
    }
    if (refreshResultTimeline != null) {
      refreshResultTimeline.stop();
    }

    refreshButton.setDisable(true);
    refreshButton.setText("↻");
    refreshButton.setRotate(0.0);
    refreshButton.setScaleX(1.0);
    refreshButton.setScaleY(1.0);
    refreshButton.getStyleClass().remove("refresh-icon-button-success");
    refreshStatusLabel.setText("");
    refreshStatusLabel.setManaged(false);
    refreshStatusLabel.setVisible(false);

    refreshFeedbackTimeline = new Timeline(
        new KeyFrame(
            Duration.ZERO,
            new KeyValue(refreshButton.rotateProperty(), 0.0, Interpolator.EASE_BOTH),
            new KeyValue(refreshButton.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
            new KeyValue(refreshButton.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
        ),
        new KeyFrame(
            Duration.millis(180),
            new KeyValue(refreshButton.rotateProperty(), 360.0, Interpolator.EASE_BOTH),
            new KeyValue(refreshButton.scaleXProperty(), 0.92, Interpolator.EASE_BOTH),
            new KeyValue(refreshButton.scaleYProperty(), 0.92, Interpolator.EASE_BOTH)
        ),
        new KeyFrame(
            Duration.millis(320),
            new KeyValue(refreshButton.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
            new KeyValue(refreshButton.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
        )
    );
    refreshFeedbackTimeline.setOnFinished(e -> {
      refreshButton.setRotate(0.0);
      refreshButton.setDisable(false);
      refreshButton.setText("✓");
      refreshButton.getStyleClass().add("refresh-icon-button-success");
      String refreshedAt = LocalTime.now().format(TIME_FORMATTER);
      refreshTooltip.setText("已刷新: " + deviceCount + " 个设备 @ " + refreshedAt);
      refreshStatusLabel.setText("已刷新 " + deviceCount + " @ " + refreshedAt);
      refreshStatusLabel.setManaged(true);
      refreshStatusLabel.setVisible(true);

      refreshResultTimeline = new Timeline(
          new KeyFrame(Duration.millis(1200),
              new KeyValue(refreshButton.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
              new KeyValue(refreshButton.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
          )
      );
      refreshResultTimeline.setOnFinished(done -> {
        refreshButton.setText("↻");
        refreshButton.getStyleClass().remove("refresh-icon-button-success");
        refreshStatusLabel.setText("");
        refreshStatusLabel.setManaged(false);
        refreshStatusLabel.setVisible(false);
      });
      refreshResultTimeline.play();
    });
    refreshFeedbackTimeline.play();
  }

  @FXML
  private void onThemeChanged() {
    applyTheme(themeBox.getValue());
  }

  @FXML
  private void onOutputModeChanged() {
    refreshOutputModeUiState(false);
  }

  @FXML
  private void onSpectrumStyleChanged() {
    applySpectrumStyle(spectrumStyleBox.getValue());
  }

  @FXML
  private void onMasterSwitchChanged() {
    if (syncingMasterSwitch) {
      return;
    }
    if (masterSwitchCheck.isSelected()) {
      startSender();
    } else {
      stopSender();
    }
  }

  @FXML
  private void onCaptureSwitchChanged() {
    if (sender != null && sender.isRunning()) {
      sender.setCaptureEnabled(captureSwitchCheck.isSelected());
    }
  }

  @FXML
  private void onPushSwitchChanged() {
    if (sender != null && sender.isRunning()) {
      if (pushSwitchCheck.isSelected()) {
        sender.setPushTarget(selectedPushDest(), selectedPushPort());
      }
      sender.setPushEnabled(pushSwitchCheck.isSelected());
    }
  }

  private void applyLiveDdpRuntimeSettings() {
    if (sender == null || !sender.isRunning()) {
      return;
    }
    if (selectedOutputMode() != OutputMode.DDP) {
      return;
    }
    sender.setDdpLayoutMode(selectedDdpLayoutMode());
    sender.setDdpColorPalette(selectedDdpColorPalette());
  }

  private void startSender() {
    if (sender != null && sender.isRunning()) {
      return;
    }

    SenderConfig cfg;
    try {
      cfg = readConfigFromForm();
    } catch (RuntimeException ex) {
      appendLog("参数错误: " + ex.getMessage());
      syncMasterSwitch(false);
      return;
    }

    sender = new SenderController(
        cfg,
        this::appendLog,
        this::updateMetrics,
        t -> appendLog("错误: " + t.getMessage())
    );

    resetSpectrumView();
    boolean verboseEnabled = verboseCheck.isSelected();
    if (verboseEnabled && !logVisibleCheck.isSelected()) {
      logVisibleCheck.setSelected(true);
    }
    sender.setVerboseEnabled(verboseEnabled);
    sender.setCaptureEnabled(captureSwitchCheck.isSelected());
    sender.setPushEnabled(pushSwitchCheck.isSelected());
    sender.start();
  }

  private void stopSender() {
    if (sender != null) {
      sender.stop();
    }
  }

  private void syncMasterSwitch(boolean enabled) {
    if (masterSwitchCheck.isSelected() == enabled) {
      return;
    }
    syncingMasterSwitch = true;
    masterSwitchCheck.setSelected(enabled);
    syncingMasterSwitch = false;
  }

  private void installAnimatedSwitch(ToggleButton toggle) {
    Region track = new Region();
    track.getStyleClass().add("switch-track");
    track.setMinSize(38, 20);
    track.setPrefSize(38, 20);
    track.setMaxSize(38, 20);

    Region thumb = new Region();
    thumb.getStyleClass().add("switch-thumb");
    thumb.setMinSize(14, 14);
    thumb.setPrefSize(14, 14);
    thumb.setMaxSize(14, 14);

    StackPane graphic = new StackPane(track, thumb);
    graphic.getStyleClass().add("switch-graphic");
    graphic.setMinSize(38, 20);
    graphic.setPrefSize(38, 20);
    graphic.setMaxSize(38, 20);

    toggle.setText("");
    toggle.setGraphic(graphic);
    applySwitchState(toggle, false);
    toggle.selectedProperty().addListener((obs, oldVal, newVal) -> applySwitchState(toggle, true));
    switchVisuals.put(toggle, new SwitchVisual(graphic, thumb));
  }

  private void applySwitchState(ToggleButton toggle, boolean animated) {
    SwitchVisual visual = switchVisuals.get(toggle);
    if (visual == null) {
      StackPane graphic = (StackPane) toggle.getGraphic();
      if (graphic == null || graphic.getChildren().size() < 2 || !(graphic.getChildren().get(1) instanceof Region)) {
        return;
      }
      visual = new SwitchVisual(graphic, (Region) graphic.getChildren().get(1));
      switchVisuals.put(toggle, visual);
    }

    double toX = toggle.isSelected() ? 9.0 : -9.0;
    visual.graphic.getStyleClass().remove("on");
    if (toggle.isSelected()) {
      visual.graphic.getStyleClass().add("on");
    }

    if (!animated) {
      visual.thumb.setTranslateX(toX);
      return;
    }

    Timeline timeline = new Timeline(
        new KeyFrame(Duration.millis(0),
            new KeyValue(visual.thumb.translateXProperty(), visual.thumb.getTranslateX(), Interpolator.EASE_BOTH),
            new KeyValue(visual.thumb.scaleXProperty(), visual.thumb.getScaleX(), Interpolator.EASE_BOTH),
            new KeyValue(visual.thumb.scaleYProperty(), visual.thumb.getScaleY(), Interpolator.EASE_BOTH)
        ),
        new KeyFrame(Duration.millis(85),
            new KeyValue(visual.thumb.scaleXProperty(), 1.10, Interpolator.EASE_BOTH),
            new KeyValue(visual.thumb.scaleYProperty(), 1.10, Interpolator.EASE_BOTH)
        ),
        new KeyFrame(Duration.millis(170),
            new KeyValue(visual.thumb.translateXProperty(), toX, Interpolator.EASE_BOTH),
            new KeyValue(visual.thumb.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
            new KeyValue(visual.thumb.scaleYProperty(), 1.0, Interpolator.EASE_BOTH)
        )
    );
    timeline.play();
  }

  private static final class SwitchVisual {
    final StackPane graphic;
    final Region thumb;

    SwitchVisual(StackPane graphic, Region thumb) {
      this.graphic = graphic;
      this.thumb = thumb;
    }
  }

  private void buildSpectrumCanvas() {
    smoothedBands = new double[DISPLAY_SPECTRUM_BANDS];
    peakBands = new double[DISPLAY_SPECTRUM_BANDS];
    peakHoldLeft = new int[DISPLAY_SPECTRUM_BANDS];
    spectrumGraphics = spectrumCanvas.getGraphicsContext2D();

    spectrumCanvas.setManaged(false);
    spectrumCanvas.widthProperty().bind(spectrumFrame.widthProperty());
    spectrumCanvas.heightProperty().bind(spectrumFrame.heightProperty());
    spectrumCanvas.widthProperty().addListener((obs, oldValue, newValue) -> drawSpectrumCanvas());
    spectrumCanvas.heightProperty().addListener((obs, oldValue, newValue) -> drawSpectrumCanvas());
  }

  private void updateSpectrumRowSizing(boolean logVisible) {
    if (mainColumn != null) {
      mainColumn.setPrefWidth(Region.USE_COMPUTED_SIZE);
      mainColumn.setMaxWidth(Double.MAX_VALUE);
    }

    spectrumFrame.setMaxWidth(Double.MAX_VALUE);
    spectrumFrame.setPrefWidth(Region.USE_COMPUTED_SIZE);
    spectrumFrame.setMinWidth(0.0);

    if (spectrumCanvas == null) {
      return;
    }

    double slotHeight = logVisible ? SPECTRUM_HEIGHT_WITH_LOG : SPECTRUM_HEIGHT_NO_LOG;
    double canvasHeight = slotHeight + 24.0;
    spectrumFrame.setMinHeight(canvasHeight);
    spectrumFrame.setPrefHeight(canvasHeight);
    spectrumFrame.setMaxHeight(Double.MAX_VALUE);
    drawSpectrumCanvas();
  }

  private int refreshDevices(String preferred) {
    inputBox.getItems().setAll(AudioDeviceCatalog.listCaptureDeviceNames());
    if (preferred != null && !preferred.trim().isEmpty()) {
      inputBox.getEditor().setText(preferred);
    }
    return inputBox.getItems().size();
  }

  private String selectedInput() {
    String fromEditor = inputBox.getEditor().getText();
    if (fromEditor != null && !fromEditor.trim().isEmpty()) {
      return fromEditor.trim();
    }
    String selected = inputBox.getSelectionModel().getSelectedItem();
    return selected == null ? "" : selected;
  }

  private SenderConfig readConfigFromForm() {
    OutputMode outputMode = selectedOutputMode();
    int port = parseInt(portField.getText(), Args.DEFAULT_PORT);
    if (outputMode == OutputMode.DDP && (port <= 0 || port == Args.DEFAULT_PORT)) {
      port = Args.DEFAULT_DDP_PORT;
    }
    int rate = parseInt(rateField.getText(), Args.DEFAULT_SAMPLE_RATE);
    int channels = parseInt(channelsField.getText(), Args.DEFAULT_CHANNELS);
    int fft = parseInt(fftField.getText(), Args.DEFAULT_FFT_SIZE);
    int fps = parseInt(fpsField.getText(), Args.DEFAULT_FPS);
    int ddpPixelCount = parseInt(pixelCountField.getText(), Args.DEFAULT_DDP_PIXELS);
    DdpLayoutMode ddpLayoutMode = selectedDdpLayoutMode();
    DdpColorPalette ddpColorPalette = selectedDdpColorPalette();
    if ((fft & (fft - 1)) != 0) {
      throw new IllegalArgumentException("频谱窗口大小 (FFT Size) 必须为 2 的幂");
    }
    if (channels < 1 || channels > 2) {
      throw new IllegalArgumentException("声道数 (Channels) 只能是 1 或 2");
    }
    if (ddpPixelCount < 1 || ddpPixelCount > 4096) {
      throw new IllegalArgumentException("DDP 像素数 (Pixels) 必须在 1 到 4096 之间");
    }

    String dest = destField.getText();
    if (dest == null || dest.trim().isEmpty()) {
      dest = Args.DEFAULT_DEST;
    }
    return new SenderConfig(
        dest.trim(),
        port,
        selectedInput(),
        rate,
        channels,
        fft,
        fps,
        verboseCheck.isSelected(),
        outputMode,
        ddpPixelCount,
        ddpLayoutMode,
        ddpColorPalette,
        captureSwitchCheck.isSelected(),
        pushSwitchCheck.isSelected()
    );
  }

  private OutputMode selectedOutputMode() {
    return OutputMode.fromUiLabel(outputModeBox.getValue());
  }

  private DdpLayoutMode selectedDdpLayoutMode() {
    return DdpLayoutMode.fromUiLabel(ddpLayoutBox.getValue());
  }

  private DdpColorPalette selectedDdpColorPalette() {
    return DdpColorPalette.fromUiLabel(ddpPaletteBox.getValue());
  }

  private void refreshOutputModeUiState(boolean keepPort) {
    OutputMode selectedMode = selectedOutputMode();
    boolean ddpMode = selectedMode == OutputMode.DDP;
    pixelCountField.setDisable(!ddpMode);
    ddpLayoutBox.setDisable(!ddpMode);
    ddpPaletteBox.setDisable(!ddpMode);

    if (keepPort) {
      return;
    }
    String text = portField.getText();
    int currentPort = parseInt(text, selectedMode.defaultPort());
    boolean blankPort = text == null || text.trim().isEmpty();
    if (ddpMode && (blankPort || currentPort == OutputMode.AUDIO_SYNC_V2.defaultPort())) {
      portField.setText(String.valueOf(OutputMode.DDP.defaultPort()));
    } else if (!ddpMode && (blankPort || currentPort == OutputMode.DDP.defaultPort())) {
      portField.setText(String.valueOf(OutputMode.AUDIO_SYNC_V2.defaultPort()));
    }
  }

  private static int parseInt(String s, int def) {
    if (s == null) {
      return def;
    }
    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return def;
    }
  }

  private String selectedPushDest() {
    String dest = destField.getText();
    if (dest == null || dest.trim().isEmpty()) {
      return Args.DEFAULT_DEST;
    }
    return dest.trim();
  }

  private int selectedPushPort() {
    int fallbackPort = selectedOutputMode().defaultPort();
    int port = parseInt(portField.getText(), fallbackPort);
    return port > 0 ? port : fallbackPort;
  }

  private void appendLog(String text) {
    Platform.runLater(() -> {
      logArea.appendText(text + "\n");
      logArea.positionCaret(logArea.getLength());
      logArea.setScrollTop(Double.MAX_VALUE);
      if (text.startsWith("发送已停止") || text.startsWith("错误:")) {
        syncMasterSwitch(false);
      }
    });
  }

  private void applyLogAreaSkinStyles() {
    styleLogAreaNode(".content", LOG_AREA_SURFACE_STYLE);
    styleLogAreaNode(".viewport", LOG_AREA_SURFACE_STYLE);
    styleLogAreaNode(".scroll-pane", LOG_AREA_SURFACE_STYLE);
    styleLogAreaNode(".corner", LOG_AREA_SURFACE_STYLE);
    styleLogAreaScrollBars();
  }

  private void styleLogAreaNode(String selector, String style) {
    Node node = logArea.lookup(selector);
    if (node != null) {
      node.setStyle(style);
    }
  }

  private void styleLogAreaScrollBars() {
    for (Node barNode : logArea.lookupAll(".scroll-bar:vertical")) {
      barNode.setStyle(LOG_SCROLLBAR_BASE_STYLE);
      if (barNode instanceof ScrollBar) {
        ((ScrollBar) barNode).setPrefWidth(11);
      }
      styleChildNode(barNode, ".track", LOG_SCROLLBAR_TRACK_STYLE);
      styleChildNode(barNode, ".thumb", LOG_SCROLLBAR_THUMB_STYLE);
      styleChildNode(barNode, ".increment-button", LOG_SCROLLBAR_BUTTON_STYLE);
      styleChildNode(barNode, ".decrement-button", LOG_SCROLLBAR_BUTTON_STYLE);
      styleChildNode(barNode, ".increment-arrow", LOG_SCROLLBAR_ARROW_STYLE);
      styleChildNode(barNode, ".decrement-arrow", LOG_SCROLLBAR_ARROW_STYLE);
    }
  }

  private void styleChildNode(Node root, String selector, String style) {
    Node child = root.lookup(selector);
    if (child != null) {
      child.setStyle(style);
    }
  }

  private void updateMetrics(SenderMetrics m) {
    boolean shouldSchedule = false;
    synchronized (metricsRenderLock) {
      pendingMetrics = m;
      long now = System.nanoTime();
      boolean canRenderNow = (lastUiRenderAtNanos == 0L) || (now - lastUiRenderAtNanos >= UI_FRAME_INTERVAL_NANOS);
      if (!renderScheduled && canRenderNow) {
        renderScheduled = true;
        shouldSchedule = true;
      }
    }
    if (shouldSchedule) {
      Platform.runLater(this::renderPendingMetrics);
    }
  }

  private void renderPendingMetrics() {
    SenderMetrics m;
    synchronized (metricsRenderLock) {
      m = pendingMetrics;
      pendingMetrics = null;
      renderScheduled = false;
      lastUiRenderAtNanos = System.nanoTime();
    }
    if (m == null) {
      return;
    }

    byte[] spectrum = m.spectrum16;
    if (spectrum != null && smoothedBands != null && smoothedBands.length > 0) {
      double gamma = currentSpectrumGamma();
      double riseLerp = currentRiseLerp();
      double barDecayStep = currentBarDecayStep();
      double peakDecayStep = currentPeakDecayStep();
      int peakHoldTicks = currentPeakHoldTicks();
      double noiseFloor = currentNoiseFloor();
      double energySum = 0.0;
      for (int i = 0; i < smoothedBands.length; i++) {
        int v = sampleSpectrumBand(spectrum, i, smoothedBands.length);
        double n = Math.max(0.0, Math.min(1.0, v / 255.0));
        double target = Math.pow(n, gamma);
        target = Math.max(0.0, (target - noiseFloor) / (1.0 - noiseFloor));
        double current = smoothedBands[i];

        if (target > current) {
          current = current + (target - current) * riseLerp;
        } else {
          current = Math.max(target, current - barDecayStep);
        }
        smoothedBands[i] = Math.max(0.0, Math.min(1.0, current));
        energySum += smoothedBands[i];

        if (smoothedBands[i] >= peakBands[i]) {
          peakBands[i] = smoothedBands[i];
          peakHoldLeft[i] = peakHoldTicks;
        } else if (peakHoldLeft[i] > 0) {
          peakHoldLeft[i]--;
        } else {
          peakBands[i] = Math.max(smoothedBands[i], peakBands[i] - peakDecayStep);
        }
      }
      updateSpectrumEnergyClass(energySum / smoothedBands.length);
      drawSpectrumCanvas();
    }

    if (statsUpdateCountdown <= 0) {
      statsLabel.setText(formatStatsLine(
          m.frameCounter,
          m.smoothedAmp,
          m.peak,
          m.majorPeakHz,
          m.magnitude));
      statsUpdateCountdown = STATS_UPDATE_INTERVAL - 1;
    } else {
      statsUpdateCountdown--;
    }

    boolean shouldSchedule = false;
    synchronized (metricsRenderLock) {
      if (pendingMetrics != null && !renderScheduled) {
        long now = System.nanoTime();
        boolean canRenderNow = now - lastUiRenderAtNanos >= UI_FRAME_INTERVAL_NANOS;
        if (canRenderNow) {
          renderScheduled = true;
          shouldSchedule = true;
        }
      }
    }
    if (shouldSchedule) {
      Platform.runLater(this::renderPendingMetrics);
    }
  }

  private static String formatStatsLine(long frameCounter,
                                        double smoothedAmp,
                                        int peak,
                                        double majorPeakHz,
                                        double magnitude) {
    return String.format(Locale.ROOT,
        "帧(frame)=%6d 平滑(smoothed)=%5.1f 峰值(peak)=%3d 主频(majorHz)=%6.1fHz 幅值(mag)=%6.1f",
        frameCounter,
        smoothedAmp,
        peak,
        majorPeakHz,
        magnitude);
  }

  private void applyTheme(String themeName) {
    String selected = sanitizeTheme(themeName);
    if (!selected.equals(themeBox.getValue())) {
      themeBox.setValue(selected);
    }
    activeTheme = selected;

    rootThemeClassClear();
    if (THEME_ELECTRO_GREEN.equals(selected)) {
      rootPane.getStyleClass().add(STYLE_THEME_ELECTRO);
    } else if (THEME_WARM_AMBER.equals(selected)) {
      rootPane.getStyleClass().add(STYLE_THEME_AMBER);
    } else if (THEME_NOIR_GRID.equals(selected)) {
      rootPane.getStyleClass().add(STYLE_THEME_NOIR);
    } else if (THEME_PAPER_DAYLIGHT.equals(selected)) {
      rootPane.getStyleClass().add(STYLE_THEME_PAPER);
    } else if (THEME_ARCADE_POP.equals(selected)) {
      rootPane.getStyleClass().add(STYLE_THEME_ARCADE);
    } else {
      rootPane.getStyleClass().add(STYLE_THEME_CYBER);
    }

    FX_PREFS.put(PREF_THEME, selected);
    activeSpectrumPalette = SpectrumPalette.forStyle(activeSpectrumStyle, activeTheme);
    drawSpectrumCanvas();
    Platform.runLater(this::applyLogAreaSkinStyles);
  }

  private void rootThemeClassClear() {
    rootPane.getStyleClass().remove(STYLE_THEME_CYBER);
    rootPane.getStyleClass().remove(STYLE_THEME_ELECTRO);
    rootPane.getStyleClass().remove(STYLE_THEME_AMBER);
    rootPane.getStyleClass().remove(STYLE_THEME_NOIR);
    rootPane.getStyleClass().remove(STYLE_THEME_PAPER);
    rootPane.getStyleClass().remove(STYLE_THEME_ARCADE);
  }

  private void applySpectrumStyle(String styleName) {
    String selected = sanitizeSpectrumStyle(styleName);
    if (!selected.equals(spectrumStyleBox.getValue())) {
      spectrumStyleBox.setValue(selected);
    }
    activeSpectrumStyle = selected;

    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_SMOOTH);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_PUNCH);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_NEON);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_CRYSTAL);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_RETRO);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_MECHA);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_PRISM);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_WAVE);
    spectrumFrame.getStyleClass().remove(STYLE_SPECTRUM_FUSION);
    if (SPECTRUM_STYLE_PUNCH.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_PUNCH);
      activeBarGap = 4.0;
    } else if (SPECTRUM_STYLE_NEON.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_NEON);
      activeBarGap = 4.0;
    } else if (SPECTRUM_STYLE_CRYSTAL.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_CRYSTAL);
      activeBarGap = 5.0;
    } else if (SPECTRUM_STYLE_RETRO.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_RETRO);
      activeBarGap = 3.0;
    } else if (SPECTRUM_STYLE_MECHA.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_MECHA);
      activeBarGap = 2.0;
    } else if (SPECTRUM_STYLE_PRISM.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_PRISM);
      activeBarGap = 4.0;
    } else if (SPECTRUM_STYLE_WAVE.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_WAVE);
      activeBarGap = 6.0;
    } else if (SPECTRUM_STYLE_FUSION.equals(selected)) {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_FUSION);
      activeBarGap = 3.0;
    } else {
      spectrumFrame.getStyleClass().add(STYLE_SPECTRUM_SMOOTH);
      activeBarGap = 5.0;
    }
    activeSpectrumPalette = SpectrumPalette.forStyle(selected, activeTheme);
    applySpectrumStyleGeometry(selected);
    drawSpectrumCanvas();

    FX_PREFS.put(PREF_SPECTRUM_STYLE, selected);
  }

  private void applySpectrumStyleGeometry(String selected) {
    activeBarCorner = 5.0;
    activePeakThickness = 2.0;

    if (SPECTRUM_STYLE_PUNCH.equals(selected)) {
      activeBarCorner = 2.0;
      activePeakThickness = 3.0;
    } else if (SPECTRUM_STYLE_NEON.equals(selected)) {
      activeBarCorner = 999.0;
      activePeakThickness = 2.0;
    } else if (SPECTRUM_STYLE_CRYSTAL.equals(selected)) {
      activeBarCorner = 3.0;
      activePeakThickness = 2.0;
    } else if (SPECTRUM_STYLE_RETRO.equals(selected)) {
      activeBarCorner = 1.0;
      activePeakThickness = 4.0;
    } else if (SPECTRUM_STYLE_MECHA.equals(selected)) {
      activeBarCorner = 1.0;
      activePeakThickness = 3.0;
    } else if (SPECTRUM_STYLE_PRISM.equals(selected)) {
      activeBarCorner = 4.0;
      activePeakThickness = 2.0;
    } else if (SPECTRUM_STYLE_WAVE.equals(selected)) {
      activeBarCorner = 999.0;
      activePeakThickness = 2.0;
    } else if (SPECTRUM_STYLE_FUSION.equals(selected)) {
      activeBarCorner = 1.0;
      activePeakThickness = 3.0;
    }
  }

  private void updateSpectrumEnergyClass(double averageEnergy) {
    int nextLevel = 0;
    if (averageEnergy > 0.68) {
      nextLevel = 2;
    } else if (averageEnergy > 0.33) {
      nextLevel = 1;
    }
    if (activeEnergyLevel == nextLevel) {
      return;
    }
    activeEnergyLevel = nextLevel;

    ObservableList<String> styleClasses = spectrumFrame.getStyleClass();
    styleClasses.remove(STYLE_ENERGY_SOFT);
    styleClasses.remove(STYLE_ENERGY_MID);
    styleClasses.remove(STYLE_ENERGY_HARD);
    if (nextLevel == 2) {
      styleClasses.add(STYLE_ENERGY_HARD);
    } else if (nextLevel == 1) {
      styleClasses.add(STYLE_ENERGY_MID);
    } else {
      styleClasses.add(STYLE_ENERGY_SOFT);
    }
  }

  private void drawSpectrumCanvas() {
    if (spectrumGraphics == null || spectrumCanvas == null || smoothedBands == null) {
      return;
    }

    double width = spectrumCanvas.getWidth();
    double height = spectrumCanvas.getHeight();
    if (width <= 4 || height <= 4) {
      return;
    }

    double insetX = 12.0;
    double insetY = 12.0;
    double contentWidth = Math.max(1.0, width - insetX * 2.0);
    double contentHeight = Math.max(6.0, height - insetY * 2.0);
    double drawWidth = contentWidth;
    double drawStartX = insetX;
    double totalGap = activeBarGap * (smoothedBands.length - 1);
    double barWidth = Math.max(2.0, (drawWidth - totalGap) / smoothedBands.length);
    double slotRadius = Math.max(2.0, Math.min(10.0, activeBarCorner + 2.0));
    double barRadius = Math.max(1.0, Math.min(activeBarCorner, barWidth / 2.0));
    double peakHeight = Math.max(1.0, activePeakThickness);
    double effectAmount = currentEffectIntensity();
    double effectStrength = 0.2 + effectAmount * 0.8;
    double barDepthStrength = currentBarDepthStrength() * effectStrength;
    double glowPad = currentGlowPad();
    double glowAlpha = currentGlowAlpha() * effectStrength;
    double shineAlpha = currentTopHighlightAlpha() * effectStrength;
    boolean useScanline = currentUseScanline() && effectAmount > 0.05;
    boolean useSmoothAura = currentUseSmoothAura() && effectAmount > 0.05;
    boolean useNeonCore = currentUseNeonCore() && effectAmount > 0.05;
    boolean useWaveCrest = currentUseWaveCrest() && effectAmount > 0.05;
    boolean useRetroStepShade = currentUseRetroStepShade() && effectAmount > 0.05;
    boolean useMechaTick = currentUseMechaTick() && effectAmount > 0.05;
    boolean usePrismSplit = currentUsePrismSplit() && effectAmount > 0.05;
    boolean useFusionSpark = currentUseFusionSpark() && effectAmount > 0.05;
    boolean useCrystalFacet = currentUseCrystalFacet();
    boolean usePunchCap = currentUsePunchCap();
    boolean useRetroOutline = currentUseRetroOutline();
    boolean useMechaRib = currentUseMechaRib();
    boolean usePrismFacet = currentUsePrismFacet();
    boolean useFusionCore = currentUseFusionCore();
    double stylePhase = currentStylePhase();
    double colorPulse = currentColorPulseAmount() * effectStrength;
    double quantizeStep = currentBarQuantizeStep();

    double visualEnergySum = 0.0;
    spectrumGraphics.clearRect(0, 0, width, height);
    for (int i = 0; i < smoothedBands.length; i++) {
      double x = drawStartX + i * (barWidth + activeBarGap);
      double barHeight = Math.max(2.0, smoothedBands[i] * (contentHeight - 2.0));
      visualEnergySum += smoothedBands[i];
      if (quantizeStep > 0.0) {
        barHeight = Math.max(2.0, Math.round(barHeight / quantizeStep) * quantizeStep);
      }
      double barY = insetY + contentHeight - barHeight;
      Color bandColor = activeSpectrumPalette.bandColor(i, smoothedBands.length);
      if (colorPulse > 0.0) {
        double pulse = 0.5 + 0.5 * Math.sin(stylePhase + i * 0.5);
        bandColor = blendToWhite(bandColor, colorPulse * pulse);
      }

      spectrumGraphics.setFill(activeSpectrumPalette.slotFill);
      spectrumGraphics.fillRoundRect(x, insetY, barWidth, contentHeight, slotRadius, slotRadius);
      spectrumGraphics.setStroke(activeSpectrumPalette.slotBorder);
      spectrumGraphics.setLineWidth(1.0);
      spectrumGraphics.strokeRoundRect(x + 0.5, insetY + 0.5, Math.max(1.0, barWidth - 1.0), Math.max(1.0, contentHeight - 1.0), slotRadius, slotRadius);

      if (glowAlpha > 0.0) {
        spectrumGraphics.setFill(withAlpha(bandColor, glowAlpha));
        spectrumGraphics.fillRoundRect(
            x - glowPad,
            barY - glowPad,
            barWidth + glowPad * 2.0,
            barHeight + glowPad * 2.0,
            barRadius + glowPad,
            barRadius + glowPad
        );
      }

      spectrumGraphics.setFill(bandColor);
      spectrumGraphics.fillRoundRect(x, barY, barWidth, barHeight, barRadius, barRadius);

      if (barDepthStrength > 0.02 && barHeight > 7.0) {
        double bevelWidth = Math.max(1.0, Math.min(3.0, barWidth * 0.18));
        double inset = Math.min(1.0, bevelWidth * 0.4);
        double leftX = x + inset;
        double rightX = x + barWidth - bevelWidth - inset;
        double bodyY = barY + 1.0;
        double bodyHeight = Math.max(2.0, barHeight - 1.5);

        spectrumGraphics.setFill(withAlpha(Color.BLACK, 0.34 * barDepthStrength));
        spectrumGraphics.fillRoundRect(leftX, bodyY, bevelWidth, bodyHeight, 1.0, 1.0);
        spectrumGraphics.setFill(withAlpha(Color.WHITE, 0.42 * barDepthStrength));
        spectrumGraphics.fillRoundRect(rightX, bodyY, bevelWidth, bodyHeight, 1.0, 1.0);

        double capHeight = Math.max(1.0, Math.min(4.2, barHeight * 0.14));
        spectrumGraphics.setFill(withAlpha(Color.WHITE, 0.28 * barDepthStrength));
        spectrumGraphics.fillRoundRect(x + 0.8, barY + 0.8, Math.max(1.0, barWidth - 1.6), capHeight, barRadius, barRadius);
      }

      if (useSmoothAura && barHeight > 8.0) {
        spectrumGraphics.setFill(withAlpha(Color.WHITE, 0.14 * effectStrength));
        spectrumGraphics.fillRoundRect(x + 0.9, barY + 1.0, Math.max(1.0, barWidth - 1.8), Math.max(2.0, barHeight * 0.55), barRadius, barRadius);
      }

      if (useNeonCore && barHeight > 8.0) {
        double coreWidth = Math.max(1.3, barWidth * 0.28);
        double coreX = x + (barWidth - coreWidth) * 0.5;
        spectrumGraphics.setFill(withAlpha(Color.web("#e0f2fe"), 0.44 * effectStrength));
        spectrumGraphics.fillRoundRect(coreX, barY + 1.0, coreWidth, Math.max(2.0, barHeight - 1.8), 999.0, 999.0);
      }

      if (shineAlpha > 0.0) {
        double shineHeight = Math.max(1.0, Math.min(5.0, barHeight * 0.18));
        spectrumGraphics.setFill(withAlpha(Color.WHITE, shineAlpha));
        spectrumGraphics.fillRoundRect(x, barY, barWidth, shineHeight, barRadius, barRadius);
      }

      if (usePunchCap && barHeight > 5.0) {
        spectrumGraphics.setStroke(withAlpha(Color.web("#fef3c7"), 0.72));
        spectrumGraphics.setLineWidth(1.2);
        spectrumGraphics.strokeLine(x + 0.8, barY + 1.2, x + barWidth - 0.8, barY + 1.2);
        spectrumGraphics.setFill(withAlpha(Color.web("#0f172a"), 0.18 * effectStrength));
        spectrumGraphics.fillRect(x + 0.6, barY + barHeight - Math.min(6.0, barHeight * 0.2), Math.max(1.0, barWidth - 1.2), Math.min(6.0, barHeight * 0.2));
        double burstPulse = 0.5 + 0.5 * Math.sin(stylePhase * 2.2 + i * 0.75);
        double burstY = Math.max(insetY + 1.0, barY - (1.0 + burstPulse * 2.8));
        spectrumGraphics.setFill(withAlpha(Color.web("#fde68a"), (0.30 + burstPulse * 0.36) * effectStrength));
        spectrumGraphics.fillRoundRect(x + 1.0, burstY, Math.max(1.2, barWidth - 2.0), 1.6, 1.0, 1.0);
      }

      if (useRetroOutline && barHeight > 5.0) {
        spectrumGraphics.setStroke(withAlpha(Color.web("#f59e0b"), 0.52));
        spectrumGraphics.setLineWidth(1.0);
        spectrumGraphics.strokeRoundRect(x + 0.5, barY + 0.5, Math.max(1.0, barWidth - 1.0), Math.max(1.0, barHeight - 1.0), 1.0, 1.0);
      }

      if (useRetroStepShade && barHeight > 10.0) {
        spectrumGraphics.setFill(withAlpha(Color.web("#0f172a"), 0.18 * effectStrength));
        double stepHeight = Math.max(2.0, barHeight / 5.0);
        double py = barY + stepHeight;
        while (py < barY + barHeight - 1.0) {
          spectrumGraphics.fillRect(x + 1.0, py, Math.max(1.0, barWidth - 2.0), 1.0);
          py += stepHeight;
        }
      }

      if (useMechaRib && barHeight > 8.0) {
        double centerX = x + barWidth * 0.5;
        spectrumGraphics.setStroke(withAlpha(Color.web("#cbd5e1"), 0.30));
        spectrumGraphics.setLineWidth(1.0);
        spectrumGraphics.strokeLine(centerX, barY + 1.0, centerX, barY + barHeight - 1.0);
      }

      if (useMechaTick && barHeight > 12.0) {
        spectrumGraphics.setStroke(withAlpha(Color.web("#94a3b8"), 0.24 * effectStrength));
        spectrumGraphics.setLineWidth(1.0);
        double tickGap = Math.max(4.0, barHeight / 6.0);
        double py = barY + 2.0;
        while (py < barY + barHeight - 1.0) {
          spectrumGraphics.strokeLine(x + 1.0, py, x + Math.max(2.0, barWidth * 0.35), py);
          py += tickGap;
        }
      }

      if (usePrismFacet && barHeight > 8.0) {
        spectrumGraphics.setStroke(withAlpha(Color.web("#e9d5ff"), 0.34));
        spectrumGraphics.setLineWidth(1.0);
        spectrumGraphics.strokeLine(x + 1.0, barY + barHeight * 0.32, x + barWidth - 1.0, barY + barHeight * 0.56);
      }

      if (usePrismSplit && barHeight > 10.0) {
        spectrumGraphics.setStroke(withAlpha(Color.web("#bae6fd"), 0.38 * effectStrength));
        spectrumGraphics.setLineWidth(1.0);
        spectrumGraphics.strokeLine(x + 1.0, barY + barHeight * 0.18, x + barWidth - 1.0, barY + barHeight * 0.36);
        spectrumGraphics.setStroke(withAlpha(Color.web("#fde68a"), 0.34 * effectStrength));
        spectrumGraphics.strokeLine(x + 1.0, barY + barHeight * 0.58, x + barWidth - 1.0, barY + barHeight * 0.76);
      }

      if (useFusionCore && barHeight > 8.0) {
        double pulse = 0.4 + 0.6 * (0.5 + 0.5 * Math.sin(stylePhase + i * 0.65));
        double coreWidth = Math.max(1.4, barWidth * 0.26);
        double coreX = x + (barWidth - coreWidth) * 0.5;
        spectrumGraphics.setFill(withAlpha(Color.web("#fff7ed"), (0.20 + 0.32 * pulse) * effectStrength));
        spectrumGraphics.fillRoundRect(coreX, barY + 1.0, coreWidth, Math.max(2.0, barHeight - 1.5), 1.0, 1.0);
      }

      if (useFusionSpark && barHeight > 10.0) {
        double sparkPulse = 0.5 + 0.5 * Math.sin(stylePhase * 1.8 + i * 0.8);
        double sparkY = Math.max(insetY + 1.0, barY - (0.8 + sparkPulse * 2.0));
        spectrumGraphics.setFill(withAlpha(Color.web("#fed7aa"), (0.26 + sparkPulse * 0.24) * effectStrength));
        spectrumGraphics.fillRoundRect(x + Math.max(1.2, barWidth * 0.28), sparkY, Math.max(1.2, barWidth * 0.44), 1.1, 1.0, 1.0);
      }

      if (useCrystalFacet && barHeight > 8.0) {
        double facetY = barY + Math.max(2.0, barHeight * 0.28);
        double facetEndY = barY + Math.max(3.0, barHeight * 0.72);
        spectrumGraphics.setStroke(withAlpha(Color.web("#dbeafe"), 0.34 * effectStrength));
        spectrumGraphics.setLineWidth(1.0);
        spectrumGraphics.strokeLine(x + 1.0, facetY, x + barWidth - 1.2, facetEndY);
        spectrumGraphics.setStroke(withAlpha(Color.web("#ecfeff"), 0.22 * effectStrength));
        spectrumGraphics.strokeLine(x + barWidth - 1.2, facetY, x + 1.0, facetEndY);
      }

      if (useScanline && barHeight > 6.0) {
        spectrumGraphics.setStroke(withAlpha(Color.WHITE, 0.14 * effectStrength));
        spectrumGraphics.setLineWidth(1.0);
        double y = barY + 2.0;
        while (y < barY + barHeight - 1.5) {
          spectrumGraphics.strokeLine(x + 1.0, y, x + barWidth - 1.0, y);
          y += 4.0;
        }
      }

      if (useWaveCrest && barHeight > 7.0) {
        double crestOffset = 1.6 + Math.sin(stylePhase * 1.25 + i * 0.35) * 1.2;
        double crestY = Math.max(insetY + 1.0, barY + crestOffset);
        spectrumGraphics.setStroke(withAlpha(Color.web("#dbeafe"), 0.54 * effectStrength));
        spectrumGraphics.setLineWidth(1.2);
        spectrumGraphics.strokeLine(x + 0.9, crestY, x + barWidth - 0.9, crestY);
        spectrumGraphics.setStroke(withAlpha(Color.web("#ecfeff"), 0.32 * effectStrength));
        spectrumGraphics.setLineWidth(1.0);
        spectrumGraphics.strokeLine(x + 0.9, crestY + 1.3, x + barWidth - 0.9, crestY + 1.3);
      }

      if (peakBands[i] > 0.03) {
        double peakY = insetY + contentHeight - (peakBands[i] * (contentHeight - peakHeight));
        spectrumGraphics.setFill(activeSpectrumPalette.peakColor);
        spectrumGraphics.fillRoundRect(x, peakY, barWidth, peakHeight, barRadius, barRadius);
      }
    }

    double averageVisualEnergy = visualEnergySum / smoothedBands.length;
    drawStyleOverlay(drawStartX, insetY, drawWidth, contentHeight, stylePhase, effectStrength, averageVisualEnergy);
  }

  private void drawStyleOverlay(double x,
                                double y,
                                double width,
                                double height,
                                double phase,
                                double effectStrength,
                                double averageVisualEnergy) {
    if (averageVisualEnergy < 0.02) {
      return;
    }
    double overlayAlpha = currentSweepOverlayAlpha() * effectStrength;
    overlayAlpha = overlayAlpha * Math.min(1.0, averageVisualEnergy / 0.32);
    if (overlayAlpha <= 0.0) {
      return;
    }

    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      spectrumGraphics.setStroke(withAlpha(Color.WHITE, 0.34 * effectStrength));
      spectrumGraphics.setLineWidth(1.4);
      double baseline = y + height * 0.28;
      double step = Math.max(4.0, width / 64.0);
      double px = x;
      while (px < x + width) {
        double nx = Math.min(x + width, px + step);
        double y0 = baseline + Math.sin((px - x) * 0.045 + phase * 1.4) * 2.2;
        double y1 = baseline + Math.sin((nx - x) * 0.045 + phase * 1.4) * 2.2;
        spectrumGraphics.strokeLine(px, y0, nx, y1);
        px = nx;
      }
    } else if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      double beamPulse = 0.55 + 0.45 * Math.sin(phase * 2.0);
      spectrumGraphics.setStroke(withAlpha(Color.web("#67e8f9"), (0.24 + 0.28 * beamPulse) * effectStrength));
      spectrumGraphics.setLineWidth(1.5);
      double y1 = y + height * 0.22;
      double y2 = y + height * 0.76;
      spectrumGraphics.strokeLine(x + 4.0, y1, x + width - 4.0, y1);
      spectrumGraphics.strokeLine(x + 4.0, y2, x + width - 4.0, y2);
      spectrumGraphics.setStroke(withAlpha(Color.web("#e0f2fe"), (0.18 + 0.20 * beamPulse) * effectStrength));
      spectrumGraphics.setLineWidth(1.0);
      spectrumGraphics.strokeLine(x + 4.0, y + height * 0.50, x + width - 4.0, y + height * 0.50);
    } else if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      spectrumGraphics.setStroke(withAlpha(Color.web("#cbd5e1"), 0.24 * effectStrength));
      spectrumGraphics.setLineWidth(1.0);
      double step = Math.max(28.0, width / 18.0);
      double px = x + step;
      while (px < x + width - 1.0) {
        spectrumGraphics.strokeLine(px, y + 2.0, px, y + height - 2.0);
        px += step;
      }
      spectrumGraphics.setStroke(withAlpha(Color.web("#e2e8f0"), 0.16 * effectStrength));
      spectrumGraphics.strokeLine(x + 2.0, y + height * 0.22, x + width - 2.0, y + height * 0.22);
      spectrumGraphics.strokeLine(x + 2.0, y + height * 0.78, x + width - 2.0, y + height * 0.78);
    } else if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      double centerX = x + width * 0.5;
      spectrumGraphics.setStroke(withAlpha(Color.web("#e9d5ff"), 0.34 * effectStrength));
      spectrumGraphics.setLineWidth(1.2);
      spectrumGraphics.strokeLine(centerX - width * 0.22, y + height * 0.16, centerX + width * 0.22, y + height * 0.84);
      spectrumGraphics.setStroke(withAlpha(Color.web("#bae6fd"), 0.34 * effectStrength));
      spectrumGraphics.strokeLine(centerX - width * 0.22, y + height * 0.84, centerX + width * 0.22, y + height * 0.16);
    } else if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      double emberPulse = 0.55 + 0.45 * Math.sin(phase * 1.5);
      spectrumGraphics.setStroke(withAlpha(Color.web("#fdba74"), (0.16 + emberPulse * 0.14) * effectStrength));
      spectrumGraphics.setLineWidth(1.0);
      double y1 = y + height * (0.30 + 0.04 * Math.sin(phase * 1.3));
      double y2 = y + height * (0.70 + 0.04 * Math.sin(phase * 1.3 + 1.2));
      spectrumGraphics.strokeLine(x + width * 0.28, y1, x + width * 0.72, y1);
      spectrumGraphics.strokeLine(x + width * 0.28, y2, x + width * 0.72, y2);
    } else if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      spectrumGraphics.setStroke(withAlpha(Color.web("#fde68a"), 0.24 * effectStrength));
      spectrumGraphics.setLineWidth(1.0);
      double gridStep = Math.max(9.0, width / 40.0);
      double px = x;
      while (px < x + width) {
        spectrumGraphics.strokeLine(px, y + height * 0.12, px, y + height * 0.88);
        px += gridStep;
      }
    } else if (SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle)) {
      spectrumGraphics.setFill(withAlpha(Color.WHITE, overlayAlpha));
      spectrumGraphics.fillRoundRect(x + width * 0.3, y + 2.0, width * 0.4, Math.max(4.0, height - 4.0), 10.0, 10.0);
    } else if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      spectrumGraphics.setFill(withAlpha(Color.WHITE, overlayAlpha));
      spectrumGraphics.fillRoundRect(x + width * 0.36, y + 2.0, width * 0.28, Math.max(4.0, height - 4.0), 8.0, 8.0);
      spectrumGraphics.setStroke(withAlpha(Color.web("#dbeafe"), 0.28 * effectStrength));
      spectrumGraphics.strokeLine(x + width * 0.36, y + height * 0.18, x + width * 0.64, y + height * 0.82);
    }
  }

  private static final class SpectrumPalette {
    final Color low;
    final Color mid;
    final Color high;
    final Color peakColor;
    final Color slotFill;
    final Color slotBorder;

    SpectrumPalette(Color low, Color mid, Color high, Color peakColor, Color slotFill, Color slotBorder) {
      this.low = low;
      this.mid = mid;
      this.high = high;
      this.peakColor = peakColor;
      this.slotFill = slotFill;
      this.slotBorder = slotBorder;
    }

    Color bandColor(int index, int totalBands) {
      int lowCut = totalBands / 3;
      int midCut = (totalBands * 2) / 3;
      if (index < lowCut) {
        return low;
      }
      if (index < midCut) {
        return mid;
      }
      return high;
    }

    static SpectrumPalette forStyle(String style, String theme) {
      if (THEME_PAPER_DAYLIGHT.equals(theme)) {
        if (SPECTRUM_STYLE_PUNCH.equals(style)) {
          return new SpectrumPalette(Color.web("#4f83d9"), Color.web("#4dc3d5"), Color.web("#8f95d9"), Color.web("#355fc2"), Color.web("#d9e3f0"), Color.web("#94a3b8"));
        }
        if (SPECTRUM_STYLE_NEON.equals(style)) {
          return new SpectrumPalette(Color.web("#59afd8"), Color.web("#58c3b3"), Color.web("#9b8fda"), Color.web("#3a6ecf"), Color.web("#d6e1ee"), Color.web("#94a3b8"));
        }
        if (SPECTRUM_STYLE_CRYSTAL.equals(style)) {
          return new SpectrumPalette(Color.web("#6b9fd8"), Color.web("#76cfd9"), Color.web("#a3aee0"), Color.web("#3a5ea8"), Color.web("#dbe6f3"), Color.web("#94a3b8"));
        }
        if (SPECTRUM_STYLE_RETRO.equals(style)) {
          return new SpectrumPalette(Color.web("#3f70cf"), Color.web("#3fa8bf"), Color.web("#7276cf"), Color.web("#3e5388"), Color.web("#d6e0ed"), Color.web("#64748b"));
        }
        if (SPECTRUM_STYLE_MECHA.equals(style)) {
          return new SpectrumPalette(Color.web("#3a89b8"), Color.web("#49a9a0"), Color.web("#7276cf"), Color.web("#3763c2"), Color.web("#d3deeb"), Color.web("#64748b"));
        }
        if (SPECTRUM_STYLE_PRISM.equals(style)) {
          return new SpectrumPalette(Color.web("#59afd8"), Color.web("#4dc3d5"), Color.web("#8f95d9"), Color.web("#355fc2"), Color.web("#d9e4f2"), Color.web("#94a3b8"));
        }
        if (SPECTRUM_STYLE_WAVE.equals(style)) {
          return new SpectrumPalette(Color.web("#6b9fd8"), Color.web("#58c3b3"), Color.web("#9b8fda"), Color.web("#355fc2"), Color.web("#d6e2f0"), Color.web("#94a3b8"));
        }
        if (SPECTRUM_STYLE_FUSION.equals(style)) {
          return new SpectrumPalette(Color.web("#4b99c9"), Color.web("#3fa8bf"), Color.web("#7276cf"), Color.web("#3e5388"), Color.web("#d3deeb"), Color.web("#64748b"));
        }
        return new SpectrumPalette(Color.web("#59afd8"), Color.web("#4dc3d5"), Color.web("#8f95d9"), Color.web("#355fc2"), Color.web("#d9e4f2"), Color.web("#94a3b8"));
      }

      if (SPECTRUM_STYLE_PUNCH.equals(style)) {
        return new SpectrumPalette(Color.web("#22d3ee"), Color.web("#a3e635"), Color.web("#f59e0b"), Color.web("#fff7ed"), Color.web("#111827"), Color.web("#475569"));
      }
      if (SPECTRUM_STYLE_NEON.equals(style)) {
        return new SpectrumPalette(Color.web("#22d3ee"), Color.web("#a3e635"), Color.web("#fb923c"), Color.web("#e2f7ff"), Color.web("#0f172a"), Color.web("#334155"));
      }
      if (SPECTRUM_STYLE_CRYSTAL.equals(style)) {
        return new SpectrumPalette(Color.web("#7dd3fc"), Color.web("#a5f3fc"), Color.web("#c4b5fd"), Color.web("#f8fafc"), Color.web("#0f172a"), Color.web("#94a3b8"));
      }
      if (SPECTRUM_STYLE_RETRO.equals(style)) {
        return new SpectrumPalette(Color.web("#2563eb"), Color.web("#84cc16"), Color.web("#f59e0b"), Color.web("#fde68a"), Color.web("#111827"), Color.web("#b45309"));
      }
      if (SPECTRUM_STYLE_MECHA.equals(style)) {
        return new SpectrumPalette(Color.web("#38bdf8"), Color.web("#22c55e"), Color.web("#f97316"), Color.web("#fff7ed"), Color.web("#0b1220"), Color.web("#64748b"));
      }
      if (SPECTRUM_STYLE_PRISM.equals(style)) {
        return new SpectrumPalette(Color.web("#38bdf8"), Color.web("#a855f7"), Color.web("#f59e0b"), Color.web("#fdf4ff"), Color.web("#111827"), Color.web("#7c3aed"));
      }
      if (SPECTRUM_STYLE_WAVE.equals(style)) {
        return new SpectrumPalette(Color.web("#38bdf8"), Color.web("#2dd4bf"), Color.web("#fb923c"), Color.web("#dbeafe"), Color.web("#1e293b"), Color.web("#334155"));
      }
      if (SPECTRUM_STYLE_FUSION.equals(style)) {
        return new SpectrumPalette(Color.web("#06b6d4"), Color.web("#84cc16"), Color.web("#f97316"), Color.web("#fff7ed"), Color.web("#1f2937"), Color.web("#475569"));
      }
      return new SpectrumPalette(Color.web("#22d3ee"), Color.web("#a3e635"), Color.web("#fb923c"), Color.web("#fef3c7"), Color.web("#0f172a"), Color.web("#334155"));
    }
  }

  private void resetSpectrumView() {
    for (int i = 0; i < smoothedBands.length; i++) {
      smoothedBands[i] = 0.0;
      peakBands[i] = 0.0;
      peakHoldLeft[i] = 0;
    }
    activeEnergyLevel = -1;
    updateSpectrumEnergyClass(0.0);
    drawSpectrumCanvas();
    statsLabel.setText("状态 (Status)：空闲 (Idle)");
  }

  private void persistWindowSize() {
    if (stage == null) {
      return;
    }
    FX_PREFS.putDouble(PREF_WINDOW_WIDTH, clampWindowSize(stage.getWidth(), 840.0, 1960.0));
    FX_PREFS.putDouble(PREF_WINDOW_HEIGHT, clampWindowSize(stage.getHeight(), 520.0, 1280.0));
  }

  private static String sanitizeTheme(String themeName) {
    if (THEME_ELECTRO_GREEN.equals(themeName)) {
      return THEME_ELECTRO_GREEN;
    }
    if (THEME_WARM_AMBER.equals(themeName)) {
      return THEME_WARM_AMBER;
    }
    if (THEME_NOIR_GRID.equals(themeName)) {
      return THEME_NOIR_GRID;
    }
    if (THEME_PAPER_DAYLIGHT.equals(themeName)) {
      return THEME_PAPER_DAYLIGHT;
    }
    if (THEME_ARCADE_POP.equals(themeName)) {
      return THEME_ARCADE_POP;
    }
    return THEME_CYBER_BLUE;
  }

  private static String sanitizeSpectrumStyle(String styleName) {
    if (SPECTRUM_STYLE_PUNCH.equals(styleName)) {
      return SPECTRUM_STYLE_PUNCH;
    }
    if (SPECTRUM_STYLE_NEON.equals(styleName)) {
      return SPECTRUM_STYLE_NEON;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(styleName)) {
      return SPECTRUM_STYLE_CRYSTAL;
    }
    if (SPECTRUM_STYLE_RETRO.equals(styleName)) {
      return SPECTRUM_STYLE_RETRO;
    }
    if (SPECTRUM_STYLE_MECHA.equals(styleName)) {
      return SPECTRUM_STYLE_MECHA;
    }
    if (SPECTRUM_STYLE_PRISM.equals(styleName)) {
      return SPECTRUM_STYLE_PRISM;
    }
    if (SPECTRUM_STYLE_WAVE.equals(styleName)) {
      return SPECTRUM_STYLE_WAVE;
    }
    if (SPECTRUM_STYLE_FUSION.equals(styleName)) {
      return SPECTRUM_STYLE_FUSION;
    }
    return SPECTRUM_STYLE_SMOOTH;
  }

  private static int sampleSpectrumBand(byte[] source, int displayIndex, int displayBands) {
    if (source.length == 0) {
      return 0;
    }
    if (source.length == 1 || displayBands <= 1) {
      return source[0] & 0xFF;
    }

    double sourceMax = source.length - 1.0;
    double pos = (displayIndex / (double) (displayBands - 1)) * sourceMax;
    int i0 = (int) Math.floor(pos);
    int i1 = Math.min(source.length - 1, i0 + 1);
    double t = pos - i0;
    double v0 = source[i0] & 0xFF;
    double v1 = source[i1] & 0xFF;
    return (int) Math.round(v0 * (1.0 - t) + v1 * t);
  }

  private static void installBoundsClip(Region region) {
    if (region == null) {
      return;
    }
    Rectangle clip = new Rectangle();
    clip.widthProperty().bind(region.widthProperty());
    clip.heightProperty().bind(region.heightProperty());
    region.setClip(clip);
  }

  private double currentSpectrumGamma() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 1.04;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.90;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.86;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.98;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 1.10;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.92;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.80;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 1.16;
    }
    return SPECTRUM_GAMMA;
  }

  private double currentRiseLerp() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.78;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.66;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.62;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.56;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.82;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.60;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.52;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.86;
    }
    return 0.50;
  }

  private double currentBarDecayStep() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.09;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.06;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.05;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.085;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.11;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.055;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.045;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.13;
    }
    return BAR_DECAY_STEP;
  }

  private double currentPeakDecayStep() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.034;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.022;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.018;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.03;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.038;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.020;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.016;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.040;
    }
    return PEAK_DECAY_STEP;
  }

  private int currentPeakHoldTicks() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 3;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 8;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 9;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 5;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 2;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 8;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 10;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 2;
    }
    return PEAK_HOLD_TICKS;
  }

  private double currentGlowAlpha() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.22;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.46;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.28;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.34;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.24;
    }
    if (SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle)) {
      return 0.12;
    }
    return 0.0;
  }

  private double currentGlowPad() {
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle) || SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle) || SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 2.6;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle) || SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 1.8;
    }
    if (SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle)) {
      return 1.2;
    }
    return 0.0;
  }

  private double currentTopHighlightAlpha() {
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.56;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle) || SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.36;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.28;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.24;
    }
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.22;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.10;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.12;
    }
    return 0.0;
  }

  private double currentBarDepthStrength() {
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.38;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.36;
    }
    if (SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle)) {
      return 0.30;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.24;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.28;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.22;
    }
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.18;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.16;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.12;
    }
    return 0.24;
  }

  private double currentBarQuantizeStep() {
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 6.0;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 4.0;
    }
    return 0.0;
  }

  private double currentColorPulseAmount() {
    if (SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle)) {
      return 0.12;
    }
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.26;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.42;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.36;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.30;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.24;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.36;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.06;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.05;
    }
    return 0.0;
  }

  private double currentSweepOverlayAlpha() {
    if (SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle)) {
      return 0.08;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.24;
    }
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.06;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.12;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.10;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.08;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.07;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.10;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.06;
    }
    return 0.0;
  }

  private static double currentStylePhase() {
    return (System.nanoTime() * 0.000000001) * (Math.PI * 2.0);
  }

  private double currentEffectIntensity() {
    return clampWindowSize(effectIntensity, 0.0, 1.0);
  }

  private void updateEffectIntensityLabel() {
    if (effectIntensityValueLabel != null) {
      effectIntensityValueLabel.setText(String.format(Locale.ROOT, "%d%%", Math.round(currentEffectIntensity() * 100.0)));
    }
  }

  private boolean currentUseCrystalFacet() {
    return SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle);
  }

  private boolean currentUseSmoothAura() {
    return SPECTRUM_STYLE_SMOOTH.equals(activeSpectrumStyle);
  }

  private boolean currentUseNeonCore() {
    return SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle);
  }

  private boolean currentUsePunchCap() {
    return SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle);
  }

  private boolean currentUseRetroOutline() {
    return SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle);
  }

  private boolean currentUseRetroStepShade() {
    return SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle);
  }

  private boolean currentUseMechaRib() {
    return SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle);
  }

  private boolean currentUseMechaTick() {
    return SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle);
  }

  private boolean currentUsePrismFacet() {
    return SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle);
  }

  private boolean currentUsePrismSplit() {
    return SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle);
  }

  private boolean currentUseWaveCrest() {
    return SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle);
  }

  private boolean currentUseFusionCore() {
    return SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle);
  }

  private boolean currentUseFusionSpark() {
    return SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle);
  }

  private static Color blendToWhite(Color color, double amount) {
    double t = Math.max(0.0, Math.min(1.0, amount));
    return new Color(
        color.getRed() + (1.0 - color.getRed()) * t,
        color.getGreen() + (1.0 - color.getGreen()) * t,
        color.getBlue() + (1.0 - color.getBlue()) * t,
        color.getOpacity()
    );
  }

  private boolean currentUseScanline() {
    return SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle) || SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle);
  }

  private static Color withAlpha(Color color, double alpha) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0.0, Math.min(1.0, alpha)));
  }

  private double currentNoiseFloor() {
    if (SPECTRUM_STYLE_PUNCH.equals(activeSpectrumStyle)) {
      return 0.10;
    }
    if (SPECTRUM_STYLE_NEON.equals(activeSpectrumStyle)) {
      return 0.05;
    }
    if (SPECTRUM_STYLE_CRYSTAL.equals(activeSpectrumStyle)) {
      return 0.04;
    }
    if (SPECTRUM_STYLE_RETRO.equals(activeSpectrumStyle)) {
      return 0.09;
    }
    if (SPECTRUM_STYLE_MECHA.equals(activeSpectrumStyle)) {
      return 0.12;
    }
    if (SPECTRUM_STYLE_PRISM.equals(activeSpectrumStyle)) {
      return 0.05;
    }
    if (SPECTRUM_STYLE_WAVE.equals(activeSpectrumStyle)) {
      return 0.03;
    }
    if (SPECTRUM_STYLE_FUSION.equals(activeSpectrumStyle)) {
      return 0.14;
    }
    return 0.07;
  }

  private static double clampWindowSize(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
