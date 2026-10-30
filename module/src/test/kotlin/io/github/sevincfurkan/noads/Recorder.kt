package io.github.sevincfurkan.noads

/**
 * Stands in for any SDK listener or callback (NoAds calls them by name, so the type doesn't matter) and
 * appends every call it receives to a shared [log], which lets tests check ordering across objects.
 */
@Suppress("unused", "UNUSED_PARAMETER")
open class Recorder(private val log: MutableList<String>) {
    protected fun record(call: String) {
        log += call
    }

    // AdMob
    fun onAdShowedFullScreenContent() = record("onAdShowedFullScreenContent")
    fun onUserEarnedReward(reward: Any?) = record("onUserEarnedReward($reward)")
    fun onAdDismissedFullScreenContent() = record("onAdDismissedFullScreenContent")

    // AppLovin MAX, and LevelPlay's ad unit API (same names and shapes)
    fun onAdDisplayed(ad: Any?) = record("onAdDisplayed($ad)")
    fun onUserRewarded(ad: Any?, reward: Any?) = record("onUserRewarded($ad, $reward)")
    fun onAdHidden(ad: Any?) = record("onAdHidden($ad)")
    fun onAdRewarded(reward: Any?, info: Any?) = record("onAdRewarded($reward, $info)")
    fun onAdClosed(info: Any?) = record("onAdClosed($info)")

    // LevelPlay's static IronSource API
    fun onAdOpened(info: Any?) = record("onAdOpened($info)")
    fun onAdShowSucceeded(info: Any?) = record("onAdShowSucceeded($info)")

    // Unity Ads
    fun onUnityAdsShowStart(placementId: String?) = record("onUnityAdsShowStart($placementId)")
    fun onUnityAdsShowComplete(placementId: String?, state: Any?) = record("onUnityAdsShowComplete($placementId, $state)")

    // Callbacks that report impressions, clicks or money. NoAds must never call these.
    fun onAdImpression() = record("onAdImpression")
    fun onAdImpression(ad: Any?) = record("onAdImpression")
    fun onAdClicked() = record("onAdClicked")
    fun onAdClicked(ad: Any?) = record("onAdClicked")
    fun onAdClicked(placement: Any?, info: Any?) = record("onAdClicked")
    fun onUnityAdsShowClick(placementId: String?) = record("onUnityAdsShowClick")
    fun onPaidEvent(value: Any?) = record("onPaidEvent")
    fun onAdRevenuePaid(ad: Any?) = record("onAdRevenuePaid")
    fun onImpressionSuccess(data: Any?) = record("onImpressionSuccess")

    companion object {
        val FORBIDDEN = listOf(
            "onAdImpression", "onAdClicked", "onUnityAdsShowClick", "onPaidEvent", "onAdRevenuePaid", "onImpressionSuccess",
        )
    }
}

/** A MAX listener from a game built against MAX 11 or older, which still had the video callbacks. */
@Suppress("unused")
class OldMaxListener(log: MutableList<String>) : Recorder(log) {
    fun onRewardedVideoStarted(ad: Any?) = record("onRewardedVideoStarted($ad)")
    fun onRewardedVideoCompleted(ad: Any?) = record("onRewardedVideoCompleted($ad)")
}
