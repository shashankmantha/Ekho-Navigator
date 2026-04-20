package com.ekhonavigator.feature.map.di

import com.ekhonavigator.core.data.place.PlacesSeed
import com.ekhonavigator.feature.map.CampusPlacesData
import com.ekhonavigator.feature.map.toPlace
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MapDataModule {

    @Provides
    @Singleton
    fun providePlacesSeed(): PlacesSeed = PlacesSeed {
        CampusPlacesData.places.map { it.toPlace() }
    }
}
