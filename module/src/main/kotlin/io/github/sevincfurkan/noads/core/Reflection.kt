package io.github.sevincfurkan.noads.core

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

// The module never links against ad SDKs: every SDK and app class is reached by name through the hooked
// app's class loader. These helpers keep that reflection short and failure-tolerant.

internal fun ClassLoader.findClassOrNull(name: String): Class<*>? = try {
    Class.forName(name, false, this)
} catch (_: ClassNotFoundException) {
    null
} catch (_: LinkageError) {
    null
}

/** Calls the method [name] that accepts [args], wherever it is declared in the receiver's class hierarchy. */
internal fun Any.call(name: String, vararg args: Any?): Any? {
    val method = javaClass.findMethod(name, args)
        ?: throw NoSuchMethodException("${javaClass.name}.$name with ${args.size} argument(s)")
    return invokeUnwrapped(method, this, args)
}

/** Like [call], but returns null when the method is missing or throws. Meant for getters. */
internal fun Any.callOrNull(name: String, vararg args: Any?): Any? = try {
    call(name, *args)
} catch (_: Throwable) {
    null
}

internal fun Any.hasMethod(name: String, argCount: Int): Boolean =
    javaClass.allMethods().any { it.name == name && it.parameterTypes.size == argCount }

/** Calls the static method [name] declared by this class that accepts [args]. */
internal fun Class<*>.callStatic(name: String, vararg args: Any?): Any? {
    val method = declaredMethods.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == name && it.accepts(args) }
        ?: throw NoSuchMethodException("static ${this.name}.$name with ${args.size} argument(s)")
    method.isAccessible = true
    return invokeUnwrapped(method, null, args)
}

/** Like [callStatic], but returns null when the method is missing or throws. */
internal fun Class<*>.callStaticOrNull(name: String, vararg args: Any?): Any? = try {
    callStatic(name, *args)
} catch (_: Throwable) {
    null
}

/** Builds an instance of the SDK value interface [iface] whose getters return [answers], keyed by method name. */
internal fun implement(iface: Class<*>, vararg answers: Pair<String, Any?>): Any {
    val values = answers.toMap()
    return Proxy.newProxyInstance(iface.classLoader, arrayOf(iface)) { proxy, method, args ->
        when (method.name) {
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "${iface.simpleName}$values"
            else -> values[method.name]
        }
    }
}

internal fun Class<*>.enumConstant(name: String): Any? =
    enumConstants?.firstOrNull { (it as Enum<*>).name == name }

internal fun Class<*>.staticFieldOrNull(name: String): Any? = try {
    getField(name).get(null)
} catch (_: Exception) {
    null
}

/**
 * Wraps [target] in a proxy that implements all of its public interfaces, so the wrapper still passes the
 * SDK's `instanceof` checks. [onCall] sees each interface call before it is forwarded to [target]. Returns
 * [target] itself when no proxy can be built.
 */
internal fun spyOn(target: Any, onCall: (method: String, args: Array<out Any?>) -> Unit): Any {
    val interfaces = generateSequence(target.javaClass) { it.superclass }
        .flatMap { it.interfaces.asSequence() }
        .filter { Modifier.isPublic(it.modifiers) }
        .distinct()
        .toList()
    if (interfaces.isEmpty()) return target
    return try {
        Proxy.newProxyInstance(target.javaClass.classLoader, interfaces.toTypedArray()) { _, method, args ->
            val arguments = args ?: emptyArray()
            if (method.declaringClass != Any::class.java) {
                try {
                    onCall(method.name, arguments)
                } catch (_: Throwable) {
                    // Spying must never get in the way of the real call.
                }
            }
            invokeUnwrapped(method, target, arguments)
        }
    } catch (_: IllegalArgumentException) {
        target
    }
}

private fun invokeUnwrapped(method: Method, receiver: Any?, args: Array<out Any?>): Any? = try {
    method.invoke(receiver, *args)
} catch (e: InvocationTargetException) {
    throw e.targetException
}

private fun Class<*>.findMethod(name: String, args: Array<out Any?>): Method? =
    allMethods().firstOrNull { it.name == name && it.accepts(args) }?.apply { isAccessible = true }

/** Public methods first (they include interface defaults), then everything declared up the superclass chain. */
private fun Class<*>.allMethods(): Sequence<Method> = sequence {
    yieldAll(methods.asSequence())
    var cls: Class<*>? = this@allMethods
    while (cls != null) {
        yieldAll(cls.declaredMethods.asSequence())
        cls = cls.superclass
    }
}

private fun Method.accepts(args: Array<out Any?>): Boolean {
    val types = parameterTypes
    if (types.size != args.size) return false
    return types.indices.all { i ->
        val arg = args[i]
        if (arg == null) !types[i].isPrimitive else types[i].boxed().isInstance(arg)
    }
}

private val BOXED: Map<Class<*>, Class<*>> = mapOf(
    Boolean::class.javaPrimitiveType!! to Boolean::class.javaObjectType,
    Byte::class.javaPrimitiveType!! to Byte::class.javaObjectType,
    Char::class.javaPrimitiveType!! to Char::class.javaObjectType,
    Short::class.javaPrimitiveType!! to Short::class.javaObjectType,
    Int::class.javaPrimitiveType!! to Int::class.javaObjectType,
    Long::class.javaPrimitiveType!! to Long::class.javaObjectType,
    Float::class.javaPrimitiveType!! to Float::class.javaObjectType,
    Double::class.javaPrimitiveType!! to Double::class.javaObjectType,
)

private fun Class<*>.boxed(): Class<*> = BOXED[this] ?: this
