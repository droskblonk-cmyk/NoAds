# Test emulator

End-to-end checks need an Android with root and an Xposed framework. These steps set up `NoAds_Test`:
Android 15 with Google Play, rooted with Magisk, running [Vector](https://github.com/JingMatrix/Vector)
(LSPosed's maintained successor). They only touch that emulator and its own system image.

Commands below assume `adb` is on your `PATH` and `ANDROID_SERIAL=emulator-5584`.

## 1. Create and start the emulator

```bash
tools/emulator/create-avd.sh
```

```bash
~/Library/Android/sdk/emulator/emulator -avd NoAds_Test -port 5584 -no-snapshot
```

## 2. Root it with Magisk

Get [rootAVD](https://gitlab.com/newbit/rootAVD). Its bundled Magisk (26.4) predates Android 15, so
replace `Magisk.zip` in the rootAVD folder with the latest
[Magisk](https://github.com/topjohnwu/Magisk/releases) APK, renamed to `Magisk.zip`. With the emulator
running:

```bash
./rootAVD.sh system-images/android-35/google_apis_playstore/arm64-v8a/ramdisk.img
```

It patches the image's ramdisk (keeping `ramdisk.img.backup`), installs the Magisk app and shuts the
emulator down. Start it again with the same command as in step 1, open Magisk and accept "Requires
additional setup"; the emulator reboots. To undo the root:
`./rootAVD.sh system-images/android-35/google_apis_playstore/arm64-v8a/ramdisk.img restore`.

## 3. Give adb shell root

On a production image Magisk rejects `su` from adb shell without asking (`su: request rejected (2000)`
in logcat). In the Magisk app, open **Superuser**, tap **+** and tick **Shell**. Check with:

```bash
adb shell su -c id
```

## 4. Install Vector

Check that **Zygisk** is on in Magisk's settings. Download the Release zip from
[Vector's releases](https://github.com/JingMatrix/Vector/releases), then:

```bash
adb push Vector-v2.2-3080-Release.zip /data/local/tmp/vector.zip
```

```bash
adb shell su -c "magisk --install-module /data/local/tmp/vector.zip"
```

```bash
adb reboot
```

## 5. Install NoAds and the sample, then enable the module

```bash
./gradlew :module:installDebug :sample:installDebug
```

Vector ships a CLI, so no tapping through the manager is needed:

```bash
adb shell su -c "/data/adb/modules/zygisk_vector/cli modules enable io.github.sevincfurkan.noads"
```

```bash
adb shell su -c "/data/adb/modules/zygisk_vector/cli scope add io.github.sevincfurkan.noads io.github.sevincfurkan.noads.sample/0"
```

## 6. Run the scenarios

```bash
adb shell am start -S -n io.github.sevincfurkan.noads.sample/.MainActivity --es run rewarded
```

```bash
adb logcat -s NoAdsSample:I NoAds:I
```

With NoAds active you should see `AdMob hooks installed`, then `AdMob rewarded: ad skipped, …` and
`earned 10 coins … ms after show()` within a few milliseconds, with no `impression` line. The other
scenarios are `rewarded_interstitial`, `interstitial`, `app_open` and `banner`. To see the real ad for
comparison, switch the format off in the NoAds app, or disable the module with
`cli modules disable io.github.sevincfurkan.noads`.

## Real games

Sign in to the Play Store on the emulator yourself, install an offline game, and add it to the scope:

```bash
adb shell su -c "/data/adb/modules/zygisk_vector/cli scope add io.github.sevincfurkan.noads com.example.game/0"
```

Force stop the game, open it, and watch `adb logcat -s NoAds:I`. Some ad networks don't serve ads to
emulators at all; when the game says no ad is available, try a rooted phone instead.
