package com.likkai.linkrouter

import android.app.Application
import com.likkai.linkrouter.data.AppDatabase
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

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        ruleRepository = RuleRepository(database.browserRuleDao())
        userPreferences = UserPreferences(this)

        // Pre-load rules into memory for fast access from LinkRouterActivity
        applicationScope.launch {
            ruleRepository.loadRules()
        }
    }
}
