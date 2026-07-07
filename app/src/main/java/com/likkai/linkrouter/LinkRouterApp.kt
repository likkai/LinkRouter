package com.likkai.linkrouter

import android.app.Application
import com.likkai.linkrouter.data.AppDatabase
import com.likkai.linkrouter.data.RedirectCacheDao
import com.likkai.linkrouter.data.RedirectDomainDao
import com.likkai.linkrouter.data.RuleRepository
import com.likkai.linkrouter.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LinkRouterApp : Application() {

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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        ruleRepository = RuleRepository(database.browserRuleDao())
        userPreferences = UserPreferences(this)
        redirectDomainDao = database.redirectDomainDao()
        redirectCacheDao = database.redirectCacheDao()

        // Pre-load rules and redirect domains into memory
        applicationScope.launch {
            ruleRepository.loadRules()
            loadRedirectDomains()
            // Clean expired cache entries
            val expiry = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            redirectCacheDao.clearExpired(expiry)
        }
    }

    suspend fun loadRedirectDomains() {
        cachedRedirectDomains = redirectDomainDao.getAllDomainsSync().map { it.domain.lowercase() }
    }
}
