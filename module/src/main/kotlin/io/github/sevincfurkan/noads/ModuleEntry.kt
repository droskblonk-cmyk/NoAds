package io.github.sevincfurkan.noads

import android.app.Application
import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.sevincfurkan.noads.core.Log
import io.github.sevincfurkan.noads.core.XposedSettings
import io.github.sevincfurkan.noads.core.after

/** LSPosed entry point (see assets/xposed_init). Runs in every app the user enabled the module for. */
class ModuleEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) return
        // Called again for other packages loading into the same process; hooking twice would deliver
        // every callback twice, so only the process's own app sets things up.
        if (!lpparam.isFirstApplication) return

        val settings = XposedSettings()
        Log.verbose = { settings.verboseLog }
        val installer = Installer(lpparam.packageName, settings)
        installer.install(lpparam.classLoader)

        // Packed apps unpack their real code, and the ad SDKs with it, while their Application attaches.
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, after { param ->
            installer.install((param.thisObject as Context).classLoader)
        })
    }
}
