package io.github.sevincfurkan.noads.sample

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

/**
 * A tiny "game" that earns coins from Google's test ads, used to check NoAds end to end. With the module
 * active every ad is skipped and the reward arrives a few milliseconds after show(); without it the real
 * test ad plays.
 *
 * Scriptable: `adb shell am start -n io.github.sevincfurkan.noads.sample/.MainActivity --es run rewarded`
 * (or rewarded_interstitial, interstitial, app_open, banner). Results are logged under the NoAdsSample tag.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var coinsView: TextView
    private lateinit var logView: TextView
    private lateinit var bannerContainer: FrameLayout
    private var coins = 0
    private var sdkReady = false
    private var pendingScenario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coinsView = findViewById(R.id.coins)
        logView = findViewById(R.id.log)
        bannerContainer = findViewById(R.id.banner_container)
        showCoins()

        for ((id, scenario) in listOf(
            R.id.rewarded to REWARDED,
            R.id.rewarded_interstitial to REWARDED_INTERSTITIAL,
            R.id.interstitial to INTERSTITIAL,
            R.id.app_open to APP_OPEN,
            R.id.banner to BANNER,
        )) {
            findViewById<Button>(id).setOnClickListener { run(scenario) }
        }

        MobileAds.initialize(this) {
            sdkReady = true
            log("Mobile Ads SDK ready")
            pendingScenario?.let { pendingScenario = null; run(it) }
        }
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val scenario = intent.getStringExtra("run") ?: return
        if (sdkReady) run(scenario) else pendingScenario = scenario
    }

    private fun run(scenario: String) {
        log("$scenario: loading")
        when (scenario) {
            REWARDED -> RewardedAd.load(this, REWARDED_UNIT, request(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    val measure = Measure(scenario)
                    ad.fullScreenContentCallback = measure.callback
                    ad.show(this@MainActivity, measure::rewarded)
                }

                override fun onAdFailedToLoad(error: LoadAdError) = loadFailed(scenario, error)
            })

            REWARDED_INTERSTITIAL -> RewardedInterstitialAd.load(this, REWARDED_INTERSTITIAL_UNIT, request(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        val measure = Measure(scenario)
                        ad.fullScreenContentCallback = measure.callback
                        ad.show(this@MainActivity, measure::rewarded)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) = loadFailed(scenario, error)
                })

            INTERSTITIAL -> InterstitialAd.load(this, INTERSTITIAL_UNIT, request(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = Measure(scenario).callback
                    ad.show(this@MainActivity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) = loadFailed(scenario, error)
            })

            APP_OPEN -> AppOpenAd.load(this, APP_OPEN_UNIT, request(), object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.fullScreenContentCallback = Measure(scenario).callback
                    ad.show(this@MainActivity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) = loadFailed(scenario, error)
            })

            BANNER -> showBanner()
            else -> log("unknown scenario '$scenario'")
        }
    }

    private fun showBanner() {
        bannerContainer.removeAllViews()
        val adView = AdView(this)
        adView.adUnitId = BANNER_UNIT
        adView.setAdSize(AdSize.BANNER)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() = log("$BANNER: loaded")
            override fun onAdFailedToLoad(error: LoadAdError) = loadFailed(BANNER, error)
            override fun onAdImpression() = log("$BANNER: impression")
        }
        bannerContainer.addView(adView)
        adView.loadAd(request())
        log("$BANNER: loadAd() returned, view is ${if (adView.visibility == View.GONE) "hidden" else "visible"}")
    }

    /** Times every callback from the moment the ad is shown. */
    private inner class Measure(private val scenario: String) {
        private val shownAt = SystemClock.elapsedRealtime()

        private fun elapsed() = "${SystemClock.elapsedRealtime() - shownAt} ms after show()"

        val callback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = log("$scenario: showed ${elapsed()}")
            override fun onAdImpression() = log("$scenario: impression ${elapsed()}")
            override fun onAdClicked() = log("$scenario: clicked ${elapsed()}")
            override fun onAdDismissedFullScreenContent() = log("$scenario: dismissed ${elapsed()}")
            override fun onAdFailedToShowFullScreenContent(error: AdError) =
                log("$scenario: failed to show: ${error.message}")
        }

        fun rewarded(reward: RewardItem) {
            coins += reward.amount
            showCoins()
            log("$scenario: earned ${reward.amount} ${reward.type} ${elapsed()}")
        }
    }

    private fun request() = AdRequest.Builder().build()

    private fun loadFailed(scenario: String, error: LoadAdError) = log("$scenario: load failed: ${error.message}")

    private fun showCoins() {
        coinsView.text = getString(R.string.coins, coins)
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        logView.append("$message\n")
    }

    private companion object {
        const val TAG = "NoAdsSample"

        const val REWARDED = "rewarded"
        const val REWARDED_INTERSTITIAL = "rewarded_interstitial"
        const val INTERSTITIAL = "interstitial"
        const val APP_OPEN = "app_open"
        const val BANNER = "banner"

        // Google's public demo ad units: https://developers.google.com/admob/android/test-ads
        const val REWARDED_UNIT = "ca-app-pub-3940256099942544/5224354917"
        const val REWARDED_INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/5354046379"
        const val INTERSTITIAL_UNIT = "ca-app-pub-3940256099942544/1033173712"
        const val APP_OPEN_UNIT = "ca-app-pub-3940256099942544/9257395921"
        const val BANNER_UNIT = "ca-app-pub-3940256099942544/6300978111"
    }
}
