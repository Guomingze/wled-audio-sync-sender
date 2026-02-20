package local.wled.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class WledAudioSyncSenderFxApp extends Application {
  private WledAudioSyncSenderFxController controller;

  @Override
  public void start(Stage stage) {
    Args args = resolveLaunchArgs();

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

  private Args resolveLaunchArgs() {
    List<String> raw = getParameters() == null ? Collections.emptyList() : getParameters().getRaw();
    return Args.parse(raw.toArray(new String[0]));
  }
}
