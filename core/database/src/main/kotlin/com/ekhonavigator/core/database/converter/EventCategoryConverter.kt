package com.ekhonavigator.core.database.converter

import androidx.room.TypeConverter
import com.ekhonavigator.core.model.EventCategory

/**
 * Converts List<[EventCategory]> to/from a comma-separated string for Room storage.
 * Uses a safe lookup so stale enum names (from a previous app version) don't crash —
 * they fall back to GENERAL instead.
 */
internal class EventCategoryConverter {

    @TypeConverter
    fun fromCategories(value: List<EventCategory>): String =
        value.joinToString(",") { it.name }

    @TypeConverter
    fun toCategories(value: String): List<EventCategory> =
        if (value.isBlank()) emptyList()
        else value.split(",").map { name ->
            try {
                EventCategory.valueOf(name)
            } catch (_: IllegalArgumentException) {
                EventCategory.GENERAL
            }
        }
}
