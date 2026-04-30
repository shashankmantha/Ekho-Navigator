package com.ekhonavigator.core.data.route

import kotlinx.serialization.Serializable

/**
 * Supported travel modes for in-app directions.
 */
enum class TravelMode {
    WALK,
    DRIVE
}

/**
 * Simple representation of a geographical point for routing.
 */
@Serializable
data class RouteLocation(
    val latitude: Double,
    val longitude: Double
)

/**
 * Request body for the Google Routes API v2 computeRoutes endpoint.
 */
@Serializable
data class ComputeRouteRequest(
    val originLocation: RouteWaypoint,
    val destinationLocation: RouteWaypoint,
    val travelModeType: String,
    val routingPreference: String? = null,
    val computeAlternativeRoutes: Boolean = false,
    val languageCode: String = "en-US",
    val measurementUnits: String = "IMPERIAL"
)

@Serializable
data class RouteWaypoint(
    val locationContainer: LocationWrapper
)

@Serializable
data class LocationWrapper(
    val latLngCoordinates: RouteLocation
)

/**
 * Response body from the Google Routes API v2.
 */
@Serializable
data class ComputeRouteResponse(
    val routesList: List<RouteResult> = emptyList()
)

@Serializable
data class RouteResult(
    val durationInSeconds: String? = null,
    val distanceInMeters: Int? = null,
    val polylinePath: EncodedPolyline? = null
)

@Serializable
data class EncodedPolyline(
    val encodedPolylinePathString: String
)