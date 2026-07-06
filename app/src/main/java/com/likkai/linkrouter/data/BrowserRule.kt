package com.likkai.linkrouter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browser_rules")
data class BrowserRule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val matchType: MatchType,
    val pattern: String,
    val targetBrowserPackage: String,
    val targetBrowserLabel: String,
    val orderIndex: Int,
    val isEnabled: Boolean = true
)
