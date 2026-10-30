package io.github.sevincfurkan.noads.sdk

import io.github.sevincfurkan.noads.OldMaxListener
import io.github.sevincfurkan.noads.Recorder
import io.github.sevincfurkan.noads.core.Step
import io.github.sevincfurkan.noads.core.runAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowsTest {
    private val log = mutableListOf<String>()

    private fun List<Step>.run() = runAll { _, error -> throw error }

    @After
    fun neverReportsImpressionsClicksOrRevenue() {
        val forbidden = log.filter { call -> Recorder.FORBIDDEN.any { call.startsWith(it) } }
        assertEquals("forbidden callbacks were delivered", emptyList<String>(), forbidden)
    }

    @Test
    fun `AdMob rewarded opens, rewards, then closes`() {
        AdMobFlows.completed(Recorder(log), Recorder(log), reward = "10 coins").run()

        assertEquals(
            listOf("onAdShowedFullScreenContent", "onUserEarnedReward(10 coins)", "onAdDismissedFullScreenContent"),
            log,
        )
    }

    @Test
    fun `AdMob formats without a reward only open and close`() {
        AdMobFlows.completed(Recorder(log), rewardListener = null, reward = null).run()

        assertEquals(listOf("onAdShowedFullScreenContent", "onAdDismissedFullScreenContent"), log)
    }

    @Test
    fun `AdMob reward still arrives when the app set no full-screen callback`() {
        AdMobFlows.completed(fullScreenCallback = null, Recorder(log), reward = "10 coins").run()

        assertEquals(listOf("onUserEarnedReward(10 coins)"), log)
    }

    @Test
    fun `MAX rewarded displays, rewards, then hides`() {
        MaxFlows.completed(Recorder(log), ad = "ad", reward = "reward").run()

        assertEquals(listOf("onAdDisplayed(ad)", "onUserRewarded(ad, reward)", "onAdHidden(ad)"), log)
    }

    @Test
    fun `MAX also delivers the video callbacks of older SDKs when the game implements them`() {
        MaxFlows.completed(OldMaxListener(log), ad = "ad", reward = "reward").run()

        assertEquals(
            listOf(
                "onAdDisplayed(ad)",
                "onRewardedVideoStarted(ad)",
                "onRewardedVideoCompleted(ad)",
                "onUserRewarded(ad, reward)",
                "onAdHidden(ad)",
            ),
            log,
        )
    }

    @Test
    fun `MAX formats without a reward display then hide`() {
        MaxFlows.completed(Recorder(log), ad = "ad", reward = null).run()

        assertEquals(listOf("onAdDisplayed(ad)", "onAdHidden(ad)"), log)
    }

    @Test
    fun `Unity Ads starts then completes`() {
        UnityAdsFlows.completed(Recorder(log), "Rewarded_Android", completedState = "COMPLETED").run()

        assertEquals(
            listOf("onUnityAdsShowStart(Rewarded_Android)", "onUnityAdsShowComplete(Rewarded_Android, COMPLETED)"),
            log,
        )
    }

    @Test
    fun `LevelPlay rewarded ad unit displays, rewards, then closes`() {
        LevelPlayFlows.completed(Recorder(log), adInfo = "info", reward = "reward").run()

        assertEquals(listOf("onAdDisplayed(info)", "onAdRewarded(reward, info)", "onAdClosed(info)"), log)
    }

    @Test
    fun `LevelPlay interstitial ad unit displays then closes`() {
        LevelPlayFlows.completed(Recorder(log), adInfo = "info", reward = null).run()

        assertEquals(listOf("onAdDisplayed(info)", "onAdClosed(info)"), log)
    }

    @Test
    fun `LevelPlay static API rewarded opens, rewards, then closes`() {
        LevelPlayFlows.legacyRewarded(Recorder(log), placement = "placement", adInfo = "info").run()

        assertEquals(listOf("onAdOpened(info)", "onAdRewarded(placement, info)", "onAdClosed(info)"), log)
    }

    @Test
    fun `LevelPlay static API interstitial opens, succeeds, then closes`() {
        LevelPlayFlows.legacyInterstitial(Recorder(log), adInfo = "info").run()

        assertEquals(listOf("onAdOpened(info)", "onAdShowSucceeded(info)", "onAdClosed(info)"), log)
    }
}
