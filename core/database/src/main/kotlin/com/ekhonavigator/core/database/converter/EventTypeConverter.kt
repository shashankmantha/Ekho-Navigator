package com.ekhonavigator.core.database.converter

import androidx.room.TypeConverter
import com.ekhonavigator.core.model.EventType

class EventTypeConverter {

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType =
        EventType.entries.firstOrNull { it.name == value } ?: EventType.EVENT
}
