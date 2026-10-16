package com.personaltracker.data.local.db.converters

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Room TypeConverter for [LocalDate] (ISO-8601 String) and [Instant] (UTC Epoch Millis Long).
 */
class DateConverters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.let {
        try {
            it.toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun toInstant(millis: Long?): Instant? = millis?.let {
        try {
            Instant.ofEpochMilli(it)
        } catch (_: Exception) {
            null
        }
    }
}
