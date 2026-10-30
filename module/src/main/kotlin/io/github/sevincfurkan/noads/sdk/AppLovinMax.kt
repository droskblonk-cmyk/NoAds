package io.github.sevincfurkan.noads.sdk

import io.github.sevincfurkan.noads.core.AdHook
import io.github.sevincfurkan.noads.core.AdType
import io.github.sevincfurkan.noads.core.CallOriginal
import io.github.sevincfurkan.noads.core.LoadedAds
import io.github.sevincfurkan.noads.core.Settings
import io.github.sevincfurkan.noads.core.before
import io.github.sevincfurkan.noads.core.blockBanner
import io.github.sevincfurkan.noads.core.callOrNull
import io.github.sevincfurkan.noads.core.deliver
import io.github.sevincfurkan.noads.core.findClassOrNull
import io.github.sevincfurkan.noads.core.hookDeclared
import io.github.sevincfurkan.noads.core.implement
import io.github.sevincfurkan.noads.core.replace
import io.github.sevincfurkan.noads.core.staticFieldOrNull

/** AppLovin MAX (`com.applovin:applovin-sdk`), the mediation layer of many casual games. */
internal object AppLovinMax : AdHook {
    override val name = "AppLovin MAX"

    private const val ADS = "com.applovin.mediation.ads"
    const val REWARDED = "$ADS.MaxRewardedAd"
    const val INTERSTITIAL = "$ADS.MaxInterstitialAd"
    const val APP_OPEN = "$ADS.MaxAppOpenAd"
    const val AD_VIEW = "$ADS.MaxAdView"
    const val REWARD = "com.applovin.mediation.MaxReward"

    private class Format(val className: String, val label: String, val type: AdType)

    private val formats = listOf(
        Format(REWARDED, "rewarded", AdType.REWARDED),
        Format(INTERSTITIAL, "interstitial", AdType.INTERSTITIAL),
        Format(APP_OPEN, "app open", AdType.APP_OPEN),
    )

    override fun install(classLoader: ClassLoader, settings: Settings): Boolean {
        var found = false
        for (format in formats) {
            val adClass = classLoader.findClassOrNull(format.className) ?: continue
            found = true
            hookFullScreen(adClass, format, settings, classLoader)
        }
        classLoader.findClassOrNull(AD_VIEW)?.let {
            found = true
            blockBanner(name, it, settings, "loadAd", "startAutoRefresh")
        }
        return found
    }

    private fun hookFullScreen(adClass: Class<*>, format: Format, settings: Settings, classLoader: ClassLoader) {
        val ads = LoadedAds(loadCallbacks = setOf("onAdLoaded"))
        adClass.hookDeclared("setListener", before { it.args[0] = ads.register(it.thisObject, it.args[0]) })

        val what = "$name ${format.label}"
        adClass.hookDeclared("showAd", replace { param ->
            if (!settings.isEnabled(format.type)) return@replace CallOriginal
            val ad = param.thisObject
            if (ad.callOrNull("isReady") != true) return@replace CallOriginal
            val listener = ads.listenerOf(ad) ?: return@replace CallOriginal
            val maxAd = ads.loadedOf(ad) ?: return@replace CallOriginal
            val reward = if (format.type == AdType.REWARDED) defaultReward(classLoader) ?: return@replace CallOriginal else null
            deliver(what, MaxFlows.completed(listener, maxAd, reward))
            null
        })
    }

    /** What MAX passes when the network sets no reward of its own; games typically grant their own amount. */
    private fun defaultReward(classLoader: ClassLoader): Any? {
        val rewardInterface = classLoader.findClassOrNull(REWARD) ?: return null
        return implement(
            rewardInterface,
            "getLabel" to (rewardInterface.staticFieldOrNull("DEFAULT_LABEL") ?: ""),
            "getAmount" to (rewardInterface.staticFieldOrNull("DEFAULT_AMOUNT") ?: 0),
        )
    }
}
