package io.github.sevincfurkan.noads.core

/** Ad formats the user can switch on or off. [key] is the preference key shared with the settings screen. */
enum class AdType(val key: String) {
    REWARDED("rewarded"),
    INTERSTITIAL("interstitial"),
    APP_OPEN("app_open"),
    BANNER("banner"),
}

/**
 * What the hooks ask before acting. Implementations read the values at call time, so a change in the
 * settings screen applies to a running game without restarting it.
 */
interface Settings {
    fun isEnabled(type: AdType): Boolean

    val verboseLog: Boolean

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_VERBOSE_LOG = "verbose_log"
    }
}
