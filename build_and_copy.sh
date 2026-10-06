#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# CDP Learning App - Build APK and Copy to Download Directory
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Building CDP Learning App (Debug APK)..."
echo "=========================================="

# 1. Setup Environment
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH"

echo "✓ JAVA_HOME: $JAVA_HOME"
echo "✓ ANDROID_HOME: $ANDROID_HOME"

# Check if Java is working
if ! command -v java >/dev/null 2>&1; then
    echo "❌ Error: Java not found. Please install: pkg install openjdk-17 or openjdk-21"
    exit 1
fi

# Ensure local.properties exists
if [ ! -f "local.properties" ]; then
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo "✓ Generated local.properties"
fi

# Ensure Android SDK licenses are accepted
mkdir -p "$ANDROID_HOME/licenses"
cat << 'EOF' > "$ANDROID_HOME/licenses/android-sdk-license"
24333f8a63b6825ea9c5514f83c2829b004d1fee
d56f5187479451eabf01fb78af6dfcb131a6481e
84831b9409646a2fe5e12f80164a7fae98628b05
601085b94cd77f0b54ff86406955449fac38bc4d
EOF
cat << 'EOF' > "$ANDROID_HOME/licenses/android-sdk-preview-license"
84831b9409646a2fe5e12f80164a7fae98628b05
504667f4c0de7af1a06de9f4b1727b816f454d0e
EOF
if command -v sdkmanager >/dev/null 2>&1; then
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
    if [ ! -d "$ANDROID_HOME/platforms/android-34" ]; then
        echo "📥 Installing Android SDK Platform 34 via sdkmanager..."
        sdkmanager "platforms;android-34" || true
    fi
    if [ ! -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
        echo "📥 Installing Android SDK Build-Tools 34.0.0 via sdkmanager..."
        sdkmanager "build-tools;34.0.0" || true
    fi
fi
echo "✓ Android SDK packages & licenses verified"

# Ensure Termux ARM64 aapt2 is used
TERMUX_AAPT2="/data/data/com.termux/files/usr/bin/aapt2"
if [ -f "$TERMUX_AAPT2" ]; then
    echo "✓ Patching Gradle caches with native ARM64 aapt2..."
    find "$HOME/.gradle/caches" -name "aapt2" -type f -exec cp -f "$TERMUX_AAPT2" {} + 2>/dev/null || true
    if [ -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
        cp -f "$TERMUX_AAPT2" "$ANDROID_HOME/build-tools/34.0.0/aapt2" 2>/dev/null || true
        chmod +x "$ANDROID_HOME/build-tools/34.0.0/aapt2" 2>/dev/null || true
    fi
fi

# 2. Build Debug APK using Gradle
echo ""
echo "📦 Running Gradle build..."
gradle --stop 2>/dev/null || true
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew assembleDebug --no-daemon
elif command -v gradle >/dev/null 2>&1; then
    gradle assembleDebug --no-daemon
else
    echo "❌ Error: Neither ./gradlew nor system gradle found."
    echo "Please install gradle using: pkg install gradle"
    exit 1
fi

# 3. Locate the generated APK
APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_SOURCE" ]; then
    echo "❌ Build finished but APK was not found at $APK_SOURCE"
    exit 1
fi

APK_SIZE=$(du -h "$APK_SOURCE" | cut -f1)
echo ""
echo "✅ Build Successful! APK generated: $APK_SOURCE ($APK_SIZE)"

# 4. Copy to Android Download folder
TARGET_NAME="CDPLearningApp-debug.apk"
COPIED=false

# Target 1: Direct shared storage Download folder
SHARED_DOWNLOAD="/storage/emulated/0/Download"
if [ -d "$SHARED_DOWNLOAD" ]; then
    cp "$APK_SOURCE" "$SHARED_DOWNLOAD/$TARGET_NAME"
    echo "🎉 Successfully copied to: $SHARED_DOWNLOAD/$TARGET_NAME"
    COPIED=true
fi

# Target 2: Termux storage symlink Download folder
TERMUX_DOWNLOAD="$HOME/storage/downloads"
if [ -d "$TERMUX_DOWNLOAD" ]; then
    cp "$APK_SOURCE" "$TERMUX_DOWNLOAD/$TARGET_NAME"
    echo "🎉 Successfully copied to: $TERMUX_DOWNLOAD/$TARGET_NAME"
    COPIED=true
fi

if [ "$COPIED" = false ]; then
    echo ""
    echo "⚠️ Warning: Storage permission might not be granted in Termux."
    echo "Run 'termux-setup-storage' in your terminal and grant permission, then re-run this script."
    echo "The APK is currently ready at:"
    echo "$SCRIPT_DIR/$APK_SOURCE"
else
    echo ""
    echo "=========================================="
    echo "📲 You can now open your Downloads folder"
    echo "   and install $TARGET_NAME directly!"
    echo "=========================================="
fi
