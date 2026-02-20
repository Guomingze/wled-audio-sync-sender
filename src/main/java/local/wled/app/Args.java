package local.wled.app;

import local.wled.core.OutputMode;
import local.wled.core.DdpLayoutMode;
import local.wled.core.DdpColorPalette;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Args {
  static final String DEFAULT_DEST = "192.168.31.57";
  static final int DEFAULT_PORT = 11988;
  static final int DEFAULT_DDP_PORT = 4048;
  static final int DEFAULT_DDP_PIXELS = 90;
  static final DdpLayoutMode DEFAULT_DDP_LAYOUT = DdpLayoutMode.REPEAT;
  static final DdpColorPalette DEFAULT_DDP_PALETTE = DdpColorPalette.AURORA;
  static final int DEFAULT_SAMPLE_RATE = 44100;
  static final int DEFAULT_CHANNELS = 2;
  static final int DEFAULT_FFT_SIZE = 1024;
  static final int DEFAULT_FPS = 50;
  static final String DEFAULT_INPUT = "BlackHole";

  final boolean showHelp;
  final boolean listDevices;
  final boolean verbose;
  final String dest;
  final int port;
  final String inputDeviceQuery;
  final int sampleRate;
  final int channels;
  final int fftSize;
  final int fps;
  final OutputMode outputMode;
  final int ddpPixelCount;
  final DdpLayoutMode ddpLayoutMode;
  final DdpColorPalette ddpColorPalette;
  final String[] rawArgs;

  private Args(
      boolean showHelp,
      boolean listDevices,
      boolean verbose,
      String dest,
      int port,
      String inputDeviceQuery,
      int sampleRate,
      int channels,
      int fftSize,
      int fps,
      OutputMode outputMode,
      int ddpPixelCount,
      DdpLayoutMode ddpLayoutMode,
      DdpColorPalette ddpColorPalette,
      String[] rawArgs
  ) {
    this.showHelp = showHelp;
    this.listDevices = listDevices;
    this.verbose = verbose;
    this.dest = dest;
    this.port = port;
    this.inputDeviceQuery = inputDeviceQuery;
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.fftSize = fftSize;
    this.fps = fps;
    this.outputMode = outputMode;
    this.ddpPixelCount = ddpPixelCount;
    this.ddpLayoutMode = ddpLayoutMode;
    this.ddpColorPalette = ddpColorPalette;
    this.rawArgs = rawArgs;
  }

  static Args parse(String[] argv) {
    Map<String, String> kv = new HashMap<>();
    Set<String> flags = new HashSet<>();

    for (int i = 0; i < argv.length; i++) {
      String a = argv[i];
      if (a.equals("-h") || a.equals("--help")) {
        flags.add("help");
        continue;
      }
      if (a.equals("--list-devices")) {
        flags.add("list");
        continue;
      }
      if (a.equals("--verbose")) {
        flags.add("verbose");
        continue;
      }
      if (a.startsWith("--")) {
        String key = a.substring(2);
        String val = null;
        if (i + 1 < argv.length && !argv[i + 1].startsWith("--")) {
          val = argv[++i];
        }
        kv.put(key, val);
      }
    }

    OutputMode mode = OutputMode.fromCliValue(getOr(kv, "mode", OutputMode.AUDIO_SYNC_V2.cliValue()));
    String dest = getOr(kv, "dest", DEFAULT_DEST);
    int defaultPort = mode == OutputMode.DDP ? DEFAULT_DDP_PORT : DEFAULT_PORT;
    int port = parseInt(getOr(kv, "port", String.valueOf(defaultPort)), defaultPort);
    String input = getOr(kv, "input", DEFAULT_INPUT);
    int sampleRate = parseInt(getOr(kv, "rate", String.valueOf(DEFAULT_SAMPLE_RATE)), DEFAULT_SAMPLE_RATE);
    int channels = parseInt(getOr(kv, "channels", String.valueOf(DEFAULT_CHANNELS)), DEFAULT_CHANNELS);
    int fftSize = parseInt(getOr(kv, "fft", String.valueOf(DEFAULT_FFT_SIZE)), DEFAULT_FFT_SIZE);
    int fps = parseInt(getOr(kv, "fps", String.valueOf(DEFAULT_FPS)), DEFAULT_FPS);
    int ddpPixelCount = parseInt(getOr(kv, "ddp-pixels", String.valueOf(DEFAULT_DDP_PIXELS)), DEFAULT_DDP_PIXELS);
    DdpLayoutMode ddpLayoutMode = DdpLayoutMode.fromCliValue(getOr(kv, "ddp-layout", DEFAULT_DDP_LAYOUT.cliValue()));
    DdpColorPalette ddpColorPalette = DdpColorPalette.fromCliValue(getOr(kv, "ddp-palette", DEFAULT_DDP_PALETTE.cliValue()));
    if (kv.containsKey("ui")) {
      throw new IllegalArgumentException("--ui has been removed; JavaFX GUI is now the default.");
    }

    if ((fftSize & (fftSize - 1)) != 0) {
      throw new IllegalArgumentException("--fft must be a power of two (e.g. 512, 1024, 2048)");
    }
    if (channels < 1 || channels > 2) {
      throw new IllegalArgumentException("--channels must be 1 or 2");
    }
    if (ddpPixelCount < 1 || ddpPixelCount > 4096) {
      throw new IllegalArgumentException("--ddp-pixels must be between 1 and 4096");
    }

    return new Args(
        flags.contains("help"),
        flags.contains("list"),
        flags.contains("verbose"),
        dest,
        port,
        input,
        sampleRate,
        channels,
        fftSize,
        fps,
        mode,
        ddpPixelCount,
        ddpLayoutMode,
        ddpColorPalette,
        argv
    );
  }

  static void printHelp() {
    System.out.println("WledAudioSyncSender - 将 mac 音频通过 UDP 喂给 WLED 音频反应效果\n");
    System.out.println("用法:");
    System.out.println("  # JavaFX GUI");
    System.out.println("  java -cp . WledAudioSyncSender");
    System.out.println();
    System.out.println("参数:");
    System.out.println("  --list-devices            列出音频输入设备");
    System.out.println("  --input <关键字>          按名称/描述匹配选择输入设备 (默认: " + DEFAULT_INPUT + ")");
    System.out.println("  --mode <audio-sync|ddp>   推送模式 (默认: audio-sync)");
    System.out.println("  --dest <ip>               目标 IP (默认: 239.0.0.1 组播)");
    System.out.println("  --port <n>                目标 UDP 端口 (audio-sync 默认 11988, ddp 默认 4048)");
    System.out.println("  --ddp-pixels <n>          DDP 模式像素数 (默认: " + DEFAULT_DDP_PIXELS + ")");
    System.out.println("  --ddp-layout <stretch|repeat|mirror>  DDP 灯带布局 (默认: " + DEFAULT_DDP_LAYOUT.cliValue() + ")");
    System.out.println("  --ddp-palette <aurora|sunset|fire|ocean|candy>  DDP 配色 (默认: " + DEFAULT_DDP_PALETTE.cliValue() + ")");
    System.out.println("  --rate <hz>               采样率 (默认: 44100)");
    System.out.println("  --channels <1|2>          声道数 (默认: 2)");
    System.out.println("  --fft <n>                 FFT 点数, 2 的幂 (默认: 1024)");
    System.out.println("  --fps <n>                 发送帧率 (默认: 50)");
    System.out.println("  --verbose                 每约 10 帧打印一次调试信息\n");
  }

  private static String getOr(Map<String, String> kv, String key, String def) {
    String v = kv.get(key);
    return (v == null || v.isEmpty()) ? def : v;
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
}
