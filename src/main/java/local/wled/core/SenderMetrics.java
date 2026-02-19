package local.wled.core;

public final class SenderMetrics {
  public static final int SPECTRUM_BANDS = 16;

  public final float rawAmp;
  public final float smoothedAmp;
  public final int peak;
  public final float majorPeakHz;
  public final float magnitude;
  public final int frameCounter;
  public final byte[] spectrum16;

  public SenderMetrics(float rawAmp, float smoothedAmp, int peak, float majorPeakHz, float magnitude, int frameCounter, byte[] spectrum16) {
    this.rawAmp = rawAmp;
    this.smoothedAmp = smoothedAmp;
    this.peak = peak;
    this.majorPeakHz = majorPeakHz;
    this.magnitude = magnitude;
    this.frameCounter = frameCounter;
    this.spectrum16 = spectrum16;
  }
}
