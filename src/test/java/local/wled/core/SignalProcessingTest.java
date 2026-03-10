package local.wled.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalProcessingTest {

  @Test
  void buildDdpPacketsSplitsLargePayloadAndSetsPushOnlyOnLastPacket() {
    byte[] rgb = new byte[3000];
    for (int i = 0; i < rgb.length; i++) {
      rgb[i] = (byte) (i & 0xFF);
    }

    byte[][] packets = SignalProcessing.buildDdpPackets(rgb, 22, 1);

    assertEquals(3, packets.length);
    assertEquals(1450, packets[0].length);
    assertEquals(1450, packets[1].length);
    assertEquals(130, packets[2].length);

    assertEquals(0x40, packets[0][0] & 0xFF);
    assertEquals(0x40, packets[1][0] & 0xFF);
    assertEquals(0x41, packets[2][0] & 0xFF);

    assertEquals(8, packets[0][1] & 0xFF);
    assertEquals(8, packets[1][1] & 0xFF);
    assertEquals(8, packets[2][1] & 0xFF);

    assertEquals(0x0B, packets[0][2] & 0xFF);
    assertEquals(1, packets[0][3] & 0xFF);

    assertEquals(0, readU32(packets[0], 4));
    assertEquals(1440, readU32(packets[1], 4));
    assertEquals(2880, readU32(packets[2], 4));

    assertEquals(1440, readU16(packets[0], 8));
    assertEquals(1440, readU16(packets[1], 8));
    assertEquals(120, readU16(packets[2], 8));

    assertArrayEquals(slice(rgb, 0, 1440), slice(packets[0], 10, 1450));
    assertArrayEquals(slice(rgb, 1440, 2880), slice(packets[1], 10, 1450));
    assertArrayEquals(slice(rgb, 2880, 3000), slice(packets[2], 10, 130));
  }

  @Test
  void renderSpectrumToDdpRgbProducesRgbTripletsInBounds() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (i * 16);
    }
    byte[] out = new byte[90 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 180f, 0, out, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);

    for (int i = 0; i < out.length; i += 3) {
      int r = out[i] & 0xFF;
      int g = out[i + 1] & 0xFF;
      int b = out[i + 2] & 0xFF;
      assertTrue(r >= 0 && r <= 255);
      assertTrue(g >= 0 && g <= 255);
      assertTrue(b >= 0 && b <= 255);
    }
  }

  @Test
  void renderSpectrumMirrorModeIsSymmetricAcrossStrip() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (40 + i * 12);
    }
    byte[] out = new byte[240 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 210f, 11, out, DdpLayoutMode.MIRROR, DdpColorPalette.OCEAN);

    int pixels = out.length / 3;
    for (int i = 0; i < pixels / 2; i++) {
      int baseA = i * 3;
      int baseB = (pixels - 1 - i) * 3;
      assertEquals(out[baseA] & 0xFF, out[baseB] & 0xFF);
      assertEquals(out[baseA + 1] & 0xFF, out[baseB + 1] & 0xFF);
      assertEquals(out[baseA + 2] & 0xFF, out[baseB + 2] & 0xFF);
    }
  }

  @Test
  void differentPalettesProduceDifferentRgbOutput() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (50 + i * 10);
    }
    byte[] aurora = new byte[180 * 3];
    byte[] sunset = new byte[180 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 170f, 0, aurora, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 170f, 0, sunset, DdpLayoutMode.REPEAT, DdpColorPalette.SUNSET);

    assertFalse(java.util.Arrays.equals(aurora, sunset));
  }

  @Test
  void renderSpectrumToDdpRgbChangesOverFramesForSameSpectrum() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (70 + i * 8);
    }
    byte[] frame0 = new byte[180 * 3];
    byte[] frame9 = new byte[180 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 190f, 0, frame0, DdpLayoutMode.REPEAT, DdpColorPalette.FIRE);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 190f, 9, frame9, DdpLayoutMode.REPEAT, DdpColorPalette.FIRE);

    assertFalse(java.util.Arrays.equals(frame0, frame9));
  }

  @Test
  void nightclubPaletteProducesDifferentOutputFromAurora() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (60 + i * 9);
    }
    byte[] aurora = new byte[150 * 3];
    byte[] nightclub = new byte[150 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 175f, 6, aurora, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 175f, 6, nightclub, DdpLayoutMode.REPEAT, DdpColorPalette.NIGHTCLUB);

    assertFalse(java.util.Arrays.equals(aurora, nightclub));
  }

  @Test
  void fireHasMoreAggressiveFrameToFrameMotionThanOcean() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (80 + i * 7);
    }
    byte[] fireA = new byte[180 * 3];
    byte[] fireB = new byte[180 * 3];
    byte[] oceanA = new byte[180 * 3];
    byte[] oceanB = new byte[180 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 210f, 24, fireA, DdpLayoutMode.REPEAT, DdpColorPalette.FIRE);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 210f, 25, fireB, DdpLayoutMode.REPEAT, DdpColorPalette.FIRE);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 210f, 24, oceanA, DdpLayoutMode.REPEAT, DdpColorPalette.OCEAN);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 210f, 25, oceanB, DdpLayoutMode.REPEAT, DdpColorPalette.OCEAN);

    int fireDelta = frameDeltaLuma(fireA, fireB);
    int oceanDelta = frameDeltaLuma(oceanA, oceanB);
    assertTrue(fireDelta >= oceanDelta + 900, "fireDelta=" + fireDelta + ", oceanDelta=" + oceanDelta);
  }

  @Test
  void candyHasSharperSpatialCutsThanSunset() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (55 + i * 11);
    }
    byte[] candy = new byte[180 * 3];
    byte[] sunset = new byte[180 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 185f, 18, candy, DdpLayoutMode.REPEAT, DdpColorPalette.CANDY);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 185f, 18, sunset, DdpLayoutMode.REPEAT, DdpColorPalette.SUNSET);

    int candyEdges = spatialEdgeEnergy(candy);
    int sunsetEdges = spatialEdgeEnergy(sunset);
    assertTrue(candyEdges >= sunsetEdges + 1400, "candyEdges=" + candyEdges + ", sunsetEdges=" + sunsetEdges);
  }

  @Test
  void repeatLayoutStaysVisiblyChunkierThanStretchAtFortyTwoPixels() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (36 + i * 13);
    }
    byte[] stretch = new byte[42 * 3];
    byte[] repeat = new byte[42 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 176f, 12, stretch, DdpLayoutMode.STRETCH, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 176f, 12, repeat, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);

    int stretchEdges = spatialEdgeEnergy(stretch);
    int repeatEdges = spatialEdgeEnergy(repeat);
    assertTrue(repeatEdges >= stretchEdges + 220, "repeatEdges=" + repeatEdges + ", stretchEdges=" + stretchEdges);
  }

  @Test
  void mirrorLayoutMakesCenterBrighterThanEdgesOnRisingSpectrum() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (24 + i * 14);
    }
    byte[] mirror = new byte[42 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 184f, 10, mirror, DdpLayoutMode.MIRROR, DdpColorPalette.AURORA);

    int centerLuma = averageLuma(mirror, 16, 26);
    int edgeLuma = (averageLuma(mirror, 0, 5) + averageLuma(mirror, 37, 42)) / 2;
    assertTrue(centerLuma >= edgeLuma + 40, "centerLuma=" + centerLuma + ", edgeLuma=" + edgeLuma);
  }

  @Test
  void mirrorLayoutHasStrongerCenterBiasThanRepeat() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (24 + i * 14);
    }
    byte[] mirror = new byte[42 * 3];
    byte[] repeat = new byte[42 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 184f, 10, mirror, DdpLayoutMode.MIRROR, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 184f, 10, repeat, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);

    int mirrorCenterBias = averageLuma(mirror, 16, 26) - (averageLuma(mirror, 0, 5) + averageLuma(mirror, 37, 42)) / 2;
    int repeatCenterBias = averageLuma(repeat, 16, 26) - (averageLuma(repeat, 0, 5) + averageLuma(repeat, 37, 42)) / 2;
    assertTrue(mirrorCenterBias >= repeatCenterBias + 30,
        "mirrorCenterBias=" + mirrorCenterBias + ", repeatCenterBias=" + repeatCenterBias);
  }

  @Test
  void repeatLayoutRepeatsItsFirstHalfMoreThanStretch() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (32 + i * 14);
    }
    byte[] stretch = new byte[42 * 3];
    byte[] repeat = new byte[42 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 188f, 14, stretch, DdpLayoutMode.STRETCH, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 188f, 14, repeat, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);

    int stretchHalfDelta = repeatedHalfDelta(stretch);
    int repeatHalfDelta = repeatedHalfDelta(repeat);
    assertTrue(repeatHalfDelta + 220 <= stretchHalfDelta,
        "repeatHalfDelta=" + repeatHalfDelta + ", stretchHalfDelta=" + stretchHalfDelta);
  }

  @Test
  void repeatLayoutRepeatsThreeSlicesAtFortyTwoPixels() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (32 + i * 14);
    }
    byte[] repeat = new byte[42 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 188f, 14, repeat, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);

    int sliceDelta = repeatedSliceDelta(repeat, 14);
    assertTrue(sliceDelta <= 240, "sliceDelta=" + sliceDelta);
  }

  @Test
  void mirrorLayoutCreatesStrongerCenterPeakThanStretch() {
    byte[] spectrum = new byte[16];
    for (int i = 0; i < spectrum.length; i++) {
      spectrum[i] = (byte) (28 + i * 14);
    }
    byte[] stretch = new byte[42 * 3];
    byte[] mirror = new byte[42 * 3];

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 188f, 14, stretch, DdpLayoutMode.STRETCH, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 188f, 14, mirror, DdpLayoutMode.MIRROR, DdpColorPalette.AURORA);

    int stretchCenterBias = averageLuma(stretch, 16, 26) - (averageLuma(stretch, 0, 5) + averageLuma(stretch, 37, 42)) / 2;
    int mirrorCenterBias = averageLuma(mirror, 16, 26) - (averageLuma(mirror, 0, 5) + averageLuma(mirror, 37, 42)) / 2;
    assertTrue(mirrorCenterBias >= stretchCenterBias + 90,
        "mirrorCenterBias=" + mirrorCenterBias + ", stretchCenterBias=" + stretchCenterBias);
  }

  private static int readU16(byte[] src, int off) {
    return ((src[off] & 0xFF) << 8) | (src[off + 1] & 0xFF);
  }

  private static long readU32(byte[] src, int off) {
    return ((long) (src[off] & 0xFF) << 24)
        | ((long) (src[off + 1] & 0xFF) << 16)
        | ((long) (src[off + 2] & 0xFF) << 8)
        | (src[off + 3] & 0xFFL);
  }

  private static byte[] slice(byte[] src, int fromInclusive, int toExclusive) {
    byte[] out = new byte[toExclusive - fromInclusive];
    System.arraycopy(src, fromInclusive, out, 0, out.length);
    return out;
  }

  private static int frameDeltaLuma(byte[] frameA, byte[] frameB) {
    int acc = 0;
    for (int i = 0; i < frameA.length; i += 3) {
      int a = ((frameA[i] & 0xFF) * 299 + (frameA[i + 1] & 0xFF) * 587 + (frameA[i + 2] & 0xFF) * 114) / 1000;
      int b = ((frameB[i] & 0xFF) * 299 + (frameB[i + 1] & 0xFF) * 587 + (frameB[i + 2] & 0xFF) * 114) / 1000;
      acc += Math.abs(a - b);
    }
    return acc;
  }

  private static int spatialEdgeEnergy(byte[] frame) {
    int acc = 0;
    for (int i = 3; i < frame.length; i += 3) {
      int prev = ((frame[i - 3] & 0xFF) * 299 + (frame[i - 2] & 0xFF) * 587 + (frame[i - 1] & 0xFF) * 114) / 1000;
      int curr = ((frame[i] & 0xFF) * 299 + (frame[i + 1] & 0xFF) * 587 + (frame[i + 2] & 0xFF) * 114) / 1000;
      acc += Math.abs(curr - prev);
    }
    return acc;
  }

  private static int averageLuma(byte[] frame, int fromPixelInclusive, int toPixelExclusive) {
    int acc = 0;
    int count = 0;
    for (int pixel = fromPixelInclusive; pixel < toPixelExclusive; pixel++) {
      int base = pixel * 3;
      acc += ((frame[base] & 0xFF) * 299 + (frame[base + 1] & 0xFF) * 587 + (frame[base + 2] & 0xFF) * 114) / 1000;
      count++;
    }
    return count == 0 ? 0 : acc / count;
  }

  private static int repeatedHalfDelta(byte[] frame) {
    int acc = 0;
    int halfPixels = (frame.length / 3) / 2;
    for (int pixel = 0; pixel < halfPixels; pixel++) {
      int baseA = pixel * 3;
      int baseB = (pixel + halfPixels) * 3;
      int lumaA = ((frame[baseA] & 0xFF) * 299 + (frame[baseA + 1] & 0xFF) * 587 + (frame[baseA + 2] & 0xFF) * 114) / 1000;
      int lumaB = ((frame[baseB] & 0xFF) * 299 + (frame[baseB + 1] & 0xFF) * 587 + (frame[baseB + 2] & 0xFF) * 114) / 1000;
      acc += Math.abs(lumaA - lumaB);
    }
    return acc;
  }

  private static int repeatedSliceDelta(byte[] frame, int slicePixels) {
    int acc = 0;
    for (int pixel = 0; pixel < slicePixels; pixel++) {
      int lumaA = pixelLuma(frame, pixel);
      int lumaB = pixelLuma(frame, pixel + slicePixels);
      int lumaC = pixelLuma(frame, pixel + slicePixels * 2);
      acc += Math.abs(lumaA - lumaB);
      acc += Math.abs(lumaB - lumaC);
    }
    return acc;
  }

  private static int pixelLuma(byte[] frame, int pixel) {
    int base = pixel * 3;
    return ((frame[base] & 0xFF) * 299 + (frame[base + 1] & 0xFF) * 587 + (frame[base + 2] & 0xFF) * 114) / 1000;
  }
}
