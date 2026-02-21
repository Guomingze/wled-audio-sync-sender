package local.wled.app;

import java.util.logging.Level;
import java.util.logging.Logger;

final class AppIconSupport {
  private static final Logger LOG = Logger.getLogger(AppIconSupport.class.getName());

  private AppIconSupport() {
  }

  static void apply(String appName) {
    applyApplicationName(appName);
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
}
