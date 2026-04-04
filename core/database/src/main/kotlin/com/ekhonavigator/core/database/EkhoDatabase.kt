package com.ekhonavigator.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ekhonavigator.core.database.converter.EventCategoryConverter
import com.ekhonavigator.core.database.converter.EventSourceConverter
import com.ekhonavigator.core.database.converter.InstantConverter
import com.ekhonavigator.core.database.converter.RsvpStatusConverter
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.database.model.EventAttendeeEntity

@Database(
    entities = [
        CalendarEventEntity::class,
        EventAttendeeEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(
    InstantConverter::class,
    EventCategoryConverter::class,
    EventSourceConverter::class,
    RsvpStatusConverter::class,
)
abstract class EkhoDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun eventAttendeeDao(): EventAttendeeDao
}
