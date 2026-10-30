package io.github.sevincfurkan.noads.core

import android.os.Handler
import android.os.Looper
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.lang.reflect.Modifier

internal typealias HookParam = XC_MethodHook.MethodHookParam

/** Returned from a [replace] body to let the original method run, e.g. when the format is switched off. */
internal object CallOriginal

/** Runs [body] before the original method. Failures are logged and never reach the app. */
internal fun before(body: (HookParam) -> Unit): XC_MethodHook = object : XC_MethodHook() {
    override fun beforeHookedMethod(param: HookParam) = guard(param) { body(param) }
}

/** Runs [body] after the original method. Failures are logged and never reach the app. */
internal fun after(body: (HookParam) -> Unit): XC_MethodHook = object : XC_MethodHook() {
    override fun afterHookedMethod(param: HookParam) = guard(param) { body(param) }
}

/**
 * Runs [body] instead of the original method and returns its result. When [body] returns [CallOriginal]
 * or throws, the original method runs as if nothing happened: a broken hook shows the real ad instead of
 * breaking the game.
 */
internal fun replace(body: (HookParam) -> Any?): XC_MethodHook = object : XC_MethodHook() {
    override fun beforeHookedMethod(param: HookParam) = guard(param) {
        val result = body(param)
        if (result !== CallOriginal) param.result = result
    }
}

private inline fun guard(param: HookParam, block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        Log.e("Hook on ${param.method} failed, the original method runs instead", t)
    }
}

/**
 * Hooks every non-abstract method called [name] declared directly in this class (compiler-generated
 * bridges excluded, they just forward to the real method). Returns how many were hooked.
 */
internal fun Class<*>.hookDeclared(name: String, hook: XC_MethodHook): Int {
    var count = 0
    for (method in declaredMethods) {
        if (method.name == name && !method.isBridge && !Modifier.isAbstract(method.modifiers)) {
            XposedBridge.hookMethod(method, hook)
            count++
        }
    }
    return count
}

/**
 * Hooks the closest declaration of [name], walking up from this class and stopping before [stopAt]
 * (typically the SDK's abstract base, whose method is abstract or a no-op the app overrides).
 */
internal fun Class<*>.hookNearest(name: String, stopAt: Class<*>?, hook: XC_MethodHook): Int {
    var cls: Class<*>? = this
    while (cls != null && cls != stopAt && cls != Any::class.java) {
        val count = cls.hookDeclared(name, hook)
        if (count > 0) return count
        cls = cls.superclass
    }
    return 0
}

/**
 * Turns the banner's load calls into no-ops and hides it, so no ad request is ever made. [viewOf] finds the
 * banner view for static load methods.
 */
internal fun blockBanner(
    sdk: String,
    bannerClass: Class<*>,
    settings: Settings,
    vararg loadMethods: String,
    viewOf: (HookParam) -> Any? = { it.thisObject },
) {
    for (name in loadMethods) {
        bannerClass.hookDeclared(name, replace { param ->
            if (!settings.isEnabled(AdType.BANNER)) return@replace CallOriginal
            (viewOf(param) as? View)?.visibility = View.GONE
            Log.d("$sdk banner: blocked ${param.method.name}()")
            null
        })
    }
}

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/**
 * Delivers [steps] on the main thread right after the hooked show call returns, which is where and when
 * the SDK would deliver them. Posting matters: games often update their own state after calling show.
 */
internal fun deliver(what: String, steps: List<Step>) {
    Log.i("$what: ad skipped, ${steps.joinToString(" → ") { it.label }}")
    mainHandler.post {
        steps.runAll { step, error -> Log.e("$what: the app's ${step.label} threw", error) }
    }
}
