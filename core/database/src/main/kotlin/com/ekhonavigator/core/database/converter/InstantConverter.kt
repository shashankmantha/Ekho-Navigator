package com.ekhonavigator.core.database.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Converts [Instant] to/from epoch milliseconds for Room storage.
 */
internal class InstantConverter {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
