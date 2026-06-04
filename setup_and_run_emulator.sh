#!/bin/bash
set -e

SDK_ROOT="/home/shanmuga/android-sdk"

# Set paths
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
ADB="$SDK_ROOT/platform-tools/adb"
EMULATOR="$SDK_ROOT/emulator/emulator"

echo "=== 1. Accepting Android SDK Licenses ==="
yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses

echo "=== 2. Checking/Installing System Image and Emulator ==="
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "emulator" "platform-tools" "system-images;android-34;google_apis;x86_64"

echo "=== 3. Creating Android Virtual Device (AVD) ==="
# Recreate with Google Pixel 6 device profile to ensure high DPI / resolution (1080x2400, 440 dpi)
echo "no" | "$AVDMANAGER" create avd -n Pixel_34 -k "system-images;android-34;google_apis;x86_64" --device "pixel_6" --force
echo "AVD Pixel_34 created with Pixel 6 high-DPI profile."

echo "=== 4. Launching Android Emulator ==="
export DISPLAY=:0
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"

# Force hardware acceleration to use the stable integrated AMD Radeon 680M GPU
# instead of the discrete NVIDIA card using the experimental NVK driver
export DRI_PRIME=0
export MESA_VK_DEVICE_SELECT=1002

if "$ADB" devices | grep -q "emulator"; then
    echo "Emulator is already running."
else
    echo "Starting emulator in background with AMD hardware acceleration..."
    # Launching with GPU host (mapped to stable AMD card), no audio, no snapshot load
    "$EMULATOR" -avd Pixel_34 -no-audio -gpu host -no-snapshot-load > /tmp/emulator.log 2>&1 &
fi

echo "Waiting for emulator to connect via ADB..."
"$ADB" wait-for-device

echo "Waiting for Android system boot to complete..."
boot_completed=""
while [ "$boot_completed" != "1" ]; do
    sleep 3
    boot_completed=$("$ADB" shell getprop sys.boot_completed | tr -d '\r')
    echo "Waiting for UI boot completion..."
done
echo "Emulator boot completed!"

echo "=== 5. Building debug APK ==="
./gradlew assembleDebug

echo "=== 6. Installing APK ==="
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk

echo "=== 7. Launching App ==="
"$ADB" shell am start -n com.bhaai.expensetracker/com.bhaai.expensetracker.presentation.MainActivity

echo "=== Setup complete! Starting logcat filtering for 'com.bhaai.expensetracker' ==="
# Find PID and run logcat
pid=""
for i in {1..10}; do
    pid=$("$ADB" shell pidof com.bhaai.expensetracker | tr -d '\r')
    if [ -n "$pid" ]; then
        break
    fi
    sleep 1
done

if [ -n "$pid" ]; then
    echo "Filtering logs for PID $pid..."
    "$ADB" logcat --pid="$pid"
else
    echo "App PID not found. Showing general logs for package com.bhaai.expensetracker..."
    "$ADB" logcat *:V | grep com.bhaai.expensetracker
fi
