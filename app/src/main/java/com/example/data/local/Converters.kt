package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMovementType(type: MovementType): String = type.name

    @TypeConverter
    fun toMovementType(value: String): MovementType =
        runCatching { MovementType.valueOf(value) }.getOrDefault(MovementType.ENTRADA)
}
