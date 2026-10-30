package io.github.sevincfurkan.noads.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks every SDK class, method, field and callback NoAds relies on against the real SDK binaries (versions
 * in gradle/libs.versions.toml). When an SDK update renames or removes one, this fails, instead of NoAds
 * quietly falling back to showing the ads.
 *
 * Callbacks are pinned with their JVM descriptors because the flows in Flows.kt call them by name and shape.
 */
class SdkContractTest {
    private val current = SdkBinaries.fromSystemProperty("noads.sdkJars")
    private val legacy = SdkBinaries.fromSystemProperty("noads.legacySdkJars")

    @Test
    fun admob() = current.check {
        val fullScreenCallback = "Lcom/google/android/gms/ads/FullScreenContentCallback;"
        for (format in listOf(AdMob.REWARDED, AdMob.REWARDED_INTERSTITIAL, AdMob.INTERSTITIAL, AdMob.AD_MANAGER_INTERSTITIAL, AdMob.APP_OPEN)) {
            method(format, "load")
            method(format, "show")
            method(format, "getFullScreenContentCallback", "()$fullScreenCallback")
        }
        for (format in listOf(AdMob.REWARDED, AdMob.REWARDED_INTERSTITIAL)) {
            method(format, "show", "(Landroid/app/Activity;Lcom/google/android/gms/ads/OnUserEarnedRewardListener;)V")
            method(format, "getRewardItem", "()Lcom/google/android/gms/ads/rewarded/RewardItem;")
        }
        method(AdMob.LOAD_CALLBACK, "onAdLoaded", "(Ljava/lang/Object;)V")
        method("com.google.android.gms.ads.FullScreenContentCallback", "onAdShowedFullScreenContent", "()V")
        method("com.google.android.gms.ads.FullScreenContentCallback", "onAdDismissedFullScreenContent", "()V")
        method(
            "com.google.android.gms.ads.OnUserEarnedRewardListener",
            "onUserEarnedReward",
            "(Lcom/google/android/gms/ads/rewarded/RewardItem;)V",
        )
        field(AdMob.REWARD_ITEM, "DEFAULT_REWARD")
        method(AdMob.BASE_AD_VIEW, "loadAd", "(Lcom/google/android/gms/ads/AdRequest;)V")
        method(AdMob.AD_MANAGER_AD_VIEW, "loadAd", "(Lcom/google/android/gms/ads/admanager/AdManagerAdRequest;)V")
    }

    @Test
    fun appLovinMax() = current.check {
        val maxAd = "Lcom/applovin/mediation/MaxAd;"
        for (format in listOf(AppLovinMax.REWARDED, AppLovinMax.INTERSTITIAL, AppLovinMax.APP_OPEN)) {
            method(format, "setListener")
            method(format, "showAd")
            method(format, "isReady", "()Z")
        }
        method("com.applovin.mediation.MaxAdListener", "onAdLoaded", "($maxAd)V")
        method("com.applovin.mediation.MaxAdListener", "onAdDisplayed", "($maxAd)V")
        method("com.applovin.mediation.MaxAdListener", "onAdHidden", "($maxAd)V")
        method("com.applovin.mediation.MaxRewardedAdListener", "onUserRewarded", "(${maxAd}Lcom/applovin/mediation/MaxReward;)V")
        method(AppLovinMax.REWARD, "getLabel", "()Ljava/lang/String;")
        method(AppLovinMax.REWARD, "getAmount", "()I")
        field(AppLovinMax.REWARD, "DEFAULT_LABEL")
        field(AppLovinMax.REWARD, "DEFAULT_AMOUNT")
        method(AppLovinMax.AD_VIEW, "loadAd", "()V")
        method(AppLovinMax.AD_VIEW, "startAutoRefresh", "()V")
    }

    @Test
    fun unityAds() = current.check {
        val listener = "Lcom/unity3d/ads/IUnityAdsShowListener;"
        method(UnityAds.UNITY_ADS, "show", "(Landroid/app/Activity;Ljava/lang/String;$listener)V")
        method(UnityAds.UNITY_ADS, "show", "(Landroid/app/Activity;Ljava/lang/String;Lcom/unity3d/ads/UnityAdsShowOptions;$listener)V")
        method(UnityAds.SHOW_LISTENER, "onUnityAdsShowStart", "(Ljava/lang/String;)V")
        method(
            UnityAds.SHOW_LISTENER,
            "onUnityAdsShowComplete",
            "(Ljava/lang/String;Lcom/unity3d/ads/UnityAds\$UnityAdsShowCompletionState;)V",
        )
        field(UnityAds.COMPLETION_STATE, "COMPLETED")
        method(UnityAds.BANNER_VIEW, "load", "()V")
    }

    @Test
    fun levelPlayAdUnits() = current.check {
        adUnitApi()
        val reward = "Lcom/unity3d/mediation/rewarded/LevelPlayReward;"
        method(LevelPlay.REWARDED_AD, "getReward", "(Ljava/lang/String;)$reward")
        method(LevelPlay.REWARDED_AD, "getReward", "()$reward")
        method(LevelPlay.BANNER_VIEW, "loadAd", "()V")
    }

    @Test
    fun levelPlayStaticApi() = legacy.check {
        // SDK 8 also has the ad unit API, but no getReward(); LevelPlay.kt then reads the placement instead.
        adUnitApi()
        val adInfo = "Lcom/ironsource/mediationsdk/adunit/adapter/utility/AdInfo;"
        val placement = "Lcom/ironsource/mediationsdk/model/Placement;"
        for (name in listOf(
            "setLevelPlayRewardedVideoListener",
            "setLevelPlayRewardedVideoManualListener",
            "showRewardedVideo",
            "setLevelPlayInterstitialListener",
            "showInterstitial",
            "loadBanner",
        )) {
            method(LevelPlay.IRON_SOURCE, name)
        }
        method(LevelPlay.IRON_SOURCE, "isRewardedVideoAvailable", "()Z")
        method(LevelPlay.IRON_SOURCE, "isInterstitialReady", "()Z")
        method(LevelPlay.IRON_SOURCE, "getRewardedVideoPlacementInfo", "(Ljava/lang/String;)$placement")
        method(placement.className(), "getRewardName", "()Ljava/lang/String;")
        method(placement.className(), "getRewardAmount", "()I")

        val rewarded = "com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener"
        method(rewarded, "onAdAvailable", "($adInfo)V")
        method(rewarded, "onAdOpened", "($adInfo)V")
        method(rewarded, "onAdRewarded", "($placement$adInfo)V")
        method(rewarded, "onAdClosed", "($adInfo)V")
        method("com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoManualListener", "onAdReady", "($adInfo)V")

        val interstitial = "com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener"
        for (callback in listOf("onAdReady", "onAdOpened", "onAdShowSucceeded", "onAdClosed")) {
            method(interstitial, callback, "($adInfo)V")
        }
    }

    @Test
    fun `the checker reports what an SDK dropped`() {
        // LevelPlay 9 removed the static show methods, which is why levelPlayStaticApi checks SDK 8.
        val checker = Checker(current).apply {
            method(LevelPlay.IRON_SOURCE, "showRewardedVideo")
            field(AdMob.REWARD_ITEM, "NO_SUCH_FIELD")
            method("com.example.Missing", "show")
        }

        assertEquals(
            listOf(
                "${LevelPlay.IRON_SOURCE}.showRewardedVideo",
                "${AdMob.REWARD_ITEM}#NO_SUCH_FIELD",
                "class com.example.Missing",
            ),
            checker.missing,
        )
    }

    /** LevelPlay's ad unit API as both SDK generations have it. */
    private fun Checker.adUnitApi() {
        val adInfo = "Lcom/unity3d/mediation/LevelPlayAdInfo;"
        for (format in listOf(LevelPlay.REWARDED_AD, LevelPlay.INTERSTITIAL_AD)) {
            method(format, "setListener")
            method(format, "showAd")
            method(format, "isAdReady", "()Z")
        }
        val rewarded = "com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener"
        method(rewarded, "onAdLoaded", "($adInfo)V")
        method(rewarded, "onAdDisplayed", "($adInfo)V")
        method(rewarded, "onAdRewarded", "(Lcom/unity3d/mediation/rewarded/LevelPlayReward;$adInfo)V")
        method(rewarded, "onAdClosed", "($adInfo)V")
        val interstitial = "com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener"
        method(interstitial, "onAdLoaded", "($adInfo)V")
        method(interstitial, "onAdDisplayed", "($adInfo)V")
        method(interstitial, "onAdClosed", "($adInfo)V")
        method(LevelPlay.REWARD, "<init>", "(Ljava/lang/String;I)V")
    }

    class Checker(private val sdk: SdkBinaries) {
        val missing = mutableListOf<String>()

        fun method(className: String, name: String, descriptor: String? = null) {
            val methods = sdk.methodsOf(className)
            when {
                methods == null -> missing += "class $className"
                methods.none { it.name == name && (descriptor == null || it.descriptor == descriptor) } ->
                    missing += "$className.$name${descriptor ?: ""}"
            }
        }

        fun field(className: String, name: String) {
            val fields = sdk.fieldsOf(className)
            when {
                fields == null -> missing += "class $className"
                name !in fields -> missing += "$className#$name"
            }
        }
    }

    private fun SdkBinaries.check(block: Checker.() -> Unit) {
        val checker = Checker(this).apply(block)
        assertTrue(
            "Missing from the SDK binaries:\n" + checker.missing.distinct().joinToString("\n"),
            checker.missing.isEmpty(),
        )
    }

    private fun String.className() = removePrefix("L").removeSuffix(";").replace('/', '.')
}
