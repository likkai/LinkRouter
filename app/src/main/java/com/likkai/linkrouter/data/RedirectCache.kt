package com.likkai.linkrouter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "redirect_cache")
data class RedirectCache(
    @PrimaryKey
    val shortUrl: String,
    val resolvedUrl: String,
    val timestamp: Long
)
