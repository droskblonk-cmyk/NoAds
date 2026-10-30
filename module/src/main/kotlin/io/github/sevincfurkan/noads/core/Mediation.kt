package io.github.sevincfurkan.noads.core

/**
 * Packages of mediation SDKs. Their adapters call into other ad SDKs on the app's behalf and pass the "ad
 * shown" callback back to their own servers as an impression.
 */
private val MEDIATION_PACKAGES = listOf(
    "com.applovin.", // AppLovin MAX
    "com.ironsource.", // LevelPlay (legacy packages)
    "com.unity3d.mediation.", // LevelPlay
    "com.google.ads.mediation.", // AdMob mediation adapters
    "admost.sdk.", // AdMost
    "com.appodeal.",
    "com.anythink.", // TopOn
    "com.tradplus.",
    "com.yandex.mobile.ads.",
    "com.fyber.",
    "com.chartboost.",
)

/**
 * True when the current call comes from a mediation SDK (or one of [extraPackages]) rather than from the app.
 * Hooks on ad networks leave such calls alone: completing them would make the mediation SDK report an
 * impression for an ad nobody saw. When NoAds supports that mediation SDK, the ad is skipped one level up.
 */
internal fun calledByMediation(vararg extraPackages: String): Boolean =
    Thread.currentThread().stackTrace.any { frame ->
        MEDIATION_PACKAGES.any(frame.className::startsWith) || extraPackages.any(frame.className::startsWith)
    }
