package local.wled.core;

public enum DdpLayoutMode {
  STRETCH("stretch", "拉伸渐变 (Stretch)"),
  REPEAT("repeat", "重复频段 (Repeat)"),
  MIRROR("mirror", "镜像律动 (Mirror)");

  private final String cliValue;
  private final String uiLabel;

  DdpLayoutMode(String cliValue, String uiLabel) {
    this.cliValue = cliValue;
    this.uiLabel = uiLabel;
  }

  public String cliValue() {
    return cliValue;
  }

  public String uiLabel() {
    return uiLabel;
  }

  public static DdpLayoutMode fromCliValue(String value) {
    if (value == null) {
      return REPEAT;
    }
    String normalized = value.trim().toLowerCase();
    for (DdpLayoutMode mode : values()) {
      if (mode.cliValue.equals(normalized)) {
        return mode;
      }
    }
    throw new IllegalArgumentException("--ddp-layout only supports: stretch, repeat, mirror");
  }

  public static DdpLayoutMode fromUiLabel(String label) {
    if (label == null) {
      return REPEAT;
    }
    for (DdpLayoutMode mode : values()) {
      if (mode.uiLabel.equals(label)) {
        return mode;
      }
    }
    return REPEAT;
  }
}
