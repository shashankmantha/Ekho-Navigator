package com.ekhonavigator.core.database.di

import android.content.Context
import androidx.room.Room
import com.ekhonavigator.core.database.EkhoDatabase
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EkhoDatabase =
        Room.databaseBuilder(
            context,
            EkhoDatabase::class.java,
            "ekho-database",
        ).fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    fun provideCalendarEventDao(db: EkhoDatabase): CalendarEventDao =
        db.calendarEventDao()

    @Provides
    fun provideEventAttendeeDao(db: EkhoDatabase): EventAttendeeDao =
        db.eventAttendeeDao()
}
