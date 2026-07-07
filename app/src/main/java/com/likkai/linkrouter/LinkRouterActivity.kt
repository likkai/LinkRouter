package com.likkai.linkrouter

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.likkai.linkrouter.browser.BrowserLauncher
import com.likkai.linkrouter.browser.BrowserResolver
import com.likkai.linkrouter.data.RedirectCache
import com.likkai.linkrouter.engine.RuleEngine
import com.likkai.linkrouter.redirect.RedirectResolver
import com.likkai.linkrouter.ui.theme.LinkRouterTheme
import kotlinx.coroutines.launch

class LinkRouterActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LinkRouterActivity"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data ?: run { finish(); return }
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase()

        // Only handle http and https
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "Ignoring non-http(s) scheme: $scheme")
            finish()
            return
        }

        val app = application as LinkRouterApp
        val debug = app.userPreferences.debugModeEnabled

        if (debug) Log.d(TAG, "Received URL: $url")

        // Check if this URL needs redirect resolution
        if (app.userPreferences.followRedirectsEnabled && isRedirectDomain(url, app)) {
            if (debug) Log.d(TAG, "URL matches redirect domain, resolving...")

            // Show resolving popup
            setContent {
                LinkRouterTheme {
                    ResolvingPopup()
                }
            }

            // Resolve async, then route
            lifecycleScope.launch {
                val resolvedUrl = resolveRedirect(url, app, debug)
                if (debug) Log.d(TAG, "Resolved URL: $resolvedUrl")
                routeUrl(resolvedUrl, app, debug)
                finish()
            }
        } else {
            // No redirect needed, route immediately
            routeUrl(url, app, debug)
            finish()
        }
    }

    private fun isRedirectDomain(url: String, app: LinkRouterApp): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: return false
        return app.cachedRedirectDomains.any { domain ->
            host == domain || host.endsWith(".$domain")
        }
    }

    private suspend fun resolveRedirect(
        url: String,
        app: LinkRouterApp,
        debug: Boolean
    ): String {
        // Check cache first
        val minTimestamp = System.currentTimeMillis() - CACHE_DURATION_MS
        val cached = app.redirectCacheDao.getCached(url, minTimestamp)
        if (cached != null) {
            if (debug) Log.d(TAG, "Cache hit: ${cached.shortUrl} -> ${cached.resolvedUrl}")
            return cached.resolvedUrl
        }

        // Resolve via HTTP
        val resolver = RedirectResolver()
        val resolvedUrl = resolver.resolve(url, debug)

        // Cache the result if it actually resolved to something different
        if (resolvedUrl != url) {
            app.redirectCacheDao.insert(
                RedirectCache(
                    shortUrl = url,
                    resolvedUrl = resolvedUrl,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        return resolvedUrl
    }

    private fun routeUrl(url: String, app: LinkRouterApp, debug: Boolean) {
        val ruleEngine = RuleEngine(app.ruleRepository, packageName)
        val browserResolver = BrowserResolver(this)
        val launcher = BrowserLauncher(this, browserResolver)

        // Try rule engine first
        val targetPackage = ruleEngine.findTargetBrowser(url, debug)

        if (targetPackage != null) {
            if (debug) Log.d(TAG, "Rule matched: launching $targetPackage")
            if (!launcher.launch(targetPackage, url)) {
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
                launcher.launchFallback(url)
            }
        }
    }
}

@Composable
private fun ResolvingPopup() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Resolving short link...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
