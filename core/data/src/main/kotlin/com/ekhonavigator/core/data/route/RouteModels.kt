package com.ekhonavigator.core.data.route

import kotlinx.serialization.SerialName
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
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double
)

/**
 * Request body for the Google Routes API v2.
 */
@Serializable
data class ComputeRouteRequest(
    @SerialName("origin") val originLocation: RouteWaypoint,
    @SerialName("destination") val destinationLocation: RouteWaypoint,
    @SerialName("travelMode") val travelModeType: String,
    @SerialName("routingPreference") val routingPreference: String? = null,
    @SerialName("computeAlternativeRoutes") val computeAlternativeRoutes: Boolean = false,
    @SerialName("languageCode") val languageCode: String = "en-US",
    @SerialName("units") val measurementUnits: String = "IMPERIAL"
)

@Serializable
data class RouteWaypoint(
    @SerialName("location") val locationContainer: LocationWrapper
)

@Serializable
data class LocationWrapper(
    @SerialName("latLng") val latLngCoordinates: RouteLocation
)

/**
 * Response body from the Google Routes API v2.
 */
@Serializable
data class ComputeRouteResponse(
    @SerialName("routes") val routesList: List<RouteResult> = emptyList()
)

@Serializable
data class RouteResult(
    @SerialName("duration") val durationInSeconds: String? = null,
    @SerialName("distanceMeters") val distanceInMeters: Int? = null,
    @SerialName("polyline") val polylinePath: EncodedPolyline? = null
)

@Serializable
data class EncodedPolyline(
    @SerialName("encodedPolyline") val encodedPolylinePathString: String
)