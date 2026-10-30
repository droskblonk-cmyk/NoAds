package io.github.sevincfurkan.noads.core

import de.robv.android.xposed.XSharedPreferences
import io.github.sevincfurkan.noads.BuildConfig

/**
 * Reads the module's settings from inside a hooked app. LSPosed makes the file the settings screen writes
 * with MODE_WORLD_READABLE readable here. Until the user opens that screen there is no file and every
 * format defaults to on.
 */
internal class XposedSettings : Settings {
    private val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, Settings.PREFS_NAME)

    private fun current(): XSharedPreferences {
        if (prefs.hasFileChanged()) prefs.reload()
        return prefs
    }

    override fun isEnabled(type: AdType) = current().getBoolean(type.key, true)

    override val verboseLog get() = current().getBoolean(Settings.KEY_VERBOSE_LOG, false)
}
