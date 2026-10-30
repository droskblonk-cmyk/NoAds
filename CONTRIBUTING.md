# Contributing to NoAds

Thanks for helping. Most contributions are support for another ad SDK, a fix for an SDK update, or a
report that a game doesn't work. For the last one, open an issue with the game, its version, and the
`NoAds:` lines from `adb logcat -s NoAds` (turn on **Detailed logging** in the NoAds app first).

## Layout

```
module/src/main/kotlin/io/github/sevincfurkan/noads/
├── ModuleEntry.kt      LSPosed entry point (assets/xposed_init)
├── Installer.kt        installs each SDK's hooks once per process; the list of supported SDKs
├── core/               hook and reflection helpers, settings, logging
├── sdk/Flows.kt        every callback NoAds delivers, per SDK; the one file to audit
├── sdk/<Sdk>.kt        one file per SDK: finds its classes and hooks show/load
└── ui/                 the settings screen
sample/                 a tiny AdMob "game" with Google's test ads, for end-to-end checks
```

The module never links against an ad SDK. Every SDK class is reached by name through the hooked app's
class loader, so one APK works with any SDK version that keeps the same public API.

## Adding an SDK

1. **Read its public API from the binary**, not from the docs:
   `javap -cp classes.jar com.example.ads.RewardedAd`. Find how the game shows a full-screen ad, how it
   learns the ad closed and was rewarded, and how banners load.
2. **Add the flow to `Flows.kt`**: the callbacks a completed ad delivers, in the SDK's order. Only
   "opened", "reward earned" and "closed" style callbacks. Never impression, click, paid or revenue ones.
3. **Write `sdk/<Sdk>.kt`** implementing `AdHook`:
   - Return `false` without hooking anything when the SDK's classes aren't there.
   - Replace `show` with `replace { … }` and finish with `deliver(…)`. Return `CallOriginal` whenever
     something is missing or unexpected: the real ad showing is the safe failure.
   - Check `settings.isEnabled(type)` inside the hook so changes apply without a restart.
   - For an ad network that mediation SDKs also call, return `CallOriginal` when `calledByMediation()`.
   - Banners: `blockBanner(…)` on the load methods.
4. **Register it** in `Installer.SDKS`.
5. **Test it**:
   - Add the SDK to `gradle/libs.versions.toml` and to the `sdkContracts` configuration in
     `module/build.gradle.kts`, then add a test to `SdkContractTest` pinning every class, method and callback
     descriptor you use.
   - Add a flow test to `FlowsTest`. `Recorder` fails the test if a forbidden callback is called.
   - If you can, try it in a real game and say which one in the pull request.

## Checks before a pull request

```bash
./gradlew :module:testDebugUnitTest :module:assembleRelease :sample:assembleDebug
```

By contributing you agree that your contribution is licensed under GPL-3.0.
