# NoAds

**Rewarded ads end the moment they start, and you still get the reward.**

[Türkçe](README.tr.md)

NoAds is an open-source Xposed module for Android, made for LSPosed and its maintained successor
[Vector](https://github.com/JingMatrix/Vector). When a game
asks its ad SDK to play a rewarded ad ("Watch an ad for 2× coins"), NoAds answers instead of the SDK: the
ad never opens, and a few milliseconds later the game hears "ad finished, reward earned". It also skips
interstitial and app-open ads and keeps banners from loading.

## How it works

Games don't show ads themselves. They hand the job to an ad SDK and wait for its callbacks:

```
game ── show() ──▶ ad SDK ── 30 s video ──▶ "reward earned" ──▶ game gives the reward
```

NoAds hooks `show()` and delivers the SDK's own lifecycle callbacks right away, in the order the SDK uses:

```
game ── show() ──▶ NoAds ── a few ms ──▶ "opened" → "reward earned" → "closed" ──▶ game gives the reward
```

Almost every game uses one of a handful of SDKs, so one module covers thousands of games. Every callback
NoAds can deliver is listed in a single file, [`Flows.kt`](module/src/main/kotlin/io/github/sevincfurkan/noads/sdk/Flows.kt).

## Supported SDKs

| SDK | Rewarded | Interstitial | App open | Banner |
|---|:---:|:---:|:---:|:---:|
| AdMob / Google Mobile Ads | ✓ | ✓ | ✓ | ✓ |
| AppLovin MAX | ✓ | ✓ | ✓ | ✓ |
| Unity Ads | ✓ | ✓ | n/a | ✓ |
| Unity LevelPlay (ironSource): ad unit API and the older static `IronSource` API | ✓ | ✓ | n/a | ✓ |

**Tested so far:** AdMob, all four formats, end to end on Android 15 (emulator with Magisk 31.1 and
Vector 2.2) using the sample app. Rewards arrive 10–25 ms after `show()` instead of after a 9-second
video, and no impression is recorded. AppLovin MAX, Unity Ads and LevelPlay are checked against their SDK
binaries but haven't been tried in real games yet. Reports are welcome.

Every class, method and callback the hooks rely on is checked against the real SDK binaries by
`SdkContractTest`, so an SDK update that renames something fails the build rather than silently
breaking NoAds. Games built with Unity, Flutter or React Native are covered too: they go through the same
Java SDKs. Each format can be switched on or off in the NoAds app.

## What NoAds will never do

- **Fake impressions, clicks or revenue.** The ad is never displayed, so no advertiser pays for it.
  Impression, click, paid-event and revenue callbacks are never called. The tests enforce this.
- **Make a mediation SDK count an ad nobody saw.** Ad network hooks (AdMob, Unity Ads) leave calls that
  come from a mediation SDK's adapter alone. Where NoAds supports the mediation SDK, the ad is skipped one
  level up.
- **Break your game.** If anything is unexpected (an unknown SDK version, missing data), the original
  method runs and you see the real ad.
- **Bypass anti-cheat, root detection or integrity checks.** Games that check those are out of scope.

## Limitations

- **Server-verified rewards don't work.** Many online games grant rewards only when the ad network tells
  the game's server that the ad was watched (server-side verification). NoAds can't fake that, and won't.
  It works where the reward is granted on the device, as in most offline and casual games.
- **Root is required**: Magisk or KernelSU with Zygisk, plus LSPosed or Vector.
- **Terms of service**: skipping ads may break an app's terms. Online games may ban you for it.
- **Android only.** On iOS this would need a jailbreak.

## Install

1. Root your device with [Magisk](https://github.com/topjohnwu/Magisk) or
   [KernelSU](https://kernelsu.org) and enable Zygisk.
2. Install [Vector](https://github.com/JingMatrix/Vector) through Magisk or KernelSU. Vector continues
   LSPosed, which hasn't had a release since 2023 and stops at Android 14.
3. Install the NoAds APK.
4. In the Vector (LSPosed) manager, enable **NoAds** and tick the games it should work in.
5. Force stop those games and open them again.

To switch formats on or off, open **NoAds** from your launcher. Changes apply immediately.

## Build from source

Requires JDK 17 or newer and the Android SDK (platform 36).

```bash
./gradlew :module:assembleDebug
```

```bash
./gradlew :module:testDebugUnitTest
```

The unit tests cover the callback flows, the reflection helpers and the SDK contracts.

### Try it with the sample app

`sample/` is a tiny "game" that earns coins from Google's public test ads. It logs how long after `show()`
each callback arrives:

```bash
./gradlew :sample:installDebug
```

```bash
adb shell am start -n io.github.sevincfurkan.noads.sample/.MainActivity --es run rewarded
```

```bash
adb logcat -s NoAdsSample NoAds
```

With NoAds active you'll see `earned 10 coins 3 ms after show()` and no ad. Without it, the test ad plays.
Other scenarios: `rewarded_interstitial`, `interstitial`, `app_open`, `banner`.

## Contributing

Adding an SDK takes one Kotlin file plus its tests; see [CONTRIBUTING.md](CONTRIBUTING.md).

## Support the games you love

Every skipped rewarded ad is a reward the developer wasn't paid for. If you enjoy a game, buy its
"remove ads" pack or support the studio another way.

## Disclaimer

NoAds is meant for personal use on your own device. It is not affiliated with or endorsed by Google,
AppLovin, Unity or any other company named here; their names and trademarks belong to their owners.
You are responsible for following the terms of the apps you use.

## License

[GPL-3.0](LICENSE). Forks must stay open source, so anyone can check that a build does what it claims.
