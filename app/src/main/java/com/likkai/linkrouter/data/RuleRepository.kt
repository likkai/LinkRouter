package com.likkai.linkrouter.data

import kotlinx.coroutines.flow.Flow

class RuleRepository(private val dao: BrowserRuleDao) {

    val allRules: Flow<List<BrowserRule>> = dao.getAllRulesOrdered()

    @Volatile
    private var cachedRules: List<BrowserRule> = emptyList()

    suspend fun loadRules() {
        cachedRules = dao.getAllRulesSync()
    }

    fun getCachedRules(): List<BrowserRule> = cachedRules

    suspend fun insertRule(rule: BrowserRule) {
        dao.insert(rule)
        loadRules()
    }

    suspend fun updateRule(rule: BrowserRule) {
        dao.update(rule)
        loadRules()
    }

    suspend fun deleteRule(rule: BrowserRule) {
        // After deleting, re-index remaining rules to keep orderIndex contiguous
        dao.delete(rule)
        val remaining = dao.getAllRulesSync()
        remaining.forEachIndexed { index, r ->
            if (r.orderIndex != index) {
                dao.updateOrderIndex(r.id, index)
            }
        }
        loadRules()
    }

    suspend fun getNextOrderIndex(): Int {
        // New rules are added at the TOP (index 0 = highest priority)
        // Shift all existing rules down by 1
        val existing = dao.getAllRulesSync()
        existing.forEach { rule ->
            dao.updateOrderIndex(rule.id, rule.orderIndex + 1)
        }
        return 0
    }

    suspend fun swapOrder(rule1: BrowserRule, rule2: BrowserRule) {
        dao.updateOrderIndex(rule1.id, rule2.orderIndex)
        dao.updateOrderIndex(rule2.id, rule1.orderIndex)
        loadRules()
    }
}
