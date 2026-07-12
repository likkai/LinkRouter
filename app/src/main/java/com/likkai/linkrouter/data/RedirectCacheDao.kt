package com.likkai.linkrouter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RedirectCacheDao {

    @Query("SELECT * FROM redirect_cache WHERE shortUrl = :url AND timestamp > :minTimestamp LIMIT 1")
    suspend fun getCached(url: String, minTimestamp: Long): RedirectCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: RedirectCache)

    @Query("DELETE FROM redirect_cache WHERE timestamp < :minTimestamp")
    suspend fun clearExpired(minTimestamp: Long)

    @Query("SELECT * FROM redirect_cache ORDER BY timestamp DESC")
    fun getAllCache(): Flow<List<RedirectCache>>

    @Delete
    suspend fun delete(cache: RedirectCache)
}
