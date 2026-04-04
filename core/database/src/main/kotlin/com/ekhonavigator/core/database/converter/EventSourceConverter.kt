package com.ekhonavigator.core.database.converter

import androidx.room.TypeConverter
import com.ekhonavigator.core.model.EventSource

internal class EventSourceConverter {

    @TypeConverter
    fun fromEventSource(value: EventSource): String = value.name

    @TypeConverter
    fun toEventSource(value: String): EventSource =
        try {
            EventSource.valueOf(value)
        } catch (_: IllegalArgumentException) {
            EventSource.ICAL_FEED
        }
}
