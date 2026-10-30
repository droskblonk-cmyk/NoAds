package io.github.sevincfurkan.noads.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.sevincfurkan.noads.R
import io.github.sevincfurkan.noads.core.Settings

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val active = isModuleActive(requireContext())
            preferenceManager.sharedPreferencesName = Settings.PREFS_NAME
            // Only world-readable preferences reach the hooks inside games, and only LSPosed allows them.
            @Suppress("DEPRECATION")
            preferenceManager.sharedPreferencesMode = if (active) Context.MODE_WORLD_READABLE else Context.MODE_PRIVATE
            setPreferencesFromResource(R.xml.preferences, rootKey)

            findPreference<Preference>("status")?.apply {
                setTitle(if (active) R.string.status_active else R.string.status_inactive)
                setSummary(if (active) R.string.status_active_summary else R.string.status_inactive_summary)
            }
            findPreference<Preference>("source")?.setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.source_url))))
                true
            }
        }
    }

    private companion object {
        /**
         * LSPosed lets an enabled module with xposedsharedprefs open world-readable preferences, while Android
         * refuses everywhere else. So whether that works tells whether the module is active.
         */
        fun isModuleActive(context: Context): Boolean = try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_WORLD_READABLE)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
