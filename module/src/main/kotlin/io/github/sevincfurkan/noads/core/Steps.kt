package io.github.sevincfurkan.noads.core

/** One callback the real SDK would have delivered, labelled for logs and tests. */
internal class Step(val label: String, val action: () -> Unit)

internal class StepsBuilder {
    val steps = mutableListOf<Step>()

    /** Adds a call to [method] on [target]. Nothing is added when [target] is null. */
    fun then(target: Any?, method: String, vararg args: Any?) {
        if (target != null) steps += Step(method) { target.call(method, *args) }
    }

    /** Like [then], but only when [target] still has [method] (callbacks that newer SDK versions removed). */
    fun thenIfPresent(target: Any?, method: String, vararg args: Any?) {
        if (target != null && target.hasMethod(method, args.size)) then(target, method, *args)
    }
}

internal fun steps(block: StepsBuilder.() -> Unit): List<Step> = StepsBuilder().apply(block).steps

/** Runs every step even when the app's own callback code throws, reporting each failure to [onError]. */
internal fun List<Step>.runAll(onError: (Step, Throwable) -> Unit) {
    for (step in this) {
        try {
            step.action()
        } catch (t: Throwable) {
            onError(step, t)
        }
    }
}
