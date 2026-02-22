package com.ekhonavigator.core.data.di

import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.DefaultCalendarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindCalendarRepository(
        impl: DefaultCalendarRepository,
    ): CalendarRepository
}
