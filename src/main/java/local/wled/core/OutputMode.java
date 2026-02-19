package local.wled.core;

public enum OutputMode {
  AUDIO_SYNC_V2("audio-sync", "音频同步 (Audio Sync v2)", 11988),
  DDP("ddp", "像素推送 (DDP)", 4048);

  private final String cliValue;
  private final String uiLabel;
  private final int defaultPort;

  OutputMode(String cliValue, String uiLabel, int defaultPort) {
    this.cliValue = cliValue;
    this.uiLabel = uiLabel;
    this.defaultPort = defaultPort;
  }

  public String cliValue() {
    return cliValue;
  }

  public String uiLabel() {
    return uiLabel;
  }

  public int defaultPort() {
    return defaultPort;
  }

  public static OutputMode fromCliValue(String value) {
    if (value == null) {
      return AUDIO_SYNC_V2;
    }
    String normalized = value.trim().toLowerCase();
    for (OutputMode mode : values()) {
      if (mode.cliValue.equals(normalized)) {
        return mode;
      }
    }
    throw new IllegalArgumentException("--mode only supports: audio-sync, ddp");
  }

  public static OutputMode fromUiLabel(String label) {
    if (label == null) {
      return AUDIO_SYNC_V2;
    }
    for (OutputMode mode : values()) {
      if (mode.uiLabel.equals(label)) {
        return mode;
      }
    }
    return AUDIO_SYNC_V2;
  }
}
