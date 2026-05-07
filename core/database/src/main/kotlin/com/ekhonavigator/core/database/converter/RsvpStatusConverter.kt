package com.ekhonavigator.core.database.converter

import androidx.room.TypeConverter
import com.ekhonavigator.core.model.RsvpStatus

class RsvpStatusConverter {

    @TypeConverter
    fun fromRsvpStatus(value: RsvpStatus): String = value.name

    @TypeConverter
    fun toRsvpStatus(value: String): RsvpStatus =
        try {
            RsvpStatus.valueOf(value)
        } catch (_: IllegalArgumentException) {
            RsvpStatus.PENDING
        }
}
