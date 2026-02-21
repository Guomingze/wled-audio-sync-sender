#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_NAME="WLED音频同步发送器"
APP_ID="local.wled.app"
BUILD_APP="target/dist/$APP_NAME.app"
INSTALL_APP="/Applications/$APP_NAME.app"
APP_ICON_ICNS="target/dist/$APP_NAME.icns"

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
export JAVA_HOME

cd "$ROOT_DIR"

"$JAVA_HOME/bin/java" -version >/dev/null

mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/app-libs

python3 scripts/generate-macos-icon.py --output "$APP_ICON_ICNS" --workdir target/dist

APP_JAR=""
for jar in target/wled-audio-sync-sender-*.jar; do
  if [ -f "$jar" ]; then
    APP_JAR="$jar"
    break
  fi
done

if [ -z "$APP_JAR" ]; then
  echo "构建失败：未找到应用 JAR。"
  exit 1
fi

cp "$APP_JAR" target/app-libs/
rm -rf "$BUILD_APP"

jpackage \
  --type app-image \
  --dest target/dist \
  --input target/app-libs \
  --name "$APP_NAME" \
  --icon "$APP_ICON_ICNS" \
  --main-jar "$(basename "$APP_JAR")" \
  --main-class local.wled.app.WledAudioSyncSender \
  --java-options "-Xdock:name=$APP_NAME" \
  --java-options "-Dapple.awt.application.name=$APP_NAME"

PLIST="$BUILD_APP/Contents/Info.plist"
if [ -f "$PLIST" ]; then
  /usr/libexec/PlistBuddy -c "Set :CFBundleName $APP_NAME" "$PLIST" || true
  /usr/libexec/PlistBuddy -c "Set :CFBundleDisplayName $APP_NAME" "$PLIST" || \
    /usr/libexec/PlistBuddy -c "Add :CFBundleDisplayName string $APP_NAME" "$PLIST"
fi

rm -rf "$INSTALL_APP"
cp -R "$BUILD_APP" "$INSTALL_APP"

codesign --force --deep --sign - "$INSTALL_APP"
tccutil reset Microphone "$APP_ID" || true

open "$INSTALL_APP"
