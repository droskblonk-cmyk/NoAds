#!/usr/bin/env bash
# Creates the NoAds_Test emulator: Android 15 with Google Play, 4 GB of RAM.
#
# It uses a system image of its own on purpose: rootAVD patches the ramdisk inside the SDK's system image
# folder, so every other emulator built on the same image would be rooted too.
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ABI="${ABI:-arm64-v8a}" # use x86_64 on Intel machines
IMAGE="system-images;android-35;google_apis_playstore;$ABI"
AVD=NoAds_Test

"$SDK/cmdline-tools/latest/bin/sdkmanager" "$IMAGE"
echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd -n "$AVD" -k "$IMAGE" -d pixel_9
sed -i.bak 's/^hw.ramSize=.*/hw.ramSize=4G/' "$HOME/.android/avd/$AVD.avd/config.ini"

echo "Created $AVD. Start it with:"
echo "  $SDK/emulator/emulator -avd $AVD -port 5584 -no-snapshot"
