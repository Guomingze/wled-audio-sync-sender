package local.wled.core;

import javax.sound.sampled.TargetDataLine;

final class InputSelection {
  final TargetDataLine line;
  final String mixerName;
  final String mixerDescription;

  InputSelection(TargetDataLine line, String mixerName, String mixerDescription) {
    this.line = line;
    this.mixerName = mixerName;
    this.mixerDescription = mixerDescription;
  }
}
