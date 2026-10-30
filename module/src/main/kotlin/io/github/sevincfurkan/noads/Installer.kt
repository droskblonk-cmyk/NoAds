package io.github.sevincfurkan.noads

import io.github.sevincfurkan.noads.core.AdHook
import io.github.sevincfurkan.noads.core.Log
import io.github.sevincfurkan.noads.core.Settings
import io.github.sevincfurkan.noads.sdk.AdMob
import io.github.sevincfurkan.noads.sdk.AppLovinMax
import io.github.sevincfurkan.noads.sdk.LevelPlay
import io.github.sevincfurkan.noads.sdk.UnityAds
import java.util.Collections
import java.util.WeakHashMap

/**
 * Installs each SDK's hooks once per process, from whichever class loader first exposes that SDK. Packed
 * apps only reveal their real class loader once their Application starts, so this may be called again.
 */
internal class Installer(private val packageName: String, private val settings: Settings) {
    private val installed = mutableSetOf<String>()
    private val seenLoaders: MutableSet<ClassLoader> = Collections.newSetFromMap(WeakHashMap())

    @Synchronized
    fun install(classLoader: ClassLoader) {
        if (!seenLoaders.add(classLoader)) return
        for (sdk in SDKS) {
            if (sdk.name in installed) continue
            try {
                if (sdk.install(classLoader, settings)) {
                    installed += sdk.name
                    Log.i("$packageName: ${sdk.name} hooks installed")
                }
            } catch (t: Throwable) {
                // The SDK is there but hooking failed half way. Don't retry from another loader:
                // a second set of hooks on the same methods would deliver callbacks twice.
                installed += sdk.name
                Log.e("$packageName: installing ${sdk.name} hooks failed", t)
            }
        }
    }

    private companion object {
        val SDKS: List<AdHook> = listOf(AppLovinMax, LevelPlay, AdMob, UnityAds)
    }
}
