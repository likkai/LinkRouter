package com.likkai.linkrouter

import android.app.ActivityManager
import android.app.ApplicationExitInfo
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

        val uri = intent?.data ?: run { finishAndRemoveTask(); return }
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase()

        // Only handle http and https
        if (scheme != "http" && scheme != "https") {
            Log.w(TAG, "Ignoring non-http(s) scheme: $scheme")
            finishAndRemoveTask()
            return
        }

        val app = application as LinkRouterApp
        val debug = app.userPreferences.debugModeEnabled

        if (debug) {
            Log.d(TAG, "────────────────────────────────────")
            Log.d(TAG, "Step 1: Received URL: $url")
            logPreviousExitReasons()
        }

        // Show a resolving popup preemptively (will be removed on finishAndRemoveTask)
        setContent {
            LinkRouterTheme {
                ResolvingPopup()
            }
        }

        // Run everything in a coroutine so we can await initialization
        lifecycleScope.launch {
            if (debug) Log.d(TAG, "Step 2: Awaiting app initialization...")
            val initStart = System.currentTimeMillis()
            app.awaitInitialization()
            if (debug) Log.d(TAG, "Step 2: Init ready (waited ${System.currentTimeMillis() - initStart}ms)")

            if (debug) {
                Log.d(TAG, "Step 3: State check — followRedirects=${app.userPreferences.followRedirectsEnabled}")
                Log.d(TAG, "  cachedRedirectDomains=${app.cachedRedirectDomains}")
                Log.d(TAG, "  cachedRules=${app.ruleRepository.getCachedRules().size} rules")
            }

            // Check if this URL needs redirect resolution
            if (app.userPreferences.followRedirectsEnabled && isRedirectDomain(url, app, debug)) {
                if (debug) Log.d(TAG, "Step 4: URL matches redirect domain → resolving")

                val resolvedUrl = resolveRedirect(url, app, debug)
                if (debug) Log.d(TAG, "Step 5: Resolved → $resolvedUrl")
                routeUrl(resolvedUrl, app, debug)
            } else {
                if (debug) Log.d(TAG, "Step 4: No redirect needed → routing directly")
                routeUrl(url, app, debug)
            }

            if (debug) Log.d(TAG, "Step 6: Done, finishing activity")
            finishAndRemoveTask()
        }
    }

    private fun isRedirectDomain(url: String, app: LinkRouterApp, debug: Boolean = false): Boolean {
        val host = Uri.parse(url).host?.lowercase() ?: run {
            if (debug) Log.d(TAG, "  isRedirectDomain: could not parse host from URL")
            return false
        }
        val match = app.cachedRedirectDomains.any { domain ->
            host == domain || host.endsWith(".$domain")
        }
        if (debug) Log.d(TAG, "  isRedirectDomain: host=$host, match=$match")
        return match
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
            if (debug) Log.d(TAG, "  Redirect cache HIT: ${cached.shortUrl} → ${cached.resolvedUrl}")
            return cached.resolvedUrl
        }
        if (debug) Log.d(TAG, "  Redirect cache MISS, resolving via HTTP...")

        // Resolve via HTTP
        val resolver = RedirectResolver()
        val resolvedUrl = resolver.resolve(url, debug)

        // Cache the result if it actually resolved to something different
        if (resolvedUrl != url) {
            if (debug) Log.d(TAG, "  Caching: $url → $resolvedUrl")
            app.redirectCacheDao.insert(
                RedirectCache(
                    shortUrl = url,
                    resolvedUrl = resolvedUrl,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            if (debug) Log.d(TAG, "  URL did not redirect (stayed the same)")
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
            if (debug) Log.d(TAG, "  Route: rule matched → $targetPackage")
            if (!launcher.launch(targetPackage, url)) {
                if (debug) Log.d(TAG, "  Route: target not available, falling back")
                launcher.launchFallback(url)
            }
        } else {
            // No rule matched — use default browser from preferences
            val defaultPackage = app.userPreferences.defaultBrowserPackage
            if (debug) Log.d(TAG, "  Route: no rule matched, default=$defaultPackage")

            if (defaultPackage != null && defaultPackage != packageName) {
                if (!launcher.launch(defaultPackage, url)) {
                    if (debug) Log.d(TAG, "  Route: default not available, falling back")
                    launcher.launchFallback(url)
                }
            } else {
                if (debug) Log.d(TAG, "  Route: no default set, using fallback")
                launcher.launchFallback(url)
            }
        }
    }

    /**
     * Logs why the process was last killed (API 30+).
     * Useful for diagnosing why redirect caches were empty.
     */
    private fun logPreviousExitReasons() {
        try {
            val am = getSystemService(ActivityManager::class.java)
            val reasons = am.getHistoricalProcessExitReasons(packageName, 0, 3)
            if (reasons.isEmpty()) {
                Log.d(TAG, "  No previous exit reasons recorded")
            } else {
                reasons.forEachIndexed { i, info ->
                    Log.d(TAG, "  Exit #$i: reason=${exitReasonToString(info.reason)}" +
                            ", importance=${info.importance}" +
                            ", description=${info.description}" +
                            ", timestamp=${info.timestamp}" +
                            ", ago=${(System.currentTimeMillis() - info.timestamp) / 1000}s")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "  Could not read exit reasons", e)
        }
    }

    private fun exitReasonToString(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        ApplicationExitInfo.REASON_FREEZER -> "FREEZER"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "PACKAGE_STATE_CHANGE"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "PACKAGE_UPDATED"
        else -> "UNKNOWN($reason)"
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
