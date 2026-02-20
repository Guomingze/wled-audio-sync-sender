package local.wled.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AudioDeviceCatalog {
  private static final int OPEN_BUFFER_FRAMES = 1024;

  private AudioDeviceCatalog() {
  }

  public static List<String> listCaptureDeviceNames() {
    List<String> out = new ArrayList<>();
    Mixer.Info[] infos = AudioSystem.getMixerInfo();
    for (Mixer.Info mi : infos) {
      Mixer m = AudioSystem.getMixer(mi);
      boolean hasTarget = false;
      for (Line.Info li : m.getTargetLineInfo()) {
        if (TargetDataLine.class.isAssignableFrom(li.getLineClass())) {
          hasTarget = true;
          break;
        }
      }
      if (!hasTarget) {
        continue;
      }
      out.add(mi.getName() + " :: " + mi.getDescription());
    }
    return out;
  }

  static InputSelection openTargetLine(AudioFormat format, String query) {
    List<Mixer.Info> matches = new ArrayList<>();
    for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
      Mixer m = AudioSystem.getMixer(mi);
      if (!supportsTargetDataLine(m, format)) {
        continue;
      }
      if (matchesQuery(mi, query)) {
        matches.add(mi);
      }
    }

    if (matches.isEmpty()) {
      return null;
    }

    Mixer.Info picked = matches.get(0);
    Mixer m = AudioSystem.getMixer(picked);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    try {
      TargetDataLine line = (TargetDataLine) m.getLine(info);
      int bufferSize = format.getFrameSize() * OPEN_BUFFER_FRAMES * 2;
      line.open(format, bufferSize);
      return new InputSelection(line, picked.getName(), picked.getDescription());
    } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
      throw new IllegalStateException("无法打开输入设备: " + picked.getName(), e);
    }
  }

  private static boolean matchesQuery(Mixer.Info info, String query) {
    if (query == null || query.trim().isEmpty()) {
      return true;
    }

    String q = query.trim().toLowerCase(Locale.ROOT);
    String name = info.getName().toLowerCase(Locale.ROOT);
    String desc = info.getDescription().toLowerCase(Locale.ROOT);
    String label = (info.getName() + " :: " + info.getDescription()).toLowerCase(Locale.ROOT);

    if (name.contains(q) || desc.contains(q) || label.contains(q)) {
      return true;
    }

    if (q.contains("::")) {
      String[] parts = q.split("::", 2);
      String left = parts[0].trim();
      String right = parts[1].trim();
      if (!left.isEmpty() && (name.contains(left) || desc.contains(left))) {
        return true;
      }
      if (!right.isEmpty() && (name.contains(right) || desc.contains(right))) {
        return true;
      }
    }
    return false;
  }

  private static boolean supportsTargetDataLine(Mixer m, AudioFormat format) {
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    if (m.isLineSupported(info)) {
      return true;
    }
    for (Line.Info li : m.getTargetLineInfo()) {
      if (TargetDataLine.class.isAssignableFrom(li.getLineClass())) {
        return true;
      }
    }
    return false;
  }
}
