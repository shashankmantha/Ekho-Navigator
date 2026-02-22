package com.ekhonavigator.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ekhonavigator.core.database.converter.EventCategoryConverter
import com.ekhonavigator.core.database.converter.InstantConverter
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.model.CalendarEventEntity

@Database(
    entities = [CalendarEventEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(
    InstantConverter::class,
    EventCategoryConverter::class,
)
abstract class EkhoDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
}
