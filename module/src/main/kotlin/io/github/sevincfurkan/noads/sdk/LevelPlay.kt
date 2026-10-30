package io.github.sevincfurkan.noads.sdk

import io.github.sevincfurkan.noads.core.AdHook
import io.github.sevincfurkan.noads.core.AdType
import io.github.sevincfurkan.noads.core.CallOriginal
import io.github.sevincfurkan.noads.core.HookParam
import io.github.sevincfurkan.noads.core.LoadedAds
import io.github.sevincfurkan.noads.core.Settings
import io.github.sevincfurkan.noads.core.before
import io.github.sevincfurkan.noads.core.blockBanner
import io.github.sevincfurkan.noads.core.callOrNull
import io.github.sevincfurkan.noads.core.callStaticOrNull
import io.github.sevincfurkan.noads.core.deliver
import io.github.sevincfurkan.noads.core.findClassOrNull
import io.github.sevincfurkan.noads.core.hookDeclared
import io.github.sevincfurkan.noads.core.replace

/**
 * Unity LevelPlay, formerly ironSource (`com.unity3d.ads-mediation:mediation-sdk`): the ad unit API of
 * SDK 8+, and the static IronSource API with LevelPlay listeners that SDK 7.2 to 8.x games use.
 */
internal object LevelPlay : AdHook {
    override val name = "LevelPlay"

    const val REWARDED_AD = "com.unity3d.mediation.rewarded.LevelPlayRewardedAd"
    const val INTERSTITIAL_AD = "com.unity3d.mediation.interstitial.LevelPlayInterstitialAd"
    const val REWARD = "com.unity3d.mediation.rewarded.LevelPlayReward"
    const val BANNER_VIEW = "com.unity3d.mediation.banner.LevelPlayBannerAdView"
    const val IRON_SOURCE = "com.ironsource.mediationsdk.IronSource"

    /** The placement the static API uses when the game names none. */
    private const val DEFAULT_REWARDED_PLACEMENT = "DefaultRewardedVideo"

    // The static API has one listener per format, so a fixed key stands in for the ad object.
    private val legacyRewardedKey = Any()
    private val legacyInterstitialKey = Any()
    private val legacyRewarded = LoadedAds(loadCallbacks = setOf("onAdAvailable", "onAdReady"))
    private val legacyInterstitial = LoadedAds(loadCallbacks = setOf("onAdReady"))

    override fun install(classLoader: ClassLoader, settings: Settings): Boolean {
        var found = false
        classLoader.findClassOrNull(REWARDED_AD)?.let {
            found = true
            hookAdUnit(it, AdType.REWARDED, settings, classLoader)
        }
        classLoader.findClassOrNull(INTERSTITIAL_AD)?.let {
            found = true
            hookAdUnit(it, AdType.INTERSTITIAL, settings, classLoader)
        }
        classLoader.findClassOrNull(BANNER_VIEW)?.let {
            found = true
            blockBanner(name, it, settings, "loadAd")
        }
        classLoader.findClassOrNull(IRON_SOURCE)?.let {
            found = true
            hookStaticApi(it, settings)
        }
        return found
    }

    private fun hookAdUnit(adClass: Class<*>, type: AdType, settings: Settings, classLoader: ClassLoader) {
        val ads = LoadedAds(loadCallbacks = setOf("onAdLoaded"))
        adClass.hookDeclared("setListener", before { it.args[0] = ads.register(it.thisObject, it.args[0]) })

        val what = "$name ${type.key}"
        adClass.hookDeclared("showAd", replace { param ->
            if (!settings.isEnabled(type)) return@replace CallOriginal
            val ad = param.thisObject
            if (ad.callOrNull("isAdReady") != true) return@replace CallOriginal
            val listener = ads.listenerOf(ad) ?: return@replace CallOriginal
            val adInfo = ads.loadedOf(ad) ?: return@replace CallOriginal
            val reward = if (type == AdType.REWARDED) {
                adUnitReward(ad, placementOf(param), classLoader) ?: return@replace CallOriginal
            } else {
                null
            }
            deliver(what, LevelPlayFlows.completed(listener, adInfo, reward))
            null
        })
    }

    /** The reward set up for [placement]: asked from the ad on SDK 9+, read from the placement on SDK 8. */
    private fun adUnitReward(ad: Any, placement: String?, classLoader: ClassLoader): Any? {
        val reward = if (placement != null) ad.callOrNull("getReward", placement) else ad.callOrNull("getReward")
        if (reward != null) return reward
        val ironSource = classLoader.findClassOrNull(IRON_SOURCE) ?: return null
        val placementInfo = ironSource.callStaticOrNull("getRewardedVideoPlacementInfo", placement ?: DEFAULT_REWARDED_PLACEMENT)
            ?: return null
        return try {
            classLoader.findClassOrNull(REWARD)
                ?.getConstructor(String::class.java, Int::class.javaPrimitiveType)
                ?.newInstance(placementInfo.callOrNull("getRewardName"), placementInfo.callOrNull("getRewardAmount"))
        } catch (_: Exception) {
            null
        }
    }

    private fun hookStaticApi(ironSource: Class<*>, settings: Settings) {
        for (setter in listOf("setLevelPlayRewardedVideoListener", "setLevelPlayRewardedVideoManualListener")) {
            ironSource.hookDeclared(setter, before { it.args[0] = legacyRewarded.register(legacyRewardedKey, it.args[0]) })
        }
        ironSource.hookDeclared("setLevelPlayInterstitialListener", before {
            it.args[0] = legacyInterstitial.register(legacyInterstitialKey, it.args[0])
        })

        ironSource.hookDeclared("showRewardedVideo", replace { param ->
            if (!settings.isEnabled(AdType.REWARDED)) return@replace CallOriginal
            if (ironSource.callStaticOrNull("isRewardedVideoAvailable") != true) return@replace CallOriginal
            val listener = legacyRewarded.listenerOf(legacyRewardedKey) ?: return@replace CallOriginal
            val adInfo = legacyRewarded.loadedOf(legacyRewardedKey) ?: return@replace CallOriginal
            val placement = ironSource.callStaticOrNull(
                "getRewardedVideoPlacementInfo",
                placementOf(param) ?: DEFAULT_REWARDED_PLACEMENT,
            ) ?: return@replace CallOriginal
            deliver("$name rewarded (IronSource API)", LevelPlayFlows.legacyRewarded(listener, placement, adInfo))
            null
        })

        ironSource.hookDeclared("showInterstitial", replace { _ ->
            if (!settings.isEnabled(AdType.INTERSTITIAL)) return@replace CallOriginal
            if (ironSource.callStaticOrNull("isInterstitialReady") != true) return@replace CallOriginal
            val listener = legacyInterstitial.listenerOf(legacyInterstitialKey) ?: return@replace CallOriginal
            val adInfo = legacyInterstitial.loadedOf(legacyInterstitialKey) ?: return@replace CallOriginal
            deliver("$name interstitial (IronSource API)", LevelPlayFlows.legacyInterstitial(listener, adInfo))
            null
        })

        blockBanner(name, ironSource, settings, "loadBanner", viewOf = { it.args.firstOrNull() })
    }

    private fun placementOf(param: HookParam): String? = param.args.firstOrNull { it is String } as String?
}
