package com.ekhonavigator.core.database.di

import android.content.Context
import androidx.room.Room
import com.ekhonavigator.core.database.EkhoDatabase
import com.ekhonavigator.core.database.dao.CalendarEventDao
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
        ).build()

    @Provides
    fun provideCalendarEventDao(db: EkhoDatabase): CalendarEventDao =
        db.calendarEventDao()
}
