package com.likkai.linkrouter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RedirectDomainDao {

    @Query("SELECT * FROM redirect_domains ORDER BY domain ASC")
    fun getAllDomains(): Flow<List<RedirectDomain>>

    @Query("SELECT * FROM redirect_domains ORDER BY domain ASC")
    suspend fun getAllDomainsSync(): List<RedirectDomain>

    @Insert
    suspend fun insert(domain: RedirectDomain)

    @Delete
    suspend fun delete(domain: RedirectDomain)

    @Query("SELECT COUNT(*) FROM redirect_domains WHERE domain = :domain")
    suspend fun countByDomain(domain: String): Int
}
