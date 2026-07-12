package com.likkai.linkrouter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.likkai.linkrouter.LinkRouterApp
import com.likkai.linkrouter.data.RedirectCache
import com.likkai.linkrouter.data.RedirectDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LinkRouterApp
    private val preferences = app.userPreferences
    private val redirectDomainDao = app.redirectDomainDao
    private val redirectCacheDao = app.redirectCacheDao

    val redirectDomains: StateFlow<List<RedirectDomain>> = redirectDomainDao.getAllDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val redirectCache: StateFlow<List<RedirectCache>> = redirectCacheDao.getAllCache()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _followRedirects = MutableStateFlow(preferences.followRedirectsEnabled)
    val followRedirects: StateFlow<Boolean> = _followRedirects.asStateFlow()

    fun toggleFollowRedirects() {
        val newValue = !_followRedirects.value
        preferences.followRedirectsEnabled = newValue
        _followRedirects.value = newValue
    }

    fun addDomain(domain: String) {
        val trimmed = domain.trim().lowercase()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            // Check for duplicates
            if (redirectDomainDao.countByDomain(trimmed) == 0) {
                redirectDomainDao.insert(RedirectDomain(domain = trimmed))
                app.loadRedirectDomains()
            }
        }
    }

    fun removeDomain(domain: RedirectDomain) {
        viewModelScope.launch {
            redirectDomainDao.delete(domain)
            app.loadRedirectDomains()
        }
    }

    fun deleteCacheEntry(cache: RedirectCache) {
        viewModelScope.launch {
            redirectCacheDao.delete(cache)
        }
    }
}
