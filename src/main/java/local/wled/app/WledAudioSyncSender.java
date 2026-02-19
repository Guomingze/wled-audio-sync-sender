package local.wled.app;

import javafx.application.Application;

import local.wled.core.AudioDeviceCatalog;

public final class WledAudioSyncSender {
  static final String APP_NAME = "WLED 音频同步发送器";

  private WledAudioSyncSender() {
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("apple.awt.application.name", APP_NAME);
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);

    Args parsed;
    try {
      parsed = Args.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.err.println("使用 --help 查看参数。\n");
      return;
    }

    if (parsed.showHelp) {
      Args.printHelp();
      return;
    }

    if (parsed.listDevices) {
      listCaptureDevicesCli();
      return;
    }

    WledAudioSyncSenderFxApp.bootstrap(parsed);
    Application.launch(WledAudioSyncSenderFxApp.class, parsed.rawArgs);
  }

  static void listCaptureDevicesCli() {
    System.out.println("可用音频输入设备 (TargetDataLine)：");
    for (String name : AudioDeviceCatalog.listCaptureDeviceNames()) {
      System.out.println("- " + name);
    }
    System.out.println("\n提示：一般用 --input \"BlackHole\" 来选择 BlackHole 2ch。\n");
  }
}
