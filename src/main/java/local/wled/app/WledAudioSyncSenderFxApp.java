package local.wled.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class WledAudioSyncSenderFxApp extends Application {
  private static Args bootstrapArgs;

  private WledAudioSyncSenderFxController controller;

  static void bootstrap(Args args) {
    bootstrapArgs = args;
  }

  @Override
  public void start(Stage stage) {
    Args args = bootstrapArgs;
    if (args == null) {
      args = Args.parse(new String[0]);
    }

    FXMLLoader loader = new FXMLLoader(WledAudioSyncSenderFxApp.class.getResource("/wled-audio-sync.fxml"));
    Parent root;
    try {
      root = loader.load();
    } catch (IOException e) {
      throw new IllegalStateException("无法加载 JavaFX 布局文件: wled-audio-sync.fxml", e);
    }

    controller = loader.getController();
    controller.setup(args, stage);

    Scene scene = new Scene(root, controller.getInitialWindowWidth(), controller.getInitialWindowHeight());
    scene.getStylesheets().add(requireResource("/wled-audio-sync.css"));

    AppIconSupport.apply(stage, WledAudioSyncSender.APP_NAME);
    stage.setScene(scene);
    stage.show();
  }

  @Override
  public void stop() {
    if (controller != null) {
      controller.shutdown();
    }
  }

  private static String requireResource(String path) {
    java.net.URL url = WledAudioSyncSenderFxApp.class.getResource(path);
    if (url == null) {
      throw new IllegalStateException("缺少资源文件: " + path);
    }
    return url.toExternalForm();
  }
}
