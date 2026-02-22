package com.ekhonavigator.core.database.converter

import androidx.room.TypeConverter
import com.ekhonavigator.core.model.EventCategory

/**
 * Converts List<[EventCategory]> to/from a comma-separated string for Room storage.
 */
internal class EventCategoryConverter {

    @TypeConverter
    fun fromCategories(value: List<EventCategory>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toCategories(value: String): List<EventCategory> =
        if (value.isBlank()) emptyList()
        else value.split(",").map { EventCategory.valueOf(it) }
}
