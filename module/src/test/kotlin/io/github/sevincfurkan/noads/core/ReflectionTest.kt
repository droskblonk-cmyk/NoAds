package io.github.sevincfurkan.noads.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionTest {
    interface Greeter {
        fun greet(name: String): String
    }

    open class Base {
        fun twice(x: Int) = x * 2

        @Suppress("unused")
        private fun secret() = "secret"
    }

    class Impl : Base(), Greeter {
        override fun greet(name: String) = "hi $name"

        fun orNull(value: String?) = value ?: "was null"

        fun fail(): Nothing = throw IllegalStateException("app bug")
    }

    @Test
    fun `call finds inherited methods and boxes primitive arguments`() {
        assertEquals(4, Impl().call("twice", 2))
    }

    @Test
    fun `call reaches private methods up the class hierarchy`() {
        assertEquals("secret", Impl().call("secret"))
    }

    @Test
    fun `call passes null to reference parameters`() {
        assertEquals("was null", Impl().call("orNull", null))
    }

    @Test
    fun `call rethrows the app's own exception unwrapped`() {
        val error = assertThrows(IllegalStateException::class.java) { Impl().call("fail") }
        assertEquals("app bug", error.message)
    }

    @Test
    fun `call fails loudly for a missing method and callOrNull quietly`() {
        assertThrows(NoSuchMethodException::class.java) { Impl().call("missing") }
        assertNull(Impl().callOrNull("missing"))
    }

    @Test
    fun `callStatic, enumConstant and staticFieldOrNull read JDK classes`() {
        assertEquals(42, Integer::class.java.callStatic("parseInt", "42"))
        assertSame(Thread.State.RUNNABLE, Thread.State::class.java.enumConstant("RUNNABLE"))
        assertEquals(Int.MAX_VALUE, Integer::class.java.staticFieldOrNull("MAX_VALUE"))
        assertNull(Integer::class.java.staticFieldOrNull("NO_SUCH_FIELD"))
    }

    @Test
    fun `spyOn sees interface calls and forwards them to the target`() {
        val seen = mutableListOf<String>()
        val spy = spyOn(Impl()) { method, args -> seen += "$method(${args.joinToString()})" }

        assertTrue(spy is Greeter)
        assertEquals("hi Ada", (spy as Greeter).greet("Ada"))
        assertEquals(listOf("greet(Ada)"), seen)
    }

    @Test
    fun `spyOn leaves objects without interfaces alone`() {
        val target = Base()
        assertSame(target, spyOn(target) { _, _ -> })
    }
}
