package local.wled.core;

import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class SignalProcessing {
  private static final int DDP_HEADER_LEN = 10;
  private static final int DDP_MAX_DATALEN = 480 * 3;
  private static final int DDP_FLAG_VER1 = 0x40;
  private static final int DDP_FLAG_PUSH = 0x01;
  private static final int DDP_DATATYPE_RGB = 0x0B;

  private SignalProcessing() {
  }

  static DatagramSocket createMulticastSender() throws IOException {
    MulticastSocket ms = new MulticastSocket();
    try {
      ms.setTimeToLive(1);
    } catch (Exception ignored) {
    }
    return ms;
  }

  static int readFully(TargetDataLine line, byte[] buf, int off, int len) {
    int read = 0;
    while (read < len) {
      int n = line.read(buf, off + read, len - read);
      if (n <= 0) {
        break;
      }
      read += n;
    }
    return read;
  }

  static byte[] buildWledAudioSyncV2(
      float sampleRaw,
      float sampleSmth,
      int samplePeak,
      int frameCounter,
      byte[] fft16,
      float fftMagnitude,
      float fftMajorPeakHz
  ) {
    ByteBuffer bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
    bb.put((byte) '0').put((byte) '0').put((byte) '0').put((byte) '0').put((byte) '2').put((byte) 0);
    bb.putShort((short) 0);
    bb.putFloat(sampleRaw);
    bb.putFloat(sampleSmth);
    bb.put((byte) (samplePeak & 0xFF));
    bb.put((byte) (frameCounter & 0xFF));
    bb.put(fft16, 0, 16);
    bb.putShort((short) 0);
    bb.putFloat(fftMagnitude);
    bb.putFloat(fftMajorPeakHz);
    return bb.array();
  }

  static byte[][] buildDdpPackets(byte[] rgbData, int frameCounter, int destinationId) {
    int packetCount = Math.max(1, (rgbData.length + DDP_MAX_DATALEN - 1) / DDP_MAX_DATALEN);
    byte[][] packets = new byte[packetCount][];
    int sequence = (frameCounter % 15) + 1;
    for (int i = 0; i < packetCount; i++) {
      int offset = i * DDP_MAX_DATALEN;
      int len = Math.min(DDP_MAX_DATALEN, rgbData.length - offset);
      boolean push = i == packetCount - 1;
      ByteBuffer bb = ByteBuffer.allocate(DDP_HEADER_LEN + len).order(ByteOrder.BIG_ENDIAN);
      bb.put((byte) (DDP_FLAG_VER1 | (push ? DDP_FLAG_PUSH : 0)));
      bb.put((byte) (sequence & 0xFF));
      bb.put((byte) DDP_DATATYPE_RGB);
      bb.put((byte) (destinationId & 0xFF));
      bb.putInt(offset);
      bb.putShort((short) (len & 0xFFFF));
      bb.put(rgbData, offset, len);
      packets[i] = bb.array();
    }
    return packets;
  }

  static void renderSpectrumToDdpRgb(
      byte[] spectrum16,
      float smoothedAmp,
      byte[] outRgb,
      DdpLayoutMode layoutMode,
      DdpColorPalette colorPalette
  ) {
    int pixels = outRgb.length / 3;
    if (pixels <= 0) {
      return;
    }

    DdpLayoutMode mode = layoutMode == null ? DdpLayoutMode.REPEAT : layoutMode;
    DdpColorPalette palette = colorPalette == null ? DdpColorPalette.AURORA : colorPalette;
    double ampNorm = clamp01(smoothedAmp / 255.0);
    double global = 0.24 + 1.40 * Math.pow(ampNorm, 0.72);
    double bandsMax = SenderMetrics.SPECTRUM_BANDS - 1.0;
    for (int i = 0; i < pixels; i++) {
      double pos = resolveBandPosition(i, pixels, mode, bandsMax);
      int i0 = (int) Math.floor(pos);
      int i1 = Math.min(SenderMetrics.SPECTRUM_BANDS - 1, i0 + 1);
      double mix = pos - i0;

      double b0 = (spectrum16[i0] & 0xFF) / 255.0;
      double b1 = (spectrum16[i1] & 0xFF) / 255.0;
      double level = mode == DdpLayoutMode.STRETCH ? b0 * (1.0 - mix) + b1 * mix : b0;
      double gated = Math.max(0.0, level - 0.045) / 0.955;
      double shaped = Math.pow(gated, 0.58);
      double punch = 0.88 + 0.46 * Math.pow(Math.max(0.0, level), 0.5);
      double value = clamp01(shaped * global * punch);
      double posNorm = pos / bandsMax;
      double hue = paletteHue(palette, posNorm, value);
      double sat = paletteSaturation(palette, posNorm, value);
      double val = paletteValue(palette, posNorm, value);

      int[] rgb = hsvToRgb(hue, sat, val);
      int base = i * 3;
      outRgb[base] = (byte) (rgb[0] & 0xFF);
      outRgb[base + 1] = (byte) (rgb[1] & 0xFF);
      outRgb[base + 2] = (byte) (rgb[2] & 0xFF);
    }
  }

  private static double paletteHue(DdpColorPalette palette, double posNorm, double value) {
    switch (palette) {
      case SUNSET:
        return 34.0 - posNorm * 44.0 - value * 8.0;
      case FIRE:
        return 16.0 - posNorm * 20.0 - value * 4.0;
      case OCEAN:
        return 222.0 - posNorm * 48.0 + value * 6.0;
      case CANDY:
        return 318.0 - posNorm * 62.0 - value * 5.0;
      case AURORA:
      default:
        return 176.0 - posNorm * 108.0 - value * 10.0;
    }
  }

  private static double paletteSaturation(DdpColorPalette palette, double posNorm, double value) {
    switch (palette) {
      case SUNSET:
        return clamp01(0.88 + value * 0.10);
      case FIRE:
        return clamp01(0.94 + value * 0.06);
      case OCEAN:
        return clamp01(0.80 + (1.0 - posNorm) * 0.10 + value * 0.08);
      case CANDY:
        return clamp01(0.90 + posNorm * 0.08 + value * 0.02);
      case AURORA:
      default:
        return clamp01(0.86 + value * 0.12);
    }
  }

  private static double paletteValue(DdpColorPalette palette, double posNorm, double value) {
    switch (palette) {
      case SUNSET:
        return clamp01(value * 0.90 + 0.10 + posNorm * 0.08);
      case FIRE:
        return clamp01(value * 0.86 + 0.14);
      case OCEAN:
        return clamp01(value * 0.80 + 0.06 + (1.0 - posNorm) * 0.06);
      case CANDY:
        return clamp01(value * 0.74 + 0.20 + posNorm * 0.04);
      case AURORA:
      default:
        return clamp01(value * 0.88 + 0.12);
    }
  }

  private static double resolveBandPosition(int pixelIndex, int pixelCount, DdpLayoutMode mode, double bandsMax) {
    if (pixelCount <= 1) {
      return 0.0;
    }
    double t = pixelIndex / (pixelCount - 1.0);
    if (mode == DdpLayoutMode.STRETCH) {
      return t * bandsMax;
    }
    if (mode == DdpLayoutMode.MIRROR) {
      double mirrored = t <= 0.5 ? t * 2.0 : (1.0 - t) * 2.0;
      double halfCycles = Math.max(1.0, Math.round((pixelCount * 0.5) / 128.0));
      return ((mirrored * halfCycles) % 1.0) * bandsMax;
    }
    double cycles = Math.max(1.0, Math.round(pixelCount / 128.0));
    return ((t * cycles) % 1.0) * bandsMax;
  }

  private static double clamp01(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }

  private static int[] hsvToRgb(double hue, double sat, double val) {
    double h = ((hue % 360.0) + 360.0) % 360.0;
    double c = val * sat;
    double x = c * (1.0 - Math.abs((h / 60.0) % 2.0 - 1.0));
    double m = val - c;

    double r;
    double g;
    double b;
    if (h < 60.0) {
      r = c;
      g = x;
      b = 0.0;
    } else if (h < 120.0) {
      r = x;
      g = c;
      b = 0.0;
    } else if (h < 180.0) {
      r = 0.0;
      g = c;
      b = x;
    } else if (h < 240.0) {
      r = 0.0;
      g = x;
      b = c;
    } else if (h < 300.0) {
      r = x;
      g = 0.0;
      b = c;
    } else {
      r = c;
      g = 0.0;
      b = x;
    }

    int ri = (int) Math.round(255.0 * (r + m));
    int gi = (int) Math.round(255.0 * (g + m));
    int bi = (int) Math.round(255.0 * (b + m));
    return new int[]{
        Math.max(0, Math.min(255, ri)),
        Math.max(0, Math.min(255, gi)),
        Math.max(0, Math.min(255, bi))
    };
  }

  static double[] hannWindow(int n) {
    double[] w = new double[n];
    for (int i = 0; i < n; i++) {
      w[i] = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (n - 1));
    }
    return w;
  }

  static float clamp255(double v) {
    if (v < 0.0) {
      return 0f;
    }
    if (v > 255.0) {
      return 255f;
    }
    return (float) v;
  }

  static float updateAutoGain(float current, float value, float target) {
    return updateAutoGain(current, value, target, 0.1f, 20f);
  }

  static float updateAutoGain(float current, float value, float target, float minGain, float maxGain) {
    float eps = 1e-3f;
    float ratio = target / Math.max(eps, value);
    float desired = clampGain(current * ratio, minGain, maxGain);
    return 0.98f * current + 0.02f * desired;
  }

  private static float clampGain(float g, float minGain, float maxGain) {
    if (g < minGain) {
      return minGain;
    }
    if (g > maxGain) {
      return maxGain;
    }
    return g;
  }

  static FftSummary summarizeFftTo16(
      double[] re,
      double[] im,
      int sampleRate,
      int n,
      float fftAutoGain,
      byte[] out16
  ) {
    int half = n / 2;
    int minBin = Math.max(1, (int) Math.floor(40.0 * n / sampleRate));
    double fftScale = 2.0 / n;

    double maxMag = 0.0;
    int maxBin = minBin;
    double[] mags = new double[half];
    for (int i = 0; i < half; i++) {
      double mag = Math.hypot(re[i], im[i]) * fftScale;
      mags[i] = mag;
      if (i >= minBin && mag > maxMag) {
        maxMag = mag;
        maxBin = i;
      }
    }

    double fLow = 40.0;
    double fHigh = Math.min(12000.0, sampleRate / 2.0);
    double ratio = Math.pow(fHigh / fLow, 1.0 / 16.0);
    double f0 = fLow;

    float gainInput = (float) (maxMag * 255.0 * fftAutoGain);
    float scaledMax = clamp255(gainInput);
    float nextGain = updateAutoGain(fftAutoGain, gainInput, 160f, 0.003f, 20f);

    for (int b = 0; b < 16; b++) {
      double f1 = f0 * ratio;
      int i0 = (int) Math.floor(f0 * n / sampleRate);
      int i1 = (int) Math.ceil(f1 * n / sampleRate);
      i0 = Math.max(i0, minBin);
      i1 = Math.min(i1, half - 1);
      if (i1 <= i0) {
        i1 = Math.min(i0 + 1, half - 1);
      }

      double bandAcc = 0.0;
      int bandCount = 0;
      for (int i = i0; i <= i1; i++) {
        double m = mags[i];
        bandAcc += m * m;
        bandCount++;
      }
      double band = Math.sqrt(bandAcc / Math.max(1, bandCount));

      double v = Math.max(0.0, band * 255.0 * nextGain);
      double vv = Math.log10(1.0 + v) / Math.log10(1.0 + 255.0);
      int out = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, vv)));
      out16[b] = (byte) (out & 0xFF);
      f0 = f1;
    }

    float majorHz = (float) (maxBin * (sampleRate / (double) n));
    return new FftSummary(scaledMax, majorHz, nextGain);
  }

  static void fftRadix2(double[] re, double[] im) {
    int n = re.length;
    int bits = 31 - Integer.numberOfLeadingZeros(n);
    for (int i = 0; i < n; i++) {
      int j = Integer.reverse(i) >>> (32 - bits);
      if (j > i) {
        double tr = re[i];
        re[i] = re[j];
        re[j] = tr;
        double ti = im[i];
        im[i] = im[j];
        im[j] = ti;
      }
    }

    for (int size = 2; size <= n; size <<= 1) {
      int halfsize = size >>> 1;
      double tablestep = -2.0 * Math.PI / size;
      for (int i = 0; i < n; i += size) {
        for (int j = 0; j < halfsize; j++) {
          double angle = j * tablestep;
          double wr = Math.cos(angle);
          double wi = Math.sin(angle);
          int k = i + j;
          int l = k + halfsize;
          double tr = wr * re[l] - wi * im[l];
          double ti = wr * im[l] + wi * re[l];
          re[l] = re[k] - tr;
          im[l] = im[k] - ti;
          re[k] = re[k] + tr;
          im[k] = im[k] + ti;
        }
      }
    }
  }

  static final class FftSummary {
    final float magnitude;
    final float majorPeakHz;
    final float nextAutoGain;

    FftSummary(float magnitude, float majorPeakHz, float nextAutoGain) {
      this.magnitude = magnitude;
      this.majorPeakHz = majorPeakHz;
      this.nextAutoGain = nextAutoGain;
    }
  }
}
