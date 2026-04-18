package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPlaceRepository @Inject constructor(
    seed: PlacesSeed,
    private val matcher: PlaceMatcher,
) : PlaceRepository {

    // Seed lists a place once per category for map filters; collapse to one per id.
    private val state: MutableStateFlow<List<Place>> =
        MutableStateFlow(seed.places().distinctBy { it.id })

    override fun observePlaces(): Flow<List<Place>> = state.asStateFlow()

    override suspend fun getPlace(id: String): Place? =
        state.value.firstOrNull { it.id == id }

    override suspend fun resolveFromText(raw: String): String? =
        matcher.resolve(raw, state.value)
}
