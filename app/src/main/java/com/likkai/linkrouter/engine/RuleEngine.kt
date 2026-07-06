package com.likkai.linkrouter.engine

import android.net.Uri
import android.util.Log
import com.likkai.linkrouter.data.BrowserRule
import com.likkai.linkrouter.data.MatchType
import com.likkai.linkrouter.data.RuleRepository

class RuleEngine(
    private val repository: RuleRepository,
    private val appPackageName: String
) {

    companion object {
        private const val TAG = "RuleEngine"
    }

    /**
     * Find the target browser package for the given URL.
     * Returns the package name of the matched browser, or null if no rule matches.
     */
    fun findTargetBrowser(url: String, debug: Boolean = false): String? {
        val rules = repository.getCachedRules()

        if (debug) {
            Log.d(TAG, "Evaluating URL: $url against ${rules.size} rules")
        }

        for (rule in rules) {
            if (!rule.isEnabled) {
                if (debug) Log.d(TAG, "Rule ${rule.id} (${rule.pattern}) is disabled, skipping")
                continue
            }

            if (rule.targetBrowserPackage == appPackageName) {
                if (debug) Log.d(TAG, "Rule ${rule.id} targets self, skipping (loop prevention)")
                continue
            }

            if (matches(rule, url)) {
                if (debug) {
                    Log.d(TAG, "MATCH: Rule ${rule.id} (${rule.matchType}: ${rule.pattern}) -> ${rule.targetBrowserPackage}")
                }
                return rule.targetBrowserPackage
            } else {
                if (debug) {
                    Log.d(TAG, "No match: Rule ${rule.id} (${rule.matchType}: ${rule.pattern})")
                }
            }
        }

        if (debug) Log.d(TAG, "No rule matched for URL: $url")
        return null
    }

    private fun matches(rule: BrowserRule, url: String): Boolean {
        return when (rule.matchType) {
            MatchType.EXACT_URL -> url == rule.pattern

            MatchType.DOMAIN -> {
                val host = Uri.parse(url).host?.lowercase() ?: return false
                val domain = rule.pattern.lowercase()
                host == domain || host.endsWith(".$domain")
            }
        }
    }
}
