package local.wled.app;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

final class AppIconSupport {
  private static final Logger LOG = Logger.getLogger(AppIconSupport.class.getName());

  private AppIconSupport() {
  }

  static void apply(Stage stage, String appName) {
    applyApplicationName(appName);
    Image icon = createFxIcon(128.0);
    stage.getIcons().setAll(icon);
    applyDockIcon();
  }

  private static void applyApplicationName(String appName) {
    try {
      Class<?> glassApplication = Class.forName("com.sun.glass.ui.Application");
      Object app = glassApplication.getMethod("GetApplication").invoke(null);
      if (app != null) {
        glassApplication.getMethod("setName", String.class).invoke(app, appName);
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      LOG.log(Level.FINE, "Unable to set JavaFX application name via Glass API", e);
    }
  }

  private static Image createFxIcon(double size) {
    Canvas canvas = new Canvas(size, size);
    GraphicsContext gc = canvas.getGraphicsContext2D();
    double pad = size * 0.1;
    double body = size - (pad * 2.0);

    gc.clearRect(0.0, 0.0, size, size);
    gc.setFill(new LinearGradient(
        0.0, 0.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.web("#0b1f25")),
        new Stop(1.0, Color.web("#0a1512"))
    ));
    gc.fillRoundRect(pad, pad, body, body, size * 0.24, size * 0.24);

    gc.setStroke(Color.web("#39d39b"));
    gc.setLineWidth(Math.max(2.0, size * 0.03));
    gc.strokeRoundRect(pad, pad, body, body, size * 0.24, size * 0.24);

    gc.setFill(new LinearGradient(
        0.0, 1.0, 0.0, 0.0, true, CycleMethod.NO_CYCLE,
        new Stop(0.0, Color.web("#22d3ee")),
        new Stop(0.6, Color.web("#34d399")),
        new Stop(1.0, Color.web("#bef264"))
    ));

    double barWidth = size * 0.11;
    double gap = size * 0.07;
    double startX = size * 0.24;
    double baseY = size * 0.75;
    double[] heights = new double[]{0.24, 0.40, 0.58, 0.34};
    for (int i = 0; i < heights.length; i++) {
      double h = size * heights[i];
      double x = startX + i * (barWidth + gap);
      double y = baseY - h;
      gc.fillRoundRect(x, y, barWidth, h, size * 0.06, size * 0.06);
    }

    SnapshotParameters params = new SnapshotParameters();
    params.setFill(Color.TRANSPARENT);
    WritableImage image = new WritableImage((int) Math.round(size), (int) Math.round(size));
    canvas.snapshot(params, image);
    return image;
  }

  private static void applyDockIcon() {
    try {
      if (!Taskbar.isTaskbarSupported()) {
        return;
      }
      Taskbar taskbar = Taskbar.getTaskbar();
      if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        return;
      }
      taskbar.setIconImage(createAwtIcon(256));
    } catch (RuntimeException e) {
      LOG.log(Level.FINE, "Unable to apply dock icon via AWT Taskbar API", e);
    }
  }

  private static java.awt.Image createAwtIcon(int size) {
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    int pad = (int) Math.round(size * 0.1);
    int body = size - (pad * 2);
    float arc = (float) (size * 0.24);

    RoundRectangle2D.Double outer = new RoundRectangle2D.Double(pad, pad, body, body, arc, arc);
    g.setPaint(new GradientPaint(0.0f, 0.0f, new java.awt.Color(11, 31, 37), size, size, new java.awt.Color(10, 21, 18)));
    g.fill(outer);

    g.setPaint(new java.awt.Color(57, 211, 155));
    g.setStroke(new BasicStroke(Math.max(3.0f, (float) (size * 0.03)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.draw(outer);

    g.setPaint(new GradientPaint(0.0f, size, new java.awt.Color(34, 211, 238), 0.0f, 0.0f, new java.awt.Color(190, 242, 100)));
    double[] heights = new double[]{0.24, 0.40, 0.58, 0.34};
    int barWidth = (int) Math.round(size * 0.11);
    int gap = (int) Math.round(size * 0.07);
    int startX = (int) Math.round(size * 0.24);
    int baseY = (int) Math.round(size * 0.75);
    int barArc = (int) Math.round(size * 0.06);
    for (int i = 0; i < heights.length; i++) {
      int h = (int) Math.round(size * heights[i]);
      int x = startX + i * (barWidth + gap);
      int y = baseY - h;
      g.fillRoundRect(x, y, barWidth, h, barArc, barArc);
    }

    g.dispose();
    return image;
  }
}
