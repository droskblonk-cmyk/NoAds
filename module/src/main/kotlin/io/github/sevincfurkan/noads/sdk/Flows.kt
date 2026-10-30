package io.github.sevincfurkan.noads.sdk

import io.github.sevincfurkan.noads.core.Step
import io.github.sevincfurkan.noads.core.steps

// The callbacks NoAds delivers instead of showing an ad, per SDK, in the order the SDK itself uses.
//
// This file is the whole answer to "what does NoAds tell the game?". It only ever reports that the ad
// opened, that the reward was earned and that the ad closed. It never calls impression, click, paid-event
// or revenue callbacks: no ad was shown, so nothing may be counted or billed for one.
//
// Kept free of Xposed types so the unit tests can run it against plain fakes.

internal object AdMobFlows {
    /** [rewardListener] and [reward] are null for formats without a reward. */
    fun completed(fullScreenCallback: Any?, rewardListener: Any?, reward: Any?): List<Step> = steps {
        then(fullScreenCallback, "onAdShowedFullScreenContent")
        then(rewardListener, "onUserEarnedReward", reward)
        then(fullScreenCallback, "onAdDismissedFullScreenContent")
    }
}

internal object MaxFlows {
    /** [ad] is the MaxAd from the listener's onAdLoaded; [reward] is null for formats without a reward. */
    fun completed(listener: Any, ad: Any, reward: Any?): List<Step> = steps {
        then(listener, "onAdDisplayed", ad)
        if (reward != null) {
            // Removed in MAX 12 but still implemented by games built against older SDKs.
            thenIfPresent(listener, "onRewardedVideoStarted", ad)
            thenIfPresent(listener, "onRewardedVideoCompleted", ad)
            then(listener, "onUserRewarded", ad, reward)
        }
        then(listener, "onAdHidden", ad)
    }
}

internal object UnityAdsFlows {
    /** [completedState] is `UnityAds.UnityAdsShowCompletionState.COMPLETED`. */
    fun completed(listener: Any, placementId: String?, completedState: Any): List<Step> = steps {
        then(listener, "onUnityAdsShowStart", placementId)
        then(listener, "onUnityAdsShowComplete", placementId, completedState)
    }
}

internal object LevelPlayFlows {
    /** The ad unit API (LevelPlayRewardedAd, LevelPlayInterstitialAd). [reward] is null for interstitials. */
    fun completed(listener: Any, adInfo: Any, reward: Any?): List<Step> = steps {
        then(listener, "onAdDisplayed", adInfo)
        if (reward != null) then(listener, "onAdRewarded", reward, adInfo)
        then(listener, "onAdClosed", adInfo)
    }

    /** The static IronSource API with a LevelPlayRewardedVideo(Manual)Listener (SDK 7.2 to 8.x). */
    fun legacyRewarded(listener: Any, placement: Any, adInfo: Any): List<Step> = steps {
        then(listener, "onAdOpened", adInfo)
        then(listener, "onAdRewarded", placement, adInfo)
        then(listener, "onAdClosed", adInfo)
    }

    /** The static IronSource API with a LevelPlayInterstitialListener (SDK 7.2 to 8.x). */
    fun legacyInterstitial(listener: Any, adInfo: Any): List<Step> = steps {
        then(listener, "onAdOpened", adInfo)
        then(listener, "onAdShowSucceeded", adInfo)
        then(listener, "onAdClosed", adInfo)
    }
}
