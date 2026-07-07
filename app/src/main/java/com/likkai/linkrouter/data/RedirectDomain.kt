package com.likkai.linkrouter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "redirect_domains")
data class RedirectDomain(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val domain: String
)
