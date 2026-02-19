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

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 180f, out, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);

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

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 210f, out, DdpLayoutMode.MIRROR, DdpColorPalette.OCEAN);

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

    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 170f, aurora, DdpLayoutMode.REPEAT, DdpColorPalette.AURORA);
    SignalProcessing.renderSpectrumToDdpRgb(spectrum, 170f, sunset, DdpLayoutMode.REPEAT, DdpColorPalette.SUNSET);

    assertFalse(java.util.Arrays.equals(aurora, sunset));
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
}
