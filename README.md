# WLED 音频同步发送端（Java）

把 macOS 的系统音频（通过 BlackHole）喂给 WLED 的**内置音频反应效果**，协议是 **UDP Sound Sync（Audio Sync v2）**。

这个工具是为了替代偏重的方案（例如 Chataigne），尽量做到“下载/编译/运行就能动”。

## 它做了什么

- 从指定的音频输入设备采集 PCM（例如 `BlackHole 2ch`）
- 计算振幅 + 16 段 FFT（频谱摘要）
- 默认向 `239.0.0.1:11988` 发送 **WLED Audio Sync v2** UDP 包
- 支持 **DDP 像素推送模式**（LedFx 常见方式），默认端口 `4048`

在 WLED 里，效果列表中带“音符”图标（Freq*）的效果，会随音频变化。

## WLED 端设置（接收）

1. WLED 网页 -> `Config` -> `WiFi Setup` -> 打开 `Disable WiFi sleep`
2. WLED 网页 -> `Config` -> `Sync Interfaces` -> `Audio Sync`
   - Mode：`Receive`
   - Port：`11988`（默认）
3. 改完 Audio Sync 模式后，建议给 WLED **重启/断电重上电**一次

WLED 默认值（来自 WLED Wiki）：
- 组播 IP：`239.0.0.1`
- UDP 端口：`11988`

### DDP 模式设置（像素推送）

当你用 `--mode ddp` 时，WLED 不再读取 Audio Sync 的 16 段频谱，而是直接接收 RGB 像素流。

1. WLED 网页 -> `Config` -> `LED Preferences`
   - 确认 `LED count` 与 `--ddp-pixels` 一致或更大
2. WLED 网页 -> `Config` -> `Sync Interfaces`
   - 打开 `DDP RGB (network)`（不同版本文案可能略有区别）
   - 端口保持 `4048`
3. 发送端参数建议：
   - `--dest <wled-ip>`（DDP 优先单播）
   - `--mode ddp --ddp-pixels <像素数> --ddp-layout <stretch|repeat|mirror> --ddp-palette <aurora|sunset|fire|ocean|candy> --port 4048`

DDP 布局模式说明：
- `stretch`：16 频段从头到尾拉伸一次，过渡平滑
- `repeat`：16 频段沿灯带重复铺开，整体起伏更明显（默认）
- `mirror`：左右镜像律动，适合中点扩散的视觉效果

DDP 配色说明：
- `aurora`：青绿到暖色渐变，通用型（默认）
- `sunset`：暖金到橙红，氛围更柔和
- `fire`：高饱和红橙，冲击感更强
- `ocean`：蓝青系冷色，适合环境光
- `candy`：粉紫霓虹，风格化更明显

DDP 常见问题：
- 有数据但灯不亮：优先检查 `LED count` 和 `--ddp-pixels` 是否匹配
- 偶发不动：确认目标 IP 正确，避免把 DDP 发到组播地址
- 延迟偏高：先把像素数降到 `60~120` 做基线，再逐步拉高

## macOS 音频路由（系统声音 -> BlackHole）

如果你希望 Spotify/YouTube 等“系统正在播放的声音”驱动 WLED：

1. 打开 `Audio MIDI Setup`（音频 MIDI 设置）
2. 创建一个 `Multi-Output Device`（多输出设备）
3. 勾选你的真实输出（扬声器/耳机）以及 `BlackHole 2ch`
4. macOS 声音设置 -> 输出设备 -> 选择这个“多输出设备”

这样你能听到声音，同时发送端也能从 `BlackHole 2ch` 采集到系统音频。

## 编译与运行

本项目现在仅支持 **JavaFX GUI** 入口。

当前目录结构（已整理为标准 Maven 布局）：

```text
.
├── pom.xml
├── src/main/java/
│   └── local/wled/
│       ├── app/
│       │   ├── WledAudioSyncSender.java
│       │   ├── Args.java
│       │   └── WledAudioSyncSenderFxApp.java
│       └── core/
│           ├── SenderController.java
│           └── ...
└── README.md
```

推荐使用 Maven（会自动处理 JavaFX 依赖）：

```bash
# 建议先确认 JDK >= 17
/usr/libexec/java_home -V

# 编译
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -DskipTests compile

# 列出可用的音频输入设备（仅命令行查看）
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -Dexec.mainClass=local.wled.app.WledAudioSyncSender -Dexec.args="--list-devices" exec:java

# JavaFX GUI（默认）
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -Djavafx.args="" javafx:run

# JavaFX GUI（带参数预填）
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -Djavafx.args="--input BlackHole --dest 239.0.0.1 --port 11988" javafx:run

# JavaFX GUI（DDP 像素推送模式）
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -Djavafx.args="--mode ddp --dest 192.168.31.57 --ddp-pixels 150 --ddp-layout repeat --ddp-palette aurora --port 4048" javafx:run
```

如果你家路由器对 UDP 组播支持很差，直接改成“单播”发到设备 IP：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -Djavafx.args="--input BlackHole --dest 192.168.31.57 --port 11988" javafx:run
```

如果 Dock 里仍显示为 `java`，改用 `.app` 方式启动（名称会固定为 `WLED音频同步发送器`）：

```bash
./scripts/build-and-open-macos-app.sh
```

常用参数：

- `--input`（默认 `BlackHole`）
- `--dest`（默认 `239.0.0.1`）
- `--mode`（`audio-sync` 或 `ddp`，默认 `audio-sync`）
- `--port`（`audio-sync` 默认 `11988`，`ddp` 默认 `4048`）
- `--ddp-pixels`（DDP 像素数，默认 `90`）
- `--ddp-layout`（DDP 布局：`stretch` / `repeat` / `mirror`，默认 `repeat`）
- `--ddp-palette`（DDP 配色：`aurora` / `sunset` / `fire` / `ocean` / `candy`，默认 `aurora`）
- `--rate`（默认 `44100`）
- `--channels`（默认 `2`）
- `--fft`（默认 `1024`）
- `--fps`（默认 `50`）
- `--verbose`（默认关闭）

## 排查清单

- 完全不动：
  - 确认 WLED 里确实有“音符”图标的效果（说明固件支持音频反应）
  - 确认 WLED：`Audio Sync = Receive`，且修改后重启过
  - 确认 mac 输出选的是“多输出设备”（否则 BlackHole 可能没音频）
  - 确认发送端选的是 `BlackHole 2ch` 作为**输入设备**（不是输出）

- 偶尔动/卡顿：
  - 组播在很多家用路由上不稳定，优先试单播（`--dest <wled-ip>`）
  - 确认 WLED 打开了 `Disable WiFi sleep`

## 备注

- 这个发送端做了简单 AGC（自动增益）和 FFT 映射，目标是“快速可用”。如果你觉得太敏感/不敏感，可以再加参数调节。
- WLED Audio Sync v2 是二进制协议（44 字节，小端序，header 为 `00002`）。
