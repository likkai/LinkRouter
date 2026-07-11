package com.likkai.linkrouter

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.Application
import android.util.Log
import com.likkai.linkrouter.data.AppDatabase
import com.likkai.linkrouter.data.RedirectCacheDao
import com.likkai.linkrouter.data.RedirectDomainDao
import com.likkai.linkrouter.data.RuleRepository
import com.likkai.linkrouter.data.UserPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LinkRouterApp : Application() {

    companion object {
        private const val TAG = "LinkRouterApp"
    }

    lateinit var database: AppDatabase
        private set

    lateinit var ruleRepository: RuleRepository
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var redirectDomainDao: RedirectDomainDao
        private set

    lateinit var redirectCacheDao: RedirectCacheDao
        private set

    // Cached redirect domains for fast lookup in LinkRouterActivity
    @Volatile
    var cachedRedirectDomains: List<String> = emptyList()
        private set

    // Signals when all caches are populated and ready
    private val _initComplete = CompletableDeferred<Unit>()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val debug = try {
            // UserPreferences might not be initialized yet at this line,
            // so read directly from SharedPreferences
            getSharedPreferences("linkrouter_prefs", MODE_PRIVATE)
                .getBoolean("debug_mode", false)
        } catch (_: Exception) { false }

        if (debug) {
            Log.d(TAG, "═══ Process created ═══")
            logPreviousExitReasons(TAG)
        }

        database = AppDatabase.getInstance(this)
        ruleRepository = RuleRepository(database.browserRuleDao())
        userPreferences = UserPreferences(this)
        redirectDomainDao = database.redirectDomainDao()
        redirectCacheDao = database.redirectCacheDao()

        // Pre-load rules and redirect domains into memory
        applicationScope.launch {
            try {
                val startMs = System.currentTimeMillis()

                ruleRepository.loadRules()
                if (debug) Log.d(TAG, "  Rules loaded: ${ruleRepository.getCachedRules().size} rules (${System.currentTimeMillis() - startMs}ms)")

                loadRedirectDomains()
                if (debug) Log.d(TAG, "  Redirect domains loaded: $cachedRedirectDomains (${System.currentTimeMillis() - startMs}ms)")

                // Clean expired cache entries
                val expiry = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                redirectCacheDao.clearExpired(expiry)

                if (debug) Log.d(TAG, "  Init complete (${System.currentTimeMillis() - startMs}ms total)")
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
            } finally {
                _initComplete.complete(Unit)
            }
        }
    }

    /**
     * Suspends until all caches (rules, redirect domains) are populated.
     * Must be called from LinkRouterActivity before reading cached data.
     */
    suspend fun awaitInitialization() {
        _initComplete.await()
    }

    suspend fun loadRedirectDomains() {
        cachedRedirectDomains = redirectDomainDao.getAllDomainsSync().map { it.domain.lowercase() }
    }

    fun logPreviousExitReasons(tag: String) {
        try {
            val am = getSystemService(ActivityManager::class.java)
            val reasons = am.getHistoricalProcessExitReasons(packageName, 0, 3)
            if (reasons.isEmpty()) {
                Log.d(tag, "  No previous exit reasons recorded")
            } else {
                reasons.forEachIndexed { i, info ->
                    Log.d(tag, "  Exit #$i: reason=${exitReasonToString(info.reason)}" +
                            ", importance=${info.importance}" +
                            ", description=${info.description}" +
                            ", timestamp=${info.timestamp}" +
                            ", ago=${(System.currentTimeMillis() - info.timestamp) / 1000}s")
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "  Could not read exit reasons", e)
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
