package io.github.sevincfurkan.noads.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LoadedAdsTest {
    interface AdListener {
        fun onAdLoaded(info: Any?)

        fun onAdHidden(info: Any?)
    }

    class GameListener(private val log: MutableList<String>) : AdListener {
        override fun onAdLoaded(info: Any?) {
            log += "loaded $info"
        }

        override fun onAdHidden(info: Any?) {
            log += "hidden $info"
        }
    }

    private val ads = LoadedAds(loadCallbacks = setOf("onAdLoaded"))
    private val log = mutableListOf<String>()

    @Test
    fun `the SDK gets a spy that keeps the loaded ad info and forwards every call`() {
        val ad = Any()
        val game = GameListener(log)

        val spy = ads.register(ad, game) as AdListener
        spy.onAdLoaded("info")
        spy.onAdHidden("info")

        assertEquals(listOf("loaded info", "hidden info"), log)
        assertSame(game, ads.listenerOf(ad))
        assertEquals("info", ads.loadedOf(ad))
    }

    @Test
    fun `each ad keeps its own listener and loaded info`() {
        val first = Any()
        val second = Any()
        val firstListener = GameListener(log)
        val secondListener = GameListener(log)

        (ads.register(first, firstListener) as AdListener).onAdLoaded("first info")
        (ads.register(second, secondListener) as AdListener).onAdLoaded("second info")

        assertSame(firstListener, ads.listenerOf(first))
        assertEquals("first info", ads.loadedOf(first))
        assertSame(secondListener, ads.listenerOf(second))
        assertEquals("second info", ads.loadedOf(second))
    }

    @Test
    fun `clearing the listener forgets the ad`() {
        val ad = Any()
        val game = GameListener(log)
        (ads.register(ad, game) as AdListener).onAdLoaded("info")

        assertNull(ads.register(ad, null))
        assertNull(ads.listenerOf(ad))
        assertNull(ads.loadedOf(ad))
    }
}
