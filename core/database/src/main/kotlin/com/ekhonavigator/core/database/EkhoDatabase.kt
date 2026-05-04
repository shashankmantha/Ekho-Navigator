package com.ekhonavigator.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ekhonavigator.core.database.converter.EventCategoryConverter
import com.ekhonavigator.core.database.converter.EventSourceConverter
import com.ekhonavigator.core.database.converter.EventTypeConverter
import com.ekhonavigator.core.database.converter.InstantConverter
import com.ekhonavigator.core.database.converter.RsvpStatusConverter
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.CanvasAnnouncementDao
import com.ekhonavigator.core.database.dao.CanvasAssignmentDao
import com.ekhonavigator.core.database.dao.CanvasAssignmentGroupDao
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.dao.CanvasPlannerItemDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.database.model.CanvasAnnouncementEntity
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import com.ekhonavigator.core.database.model.CanvasAssignmentGroupEntity
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import com.ekhonavigator.core.database.model.EventAttendeeEntity

@Database(
    entities = [
        CalendarEventEntity::class,
        EventAttendeeEntity::class,
        CanvasCourseEntity::class,
        CanvasPlannerItemEntity::class,
        CanvasAssignmentEntity::class,
        CanvasAssignmentGroupEntity::class,
        CanvasAnnouncementEntity::class,
    ],
    version = 16,
    exportSchema = false,
)
@TypeConverters(
    InstantConverter::class,
    EventCategoryConverter::class,
    EventSourceConverter::class,
    EventTypeConverter::class,
    RsvpStatusConverter::class,
)
abstract class EkhoDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun eventAttendeeDao(): EventAttendeeDao
    abstract fun canvasCourseDao(): CanvasCourseDao
    abstract fun canvasPlannerItemDao(): CanvasPlannerItemDao
    abstract fun canvasAssignmentDao(): CanvasAssignmentDao
    abstract fun canvasAssignmentGroupDao(): CanvasAssignmentGroupDao
    abstract fun canvasAnnouncementDao(): CanvasAnnouncementDao
}
