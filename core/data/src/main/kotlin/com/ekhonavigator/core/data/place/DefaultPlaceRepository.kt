package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.data.markers.UserDroppedMarker
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.PlaceCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPlaceRepository @Inject constructor(
    seed: PlacesSeed,
    private val matcher: PlaceMatcher,
    private val markerRepository: MarkerRepository,
    private val authRepository: AuthRepository,
) : PlaceRepository {

    // Seed lists a place once per category for map filters; collapse to one per id.
    private val campusPlaces: MutableStateFlow<List<Place>> =
        MutableStateFlow(seed.places().distinctBy { it.id })

    /** Re-evaluates the signed-in uid at subscription so the marker stream
     *  picks up after sign-in without restarting consumers. */
    private val userMarkerPlaces: Flow<List<Place>>
        get() = authRepository.getCurrentUserUid()?.let { uid ->
            markerRepository.observeUserMarkers(uid)
                .map { markers -> markers.map { it.toPlace(uid) } }
        } ?: flowOf(emptyList())

    override fun observePlaces(): Flow<List<Place>> =
        combine(campusPlaces.asStateFlow(), userMarkerPlaces) { campus, custom ->
            campus + custom
        }

    override suspend fun getPlace(id: String): Place? =
        observePlaces().first().firstOrNull { it.id == id }

    override suspend fun resolveFromText(raw: String): String? =
        matcher.resolve(raw, observePlaces().first())
}

private const val MARKER_ID_PREFIX = "marker_"

private fun UserDroppedMarker.toPlace(ownerUid: String): Place = Place(
    id = "$MARKER_ID_PREFIX$id",
    name = comment.ifBlank { "Custom marker" },
    latitude = latitude,
    longitude = longitude,
    category = PlaceCategory.GENERAL,
    isCustom = true,
    ownerUid = ownerUid,
)
