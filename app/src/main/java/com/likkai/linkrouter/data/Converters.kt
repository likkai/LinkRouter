package com.likkai.linkrouter.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMatchType(type: MatchType): String = type.name

    @TypeConverter
    fun toMatchType(value: String): MatchType = MatchType.valueOf(value)
}
