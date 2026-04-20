package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observePlaces(): Flow<List<Place>>
    suspend fun getPlace(id: String): Place?
    suspend fun resolveFromText(raw: String): String?
}
