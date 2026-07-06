package com.likkai.linkrouter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.likkai.linkrouter.LinkRouterApp
import com.likkai.linkrouter.browser.BrowserApp
import com.likkai.linkrouter.browser.BrowserResolver
import com.likkai.linkrouter.data.BrowserRule
import com.likkai.linkrouter.data.MatchType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LinkRouterApp
    private val repository = app.ruleRepository
    private val preferences = app.userPreferences
    private val browserResolver = BrowserResolver(application)

    // Rules from DB, observed as Flow
    val rules: StateFlow<List<BrowserRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Installed browsers
    private val _installedBrowsers = MutableStateFlow<List<BrowserApp>>(emptyList())
    val installedBrowsers: StateFlow<List<BrowserApp>> = _installedBrowsers.asStateFlow()

    // Default browser
    private val _defaultBrowserPackage = MutableStateFlow(preferences.defaultBrowserPackage)
    val defaultBrowserPackage: StateFlow<String?> = _defaultBrowserPackage.asStateFlow()

    private val _defaultBrowserLabel = MutableStateFlow(preferences.defaultBrowserLabel)
    val defaultBrowserLabel: StateFlow<String?> = _defaultBrowserLabel.asStateFlow()

    // Debug mode
    private val _debugMode = MutableStateFlow(preferences.debugModeEnabled)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()

    init {
        loadBrowsers()
    }

    fun loadBrowsers() {
        _installedBrowsers.value = browserResolver.getInstalledBrowsers()
        // If no default browser is set yet, auto-select the first available
        if (_defaultBrowserPackage.value == null && _installedBrowsers.value.isNotEmpty()) {
            setDefaultBrowser(_installedBrowsers.value.first())
        }
    }

    fun addRule(matchType: MatchType, pattern: String, browser: BrowserApp) {
        viewModelScope.launch {
            val orderIndex = repository.getNextOrderIndex()
            val rule = BrowserRule(
                matchType = matchType,
                pattern = pattern.trim(),
                targetBrowserPackage = browser.packageName,
                targetBrowserLabel = browser.label,
                orderIndex = orderIndex
            )
            repository.insertRule(rule)
        }
    }

    fun updateRule(rule: BrowserRule) {
        viewModelScope.launch {
            repository.updateRule(rule)
        }
    }

    fun deleteRule(rule: BrowserRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    fun toggleRule(rule: BrowserRule) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun moveRuleUp(rule: BrowserRule, allRules: List<BrowserRule>) {
        val index = allRules.indexOf(rule)
        if (index > 0) {
            viewModelScope.launch {
                repository.swapOrder(rule, allRules[index - 1])
            }
        }
    }

    fun moveRuleDown(rule: BrowserRule, allRules: List<BrowserRule>) {
        val index = allRules.indexOf(rule)
        if (index < allRules.size - 1) {
            viewModelScope.launch {
                repository.swapOrder(rule, allRules[index + 1])
            }
        }
    }

    fun setDefaultBrowser(browser: BrowserApp) {
        preferences.defaultBrowserPackage = browser.packageName
        preferences.defaultBrowserLabel = browser.label
        _defaultBrowserPackage.value = browser.packageName
        _defaultBrowserLabel.value = browser.label
    }

    fun toggleDebugMode() {
        val newValue = !_debugMode.value
        preferences.debugModeEnabled = newValue
        _debugMode.value = newValue
    }
}
