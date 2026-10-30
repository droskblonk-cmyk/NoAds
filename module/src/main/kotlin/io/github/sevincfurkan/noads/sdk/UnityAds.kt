package io.github.sevincfurkan.noads.sdk

import io.github.sevincfurkan.noads.core.AdHook
import io.github.sevincfurkan.noads.core.AdType
import io.github.sevincfurkan.noads.core.CallOriginal
import io.github.sevincfurkan.noads.core.Settings
import io.github.sevincfurkan.noads.core.blockBanner
import io.github.sevincfurkan.noads.core.calledByMediation
import io.github.sevincfurkan.noads.core.deliver
import io.github.sevincfurkan.noads.core.enumConstant
import io.github.sevincfurkan.noads.core.findClassOrNull
import io.github.sevincfurkan.noads.core.hookDeclared
import io.github.sevincfurkan.noads.core.replace

/** Unity Ads (`com.unity3d.ads:unity-ads`) 4.x, called directly by many Unity games. */
internal object UnityAds : AdHook {
    override val name = "Unity Ads"

    const val UNITY_ADS = "com.unity3d.ads.UnityAds"
    const val SHOW_LISTENER = "com.unity3d.ads.IUnityAdsShowListener"
    const val COMPLETION_STATE = "com.unity3d.ads.UnityAds\$UnityAdsShowCompletionState"
    const val BANNER_VIEW = "com.unity3d.services.banners.BannerView"

    override fun install(classLoader: ClassLoader, settings: Settings): Boolean {
        val unityAds = classLoader.findClassOrNull(UNITY_ADS) ?: return false
        val listenerClass = classLoader.findClassOrNull(SHOW_LISTENER)
        val completed = classLoader.findClassOrNull(COMPLETION_STATE)?.enumConstant("COMPLETED")
        if (listenerClass != null && completed != null) {
            unityAds.hookDeclared("show", replace { param ->
                val placementId = param.args.getOrNull(1) as? String
                // One show() serves every format; placements are named after theirs ("Interstitial_Android").
                val type = if (placementId?.contains("inter", ignoreCase = true) == true) AdType.INTERSTITIAL else AdType.REWARDED
                if (!settings.isEnabled(type)) return@replace CallOriginal
                if (calledByMediation("com.google.android.gms.ads.")) return@replace CallOriginal
                // Without a listener nobody hears about completion; leave the call to the SDK.
                val listener = param.args.firstOrNull { listenerClass.isInstance(it) } ?: return@replace CallOriginal
                deliver("$name ${type.key} ($placementId)", UnityAdsFlows.completed(listener, placementId, completed))
                null
            })
        }
        classLoader.findClassOrNull(BANNER_VIEW)?.let { blockBanner(name, it, settings, "load") }
        return true
    }
}
