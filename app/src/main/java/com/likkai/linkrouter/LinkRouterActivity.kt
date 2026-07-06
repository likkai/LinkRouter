package com.likkai.linkrouter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.likkai.linkrouter.browser.BrowserLauncher
import com.likkai.linkrouter.browser.BrowserResolver
import com.likkai.linkrouter.engine.RuleEngine

class LinkRouterActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LinkRouterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
        finish()
    }

    private fun handleIntent() {
        val uri = intent?.data ?: return
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase()

        // Only handle http and https
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "Ignoring non-http(s) scheme: $scheme")
            return
        }

        val app = application as LinkRouterApp
        val debug = app.userPreferences.debugModeEnabled

        if (debug) Log.d(TAG, "Received URL: $url")

        val ruleEngine = RuleEngine(app.ruleRepository, packageName)
        val browserResolver = BrowserResolver(this)
        val launcher = BrowserLauncher(this, browserResolver)

        // Try rule engine first
        val targetPackage = ruleEngine.findTargetBrowser(url, debug)

        if (targetPackage != null) {
            if (debug) Log.d(TAG, "Rule matched: launching $targetPackage")
            if (!launcher.launch(targetPackage, url)) {
                // Target browser not installed, use fallback
                if (debug) Log.d(TAG, "Target browser not available, falling back")
                launcher.launchFallback(url)
            }
        } else {
            // No rule matched — use default browser from preferences
            val defaultPackage = app.userPreferences.defaultBrowserPackage
            if (debug) Log.d(TAG, "No rule matched. Default browser: $defaultPackage")

            if (defaultPackage != null && defaultPackage != packageName) {
                if (!launcher.launch(defaultPackage, url)) {
                    if (debug) Log.d(TAG, "Default browser not available, falling back")
                    launcher.launchFallback(url)
                }
            } else {
                // No default set, or default is self — use any available browser
                launcher.launchFallback(url)
            }
        }
    }
}
