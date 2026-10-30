package io.github.sevincfurkan.noads.sdk

import de.robv.android.xposed.XposedBridge
import io.github.sevincfurkan.noads.core.AdHook
import io.github.sevincfurkan.noads.core.AdType
import io.github.sevincfurkan.noads.core.CallOriginal
import io.github.sevincfurkan.noads.core.Log
import io.github.sevincfurkan.noads.core.Settings
import io.github.sevincfurkan.noads.core.after
import io.github.sevincfurkan.noads.core.before
import io.github.sevincfurkan.noads.core.blockBanner
import io.github.sevincfurkan.noads.core.callOrNull
import io.github.sevincfurkan.noads.core.calledByMediation
import io.github.sevincfurkan.noads.core.deliver
import io.github.sevincfurkan.noads.core.findClassOrNull
import io.github.sevincfurkan.noads.core.hookNearest
import io.github.sevincfurkan.noads.core.replace
import io.github.sevincfurkan.noads.core.staticFieldOrNull
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** Google Mobile Ads SDK (`com.google.android.gms:play-services-ads`), also behind AdMob mediation. */
internal object AdMob : AdHook {
    override val name = "AdMob"

    private const val ADS = "com.google.android.gms.ads"
    const val REWARDED = "$ADS.rewarded.RewardedAd"
    const val REWARDED_INTERSTITIAL = "$ADS.rewardedinterstitial.RewardedInterstitialAd"
    const val INTERSTITIAL = "$ADS.interstitial.InterstitialAd"
    const val AD_MANAGER_INTERSTITIAL = "$ADS.admanager.AdManagerInterstitialAd"
    const val APP_OPEN = "$ADS.appopen.AppOpenAd"
    const val LOAD_CALLBACK = "$ADS.AdLoadCallback"
    const val REWARD_ITEM = "$ADS.rewarded.RewardItem"
    const val BASE_AD_VIEW = "$ADS.BaseAdView"
    const val AD_MANAGER_AD_VIEW = "$ADS.admanager.AdManagerAdView"

    private class Format(val className: String, val label: String, val type: AdType, val rewarded: Boolean)

    private val formats = listOf(
        Format(REWARDED, "rewarded", AdType.REWARDED, rewarded = true),
        Format(REWARDED_INTERSTITIAL, "rewarded interstitial", AdType.REWARDED, rewarded = true),
        Format(INTERSTITIAL, "interstitial", AdType.INTERSTITIAL, rewarded = false),
        Format(AD_MANAGER_INTERSTITIAL, "interstitial", AdType.INTERSTITIAL, rewarded = false),
        Format(APP_OPEN, "app open", AdType.APP_OPEN, rewarded = false),
    )

    private val knownAdClasses: MutableSet<Class<*>> = Collections.newSetFromMap(ConcurrentHashMap())
    private val watchedCallbackClasses: MutableSet<Class<*>> = Collections.newSetFromMap(ConcurrentHashMap())

    override fun install(classLoader: ClassLoader, settings: Settings): Boolean {
        var found = false
        val callbackBase = classLoader.findClassOrNull(LOAD_CALLBACK)
        for (format in formats) {
            val baseClass = classLoader.findClassOrNull(format.className) ?: continue
            found = true
            watchLoadedAds(baseClass, callbackBase) { adClass -> hookShow(adClass, baseClass, format, settings, classLoader) }
        }
        for (viewClass in listOf(BASE_AD_VIEW, AD_MANAGER_AD_VIEW)) {
            classLoader.findClassOrNull(viewClass)?.let {
                found = true
                blockBanner(name, it, settings, "loadAd")
            }
        }
        return found
    }

    /**
     * The full-screen formats are abstract classes implemented by obfuscated SDK classes, so the class whose
     * show() needs hooking is learned at runtime from the first ad the app receives: through the callback
     * passed to load(), or as the return value of pollAd() (the preloading API).
     */
    private fun watchLoadedAds(baseClass: Class<*>, callbackBase: Class<*>?, onNewAdClass: (Class<*>) -> Unit) {
        val discover = { ad: Any? ->
            if (ad != null && baseClass.isInstance(ad) && knownAdClasses.add(ad.javaClass)) onNewAdClass(ad.javaClass)
        }
        for (method in baseClass.declaredMethods) {
            if (!Modifier.isStatic(method.modifiers)) continue
            when {
                method.name == "load" -> XposedBridge.hookMethod(method, before { param ->
                    val callback = param.args.lastOrNull() ?: return@before
                    if (watchedCallbackClasses.add(callback.javaClass)) {
                        callback.javaClass.hookNearest("onAdLoaded", callbackBase, before { discover(it.args.firstOrNull()) })
                    }
                })
                baseClass.isAssignableFrom(method.returnType) ->
                    XposedBridge.hookMethod(method, after { discover(it.result) })
            }
        }
    }

    private fun hookShow(adClass: Class<*>, baseClass: Class<*>, format: Format, settings: Settings, classLoader: ClassLoader) {
        val what = "$name ${format.label}"
        val hooked = adClass.hookNearest("show", baseClass, replace { param ->
            if (!settings.isEnabled(format.type)) return@replace CallOriginal
            if (calledByMediation()) return@replace CallOriginal
            val ad = param.thisObject
            var rewardListener: Any? = null
            var reward: Any? = null
            if (format.rewarded) {
                rewardListener = param.args.getOrNull(1) ?: return@replace CallOriginal
                reward = ad.callOrNull("getRewardItem")
                    ?: classLoader.findClassOrNull(REWARD_ITEM)?.staticFieldOrNull("DEFAULT_REWARD")
                    ?: return@replace CallOriginal
            }
            deliver(what, AdMobFlows.completed(ad.callOrNull("getFullScreenContentCallback"), rewardListener, reward))
            null
        })
        Log.d("$what: hooked show() of ${adClass.name} ($hooked)")
    }
}
