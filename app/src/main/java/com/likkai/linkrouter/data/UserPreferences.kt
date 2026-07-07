package com.likkai.linkrouter.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var defaultBrowserPackage: String?
        get() = prefs.getString(KEY_DEFAULT_BROWSER_PACKAGE, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_BROWSER_PACKAGE, value).apply()

    var defaultBrowserLabel: String?
        get() = prefs.getString(KEY_DEFAULT_BROWSER_LABEL, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_BROWSER_LABEL, value).apply()

    var debugModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    var followRedirectsEnabled: Boolean
        get() = prefs.getBoolean(KEY_FOLLOW_REDIRECTS, true)
        set(value) = prefs.edit().putBoolean(KEY_FOLLOW_REDIRECTS, value).apply()

    companion object {
        private const val PREFS_NAME = "linkrouter_prefs"
        private const val KEY_DEFAULT_BROWSER_PACKAGE = "default_browser_package"
        private const val KEY_DEFAULT_BROWSER_LABEL = "default_browser_label"
        private const val KEY_DEBUG_MODE = "debug_mode"
        private const val KEY_FOLLOW_REDIRECTS = "follow_redirects"
    }
}
