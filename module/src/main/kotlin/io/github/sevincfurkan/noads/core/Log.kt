package io.github.sevincfurkan.noads.core

import de.robv.android.xposed.XposedBridge

/** Logs to logcat under [TAG] and to the LSPosed manager's log. */
internal object Log {
    const val TAG = "NoAds"

    var verbose: () -> Boolean = { false }

    fun i(message: String) {
        android.util.Log.i(TAG, message)
        XposedBridge.log("$TAG: $message")
    }

    fun d(message: String) {
        if (verbose()) i(message)
    }

    fun e(message: String, error: Throwable? = null) {
        android.util.Log.e(TAG, message, error)
        XposedBridge.log("$TAG: $message")
        if (error != null) XposedBridge.log(error)
    }
}
