package local.wled.core;

public enum DdpColorPalette {
  AURORA("aurora", "极光 (Aurora)"),
  SUNSET("sunset", "落日 (Sunset)"),
  FIRE("fire", "烈焰 (Fire)"),
  OCEAN("ocean", "海洋 (Ocean)"),
  CANDY("candy", "糖果 (Candy)");

  private final String cliValue;
  private final String uiLabel;

  DdpColorPalette(String cliValue, String uiLabel) {
    this.cliValue = cliValue;
    this.uiLabel = uiLabel;
  }

  public String cliValue() {
    return cliValue;
  }

  public String uiLabel() {
    return uiLabel;
  }

  public static DdpColorPalette fromCliValue(String value) {
    if (value == null) {
      return AURORA;
    }
    String normalized = value.trim().toLowerCase();
    for (DdpColorPalette palette : values()) {
      if (palette.cliValue.equals(normalized)) {
        return palette;
      }
    }
    throw new IllegalArgumentException("--ddp-palette only supports: aurora, sunset, fire, ocean, candy");
  }

  public static DdpColorPalette fromUiLabel(String label) {
    if (label == null) {
      return AURORA;
    }
    for (DdpColorPalette palette : values()) {
      if (palette.uiLabel.equals(label)) {
        return palette;
      }
    }
    return AURORA;
  }
}
