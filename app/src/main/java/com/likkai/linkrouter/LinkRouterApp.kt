package com.likkai.linkrouter

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

        if (debug) Log.d(TAG, "═══ Process created ═══")

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
}
