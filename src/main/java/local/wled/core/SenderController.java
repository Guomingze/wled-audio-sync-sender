package local.wled.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class SenderController {
  private static final OutboundFrame POISON = OutboundFrame.poison();
  private static final long PUSH_RETRY_BASE_MILLIS = 250L;
  private static final long PUSH_RETRY_MAX_MILLIS = 3_000L;

  private final SenderConfig config;
  private final Consumer<String> log;
  private final Consumer<SenderMetrics> metricsConsumer;
  private final Consumer<Throwable> errorConsumer;

  private final AtomicBoolean stopRequested = new AtomicBoolean(false);
  private final AtomicBoolean errorReported = new AtomicBoolean(false);
  private final AtomicBoolean captureEnabled;
  private final AtomicBoolean pushEnabled;
  private final AtomicBoolean verboseEnabled;
  private final AtomicReference<DdpLayoutMode> activeDdpLayoutMode;
  private final AtomicReference<DdpColorPalette> activeDdpColorPalette;
  private final AtomicReference<PushTarget> activePushTarget;
  private volatile boolean running;
  private volatile Thread worker;
  private volatile Thread captureWorker;
  private volatile Thread pushWorker;
  private volatile TargetDataLine currentLine;
  private volatile DatagramSocket currentSocket;

  public SenderController(SenderConfig config, Consumer<String> log, Consumer<SenderMetrics> metricsConsumer, Consumer<Throwable> errorConsumer) {
    this.config = config;
    this.log = log;
    this.metricsConsumer = metricsConsumer;
    this.errorConsumer = errorConsumer;
    this.captureEnabled = new AtomicBoolean(config.captureEnabled);
    this.pushEnabled = new AtomicBoolean(config.pushEnabled);
    this.verboseEnabled = new AtomicBoolean(config.verbose);
    this.activeDdpLayoutMode = new AtomicReference<DdpLayoutMode>(config.ddpLayoutMode);
    this.activeDdpColorPalette = new AtomicReference<DdpColorPalette>(config.ddpColorPalette);
    this.activePushTarget = new AtomicReference<PushTarget>(new PushTarget(config.dest, config.port));
  }

  public void setCaptureEnabled(boolean enabled) {
    boolean prev = captureEnabled.getAndSet(enabled);
    if (prev != enabled) {
      log.accept("采集开关: " + (enabled ? "已开启" : "已关闭"));
    }
  }

  public void setPushEnabled(boolean enabled) {
    boolean prev = pushEnabled.getAndSet(enabled);
    if (prev != enabled) {
      log.accept("推送开关: " + (enabled ? "已开启" : "已关闭"));
      if (!enabled) {
        closeQuietly(currentSocket);
      }
    }
  }

  public void setDdpLayoutMode(DdpLayoutMode mode) {
    if (mode == null) {
      return;
    }
    DdpLayoutMode prev = activeDdpLayoutMode.getAndSet(mode);
    if (prev != mode) {
      log.accept("DDP 布局已切换: " + mode.uiLabel());
    }
  }

  public void setDdpColorPalette(DdpColorPalette palette) {
    if (palette == null) {
      return;
    }
    DdpColorPalette prev = activeDdpColorPalette.getAndSet(palette);
    if (prev != palette) {
      log.accept("DDP 配色已切换: " + palette.uiLabel());
    }
  }

  public void setPushTarget(String dest, int port) {
    PushTarget prev = activePushTarget.get();
    String nextDest = normalizeDest(dest, prev.dest);
    int nextPort = normalizePort(port, prev.port);
    PushTarget next = new PushTarget(nextDest, nextPort);
    if (prev.equals(next)) {
      return;
    }
    activePushTarget.set(next);
    log.accept("推送目标已更新: " + next.dest + ":" + next.port);
    closeQuietly(currentSocket);
  }

  public void setVerboseEnabled(boolean enabled) {
    boolean prev = verboseEnabled.getAndSet(enabled);
    if (prev != enabled) {
      log.accept("详细日志 (Verbose): " + (enabled ? "已开启" : "已关闭"));
    }
  }

  public synchronized void start() {
    if (running) {
      return;
    }
    stopRequested.set(false);
    errorReported.set(false);
    running = true;
    worker = new Thread(this::runLoop, "wled-audio-sync-sender");
    worker.setDaemon(true);
    worker.start();
  }

  public synchronized void stop() {
    stopRequested.set(true);
    closeQuietly(currentLine);
    closeQuietly(currentSocket);
    interruptQuietly(captureWorker);
    interruptQuietly(pushWorker);
    interruptQuietly(worker);

    Thread w = worker;
    if (w != null && w != Thread.currentThread()) {
      try {
        w.join(1200L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public boolean isRunning() {
    return running;
  }

  private void runLoop() {
    BlockingQueue<OutboundFrame> queue = new ArrayBlockingQueue<OutboundFrame>(1);
    try {
      AudioFormat format = new AudioFormat((float) config.sampleRate, 16, config.channels, true, false);
      InputSelection input = AudioDeviceCatalog.openTargetLine(format, config.inputDeviceQuery);
      if (input == null) {
        throw new IllegalStateException("找不到匹配的音频输入设备。先运行 --list-devices 再确认 --input。\n例如：--input \"BlackHole\"");
      }

      PushTarget startupTarget = activePushTarget.get();
      InetAddress startupAddr = InetAddress.getByName(startupTarget.dest);
      boolean isMulticast = startupAddr.isMulticastAddress();

      TargetDataLine line = input.line;
      currentLine = line;
      currentSocket = null;

      line.start();
      log.accept("开始发送到 " + startupTarget.dest + ":" + startupTarget.port + " (" + (isMulticast ? "multicast" : "unicast") + ")");
      if (config.outputMode == OutputMode.DDP) {
        log.accept("输出模式: DDP 像素推送, 像素数=" + config.ddpPixelCount + ", 布局=" + activeDdpLayoutMode.get().cliValue() + ", 配色=" + activeDdpColorPalette.get().cliValue() + ", 端口=" + startupTarget.port);
      } else {
        log.accept("输出模式: Audio Sync v2");
      }
      log.accept("采集开关: " + (captureEnabled.get() ? "已开启" : "已关闭") + "，推送开关: " + (pushEnabled.get() ? "已开启" : "已关闭"));
      log.accept("输入设备: " + input.mixerName + " :: " + input.mixerDescription);
      log.accept("输入线路: '" + line.getLineInfo() + "'");
      log.accept("音频格式: " + format);

      captureWorker = new Thread(() -> captureLoop(line, queue), "wled-audio-capture");
      pushWorker = new Thread(() -> pushLoop(queue), "wled-audio-push");
      captureWorker.setDaemon(true);
      pushWorker.setDaemon(true);
      captureWorker.start();
      pushWorker.start();

      captureWorker.join();
      offerPoison(queue);
      pushWorker.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable t) {
      reportError(t);
    } finally {
      closeQuietly(currentLine);
      closeQuietly(currentSocket);
      currentLine = null;
      currentSocket = null;
      captureWorker = null;
      pushWorker = null;
      running = false;
      log.accept("发送已停止。");
    }
  }

  private void captureLoop(TargetDataLine line, BlockingQueue<OutboundFrame> queue) {
    try {
      float smoothed = 0f;
      float fftAutoGain = 1.0f;
      float ampAutoGain = 1.0f;
      int frameCounter = 0;
      int samplesPerFrame = config.fftSize;
      int bytesPerSample = 2;
      int bytesPerFrame = bytesPerSample * config.channels;
      int bytesToRead = samplesPerFrame * bytesPerFrame;

      byte[] buf = new byte[bytesToRead];
      double[] re = new double[config.fftSize];
      double[] im = new double[config.fftSize];
      double[] window = SignalProcessing.hannWindow(config.fftSize);
      byte[] fft16 = new byte[16];
      byte[] ddpRgb = config.outputMode == OutputMode.DDP ? new byte[Math.max(1, config.ddpPixelCount) * 3] : null;

      long frameIntervalNanos = (long) (1_000_000_000.0 / Math.max(1, config.fps));
      long nextFrame = System.nanoTime();
      float peakValue = 0f;

      while (!stopRequested.get()) {
        long now = System.nanoTime();
        if (now < nextFrame) {
          long sleepNanos = nextFrame - now;
          if (sleepNanos > 2_000_000L) {
            Thread.sleep(sleepNanos / 1_000_000L);
          }
        }
        nextFrame += frameIntervalNanos;

        int n = SignalProcessing.readFully(line, buf, 0, buf.length);
        if (n != buf.length) {
          continue;
        }

        if (!captureEnabled.get()) {
          frameCounter++;
          smoothed = smoothed * 0.80f;
          peakValue = Math.max(0f, peakValue * 0.82f - 1.2f);
          int peak = Math.round(SignalProcessing.clamp255(peakValue));
          Arrays.fill(fft16, (byte) 0);
          byte[] spectrumCopy = new byte[SenderMetrics.SPECTRUM_BANDS];
          SenderMetrics metrics = new SenderMetrics(0f, smoothed, peak, 0f, 0f, frameCounter, spectrumCopy);
          enqueueLatest(queue, OutboundFrame.data(new byte[0][], metrics));
          continue;
        }

        int frames = samplesPerFrame;
        double rmsAcc = 0.0;
        for (int i = 0; i < frames; i++) {
          int base = i * bytesPerFrame;
          int sum = 0;
          for (int ch = 0; ch < config.channels; ch++) {
            int lo = buf[base + ch * 2] & 0xFF;
            int hi = buf[base + ch * 2 + 1];
            short s = (short) ((hi << 8) | lo);
            sum += s;
          }
          double sample = (sum / (double) config.channels) / 32768.0;
          rmsAcc += sample * sample;
          re[i] = sample * window[i];
          im[i] = 0.0;
        }

        float rms = (float) Math.sqrt(rmsAcc / frames);
        float rawAmp = SignalProcessing.clamp255(rms * 512.0f * ampAutoGain);
        ampAutoGain = SignalProcessing.updateAutoGain(ampAutoGain, rawAmp, 140f);

        smoothed = 0.85f * smoothed + 0.15f * rawAmp;

        SignalProcessing.fftRadix2(re, im);
        SignalProcessing.FftSummary fftSummary = SignalProcessing.summarizeFftTo16(re, im, config.sampleRate, config.fftSize, fftAutoGain, fft16);
        fftAutoGain = fftSummary.nextAutoGain;

        if (rawAmp >= peakValue) {
          peakValue = rawAmp;
        } else {
          peakValue = Math.max(rawAmp, peakValue * 0.92f - 1.5f);
        }
        int peak = Math.round(SignalProcessing.clamp255(peakValue));

        byte[][] packets;
        if (config.outputMode == OutputMode.DDP) {
          SignalProcessing.renderSpectrumToDdpRgb(fft16, smoothed, ddpRgb, activeDdpLayoutMode.get(), activeDdpColorPalette.get());
          packets = SignalProcessing.buildDdpPackets(ddpRgb, frameCounter, 1);
        } else {
          byte[] pkt = SignalProcessing.buildWledAudioSyncV2(rawAmp, smoothed, peak, frameCounter & 0xFF, fft16, fftSummary.magnitude, fftSummary.majorPeakHz);
          packets = new byte[][]{pkt};
        }
        frameCounter++;

        byte[] spectrumCopy = new byte[SenderMetrics.SPECTRUM_BANDS];
        System.arraycopy(fft16, 0, spectrumCopy, 0, spectrumCopy.length);
        SenderMetrics metrics = new SenderMetrics(rawAmp, smoothed, peak, fftSummary.majorPeakHz, fftSummary.magnitude, frameCounter, spectrumCopy);
        enqueueLatest(queue, OutboundFrame.data(packets, metrics));

        if (verboseEnabled.get() && (frameCounter % 10 == 0)) {
          log.accept(String.format(Locale.ROOT,
              "amp raw=%.1f smth=%.1f peak=%d major=%.1fHz mag=%.1f gainA=%.2f gainF=%.2f",
              rawAmp, smoothed, peak, fftSummary.majorPeakHz, fftSummary.magnitude, ampAutoGain, fftAutoGain));
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable t) {
      reportError(t);
      stopRequested.set(true);
    } finally {
      offerPoison(queue);
    }
  }

  private void pushLoop(BlockingQueue<OutboundFrame> queue) {
    DatagramSocket socket = null;
    int consecutiveSendFailures = 0;
    long lastSendErrorLogNanos = 0L;
    boolean connected = false;
    PushTarget connectedTarget = null;
    InetAddress connectedAddress = null;
    long nextRetryAtNanos = 0L;
    long retryDelayMillis = PUSH_RETRY_BASE_MILLIS;
    try {
      while (true) {
        OutboundFrame frame = queue.poll(300L, TimeUnit.MILLISECONDS);
        if (frame == null) {
          continue;
        }
        if (frame.isPoison) {
          break;
        }

        try {
          if (pushEnabled.get()) {
            PushTarget currentTarget = activePushTarget.get();
            boolean targetChanged = !currentTarget.equals(connectedTarget);
            if (socket == null || socket.isClosed() || targetChanged) {
              long now = System.nanoTime();
              if (now < nextRetryAtNanos) {
                metricsConsumer.accept(frame.metrics);
                continue;
              }
              closeQuietly(socket);
              connected = false;
              connectedAddress = InetAddress.getByName(currentTarget.dest);
              socket = createSocket(connectedAddress);
              currentSocket = socket;
              connectedTarget = currentTarget;
            }
            for (byte[] packet : frame.packets) {
              DatagramPacket dp = new DatagramPacket(packet, packet.length, connectedAddress, connectedTarget.port);
              socket.send(dp);
            }
            if (!connected) {
              connected = true;
              log.accept("UDP 推送连接已建立。目标=" + connectedTarget.dest + ":" + connectedTarget.port);
            }
            if (consecutiveSendFailures > 0) {
              log.accept("UDP 推送恢复，连续失败次数=" + consecutiveSendFailures);
              consecutiveSendFailures = 0;
              lastSendErrorLogNanos = 0L;
            }
            nextRetryAtNanos = 0L;
            retryDelayMillis = PUSH_RETRY_BASE_MILLIS;
          } else {
            if (connected) {
              connected = false;
              log.accept("UDP 推送连接已断开（推送关闭）。");
            }
            closeQuietly(socket);
            socket = null;
            currentSocket = null;
            connectedTarget = null;
            connectedAddress = null;
            consecutiveSendFailures = 0;
            lastSendErrorLogNanos = 0L;
            nextRetryAtNanos = 0L;
            retryDelayMillis = PUSH_RETRY_BASE_MILLIS;
          }
        } catch (Exception sendError) {
          if (stopRequested.get()) {
            break;
          }
          if (connected) {
            connected = false;
          }
          closeQuietly(socket);
          socket = null;
          currentSocket = null;
          connectedTarget = null;
          connectedAddress = null;
          consecutiveSendFailures++;
          long now = System.nanoTime();
          nextRetryAtNanos = now + retryDelayMillis * 1_000_000L;
          retryDelayMillis = Math.min(PUSH_RETRY_MAX_MILLIS, retryDelayMillis * 2L);
          if (consecutiveSendFailures == 1 || now - lastSendErrorLogNanos >= 2_000_000_000L) {
            String message = sendError.getMessage();
            long retryAfterMillis = Math.max(1L, (nextRetryAtNanos - now) / 1_000_000L);
            log.accept("UDP 推送失败(不中断采集): 连续失败=" + consecutiveSendFailures + "，原因=" + (message == null ? sendError.getClass().getSimpleName() : message) + "，将在约 " + retryAfterMillis + "ms 后重试");
            lastSendErrorLogNanos = now;
          }
        }
        metricsConsumer.accept(frame.metrics);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Throwable t) {
      reportError(t);
    } finally {
      closeQuietly(socket);
      currentSocket = null;
    }
  }

  private static DatagramSocket createSocket(InetAddress destAddr) throws java.io.IOException {
    if (destAddr.isMulticastAddress()) {
      return SignalProcessing.createMulticastSender();
    }
    return new DatagramSocket();
  }

  private static String normalizeDest(String dest, String fallback) {
    if (dest == null) {
      return fallback;
    }
    String trimmed = dest.trim();
    return trimmed.isEmpty() ? fallback : trimmed;
  }

  private static int normalizePort(int port, int fallback) {
    return port > 0 ? port : fallback;
  }

  private static void enqueueLatest(BlockingQueue<OutboundFrame> queue, OutboundFrame frame) {
    if (queue.offer(frame)) {
      return;
    }
    queue.poll();
    queue.offer(frame);
  }

  private static void offerPoison(BlockingQueue<OutboundFrame> queue) {
    while (!queue.offer(POISON)) {
      queue.poll();
    }
  }

  private void reportError(Throwable t) {
    if (!stopRequested.get() && errorReported.compareAndSet(false, true)) {
      errorConsumer.accept(t);
    }
  }

  private static void interruptQuietly(Thread thread) {
    if (thread != null) {
      thread.interrupt();
    }
  }

  private static void closeQuietly(DatagramSocket socket) {
    if (socket != null) {
      socket.close();
    }
  }

  private static void closeQuietly(TargetDataLine line) {
    if (line == null) {
      return;
    }
    try {
      line.stop();
    } catch (Exception ignored) {
    }
    try {
      line.close();
    } catch (Exception ignored) {
    }
  }

  private static final class OutboundFrame {
    final byte[][] packets;
    final SenderMetrics metrics;
    final boolean isPoison;

    private OutboundFrame(byte[][] packets, SenderMetrics metrics, boolean isPoison) {
      this.packets = packets;
      this.metrics = metrics;
      this.isPoison = isPoison;
    }

    static OutboundFrame data(byte[][] packets, SenderMetrics metrics) {
      return new OutboundFrame(packets, metrics, false);
    }

    static OutboundFrame poison() {
      return new OutboundFrame(new byte[0][], null, true);
    }
  }

  private static final class PushTarget {
    final String dest;
    final int port;

    PushTarget(String dest, int port) {
      this.dest = dest;
      this.port = port;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PushTarget)) {
        return false;
      }
      PushTarget that = (PushTarget) o;
      return port == that.port && Objects.equals(dest, that.dest);
    }

    @Override
    public int hashCode() {
      return Objects.hash(dest, port);
    }
  }
}
