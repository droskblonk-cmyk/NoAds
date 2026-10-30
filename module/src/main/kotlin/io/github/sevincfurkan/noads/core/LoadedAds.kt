package io.github.sevincfurkan.noads.core

import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/**
 * For SDKs built around listener objects (MAX, LevelPlay): remembers, per ad object, the listener the app set
 * and the ad info the SDK last passed to it on load. Those two are what it takes to deliver the callbacks of
 * an ad that is never shown.
 */
internal class LoadedAds(private val loadCallbacks: Set<String>) {
    private class Entry(listener: Any) {
        val listener = WeakReference(listener)

        @Volatile
        var loaded: Any? = null
    }

    private val entries: MutableMap<Any, Entry> = Collections.synchronizedMap(WeakHashMap())

    /**
     * Records [listener] as the one set on [ad] and returns what to hand to the SDK instead: a spy that
     * forwards every call and keeps the argument of the load callbacks.
     */
    fun register(ad: Any, listener: Any?): Any? {
        if (listener == null) {
            entries.remove(ad)
            return null
        }
        val entry = Entry(listener)
        entries[ad] = entry
        return spyOn(listener) { method, args ->
            if (method in loadCallbacks && args.size == 1) entry.loaded = args[0]
        }
    }

    fun listenerOf(ad: Any): Any? = entries[ad]?.listener?.get()

    fun loadedOf(ad: Any): Any? = entries[ad]?.loaded
}
