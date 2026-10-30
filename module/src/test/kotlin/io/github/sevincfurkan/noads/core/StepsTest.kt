package io.github.sevincfurkan.noads.core

import io.github.sevincfurkan.noads.Recorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepsTest {
    @Test
    fun `then skips a null target`() {
        assertTrue(steps { then(null, "onAdShowedFullScreenContent") }.isEmpty())
    }

    @Test
    fun `thenIfPresent skips callbacks the target doesn't have`() {
        val target = Recorder(mutableListOf())
        val steps = steps {
            thenIfPresent(target, "onAdShowedFullScreenContent")
            thenIfPresent(target, "onRewardedVideoStarted", "ad")
        }

        assertEquals(listOf("onAdShowedFullScreenContent"), steps.map { it.label })
    }

    @Test
    fun `runAll keeps going when the app's callback throws`() {
        val ran = mutableListOf<String>()
        val failed = mutableListOf<String>()

        listOf(
            Step("first") { ran += "first" },
            Step("broken") { error("app bug") },
            Step("last") { ran += "last" },
        ).runAll { step, _ -> failed += step.label }

        assertEquals(listOf("first", "last"), ran)
        assertEquals(listOf("broken"), failed)
    }
}
