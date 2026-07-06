package com.likkai.linkrouter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserRuleDao {

    @Query("SELECT * FROM browser_rules ORDER BY orderIndex ASC")
    fun getAllRulesOrdered(): Flow<List<BrowserRule>>

    @Query("SELECT * FROM browser_rules ORDER BY orderIndex ASC")
    suspend fun getAllRulesSync(): List<BrowserRule>

    @Query("SELECT MIN(orderIndex) FROM browser_rules")
    suspend fun getMinOrderIndex(): Int?

    @Query("SELECT MAX(orderIndex) FROM browser_rules")
    suspend fun getMaxOrderIndex(): Int?

    @Insert
    suspend fun insert(rule: BrowserRule)

    @Update
    suspend fun update(rule: BrowserRule)

    @Delete
    suspend fun delete(rule: BrowserRule)

    @Query("UPDATE browser_rules SET orderIndex = :newIndex WHERE id = :ruleId")
    suspend fun updateOrderIndex(ruleId: Int, newIndex: Int)
}
