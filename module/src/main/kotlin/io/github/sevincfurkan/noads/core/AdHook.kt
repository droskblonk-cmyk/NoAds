package io.github.sevincfurkan.noads.core

/** Support for one ad SDK. */
internal interface AdHook {
    val name: String

    /**
     * Installs the hooks if the SDK's classes are visible through [classLoader]. Returns false, having
     * done nothing, when the app doesn't contain the SDK.
     */
    fun install(classLoader: ClassLoader, settings: Settings): Boolean
}
