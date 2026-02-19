package local.wled.core;

public final class SenderConfig {
  final String dest;
  final int port;
  final String inputDeviceQuery;
  final int sampleRate;
  final int channels;
  final int fftSize;
  final int fps;
  final boolean verbose;
  final OutputMode outputMode;
  final int ddpPixelCount;
  final DdpLayoutMode ddpLayoutMode;
  final DdpColorPalette ddpColorPalette;
  final boolean captureEnabled;
  final boolean pushEnabled;

  public SenderConfig(String dest,
                      int port,
                      String inputDeviceQuery,
                      int sampleRate,
                      int channels,
                      int fftSize,
                      int fps,
                      boolean verbose,
                      OutputMode outputMode,
                      int ddpPixelCount,
                      DdpLayoutMode ddpLayoutMode,
                      DdpColorPalette ddpColorPalette,
                      boolean captureEnabled,
                      boolean pushEnabled) {
    this.dest = dest;
    this.port = port;
    this.inputDeviceQuery = inputDeviceQuery;
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.fftSize = fftSize;
    this.fps = fps;
    this.verbose = verbose;
    this.outputMode = outputMode;
    this.ddpPixelCount = ddpPixelCount;
    this.ddpLayoutMode = ddpLayoutMode;
    this.ddpColorPalette = ddpColorPalette;
    this.captureEnabled = captureEnabled;
    this.pushEnabled = pushEnabled;
  }
}
