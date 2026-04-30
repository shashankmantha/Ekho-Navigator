package com.ekhonavigator.core.data.route

import com.ekhonavigator.core.data.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val networkClient: OkHttpClient,
    private val jsonSerializer: Json
) {
    private val directionsApiKey = BuildConfig.ROUTES_API_KEY
    private val routesApiEndpoint = "https://routes.googleapis.com/directions/v2:computeRoutes"

    /**
     * Fetches a route between two points and returns the list of coordinates.
     */
    suspend fun fetchRouteBetweenPoints(
        startLocation: LatLng,
        endLocation: LatLng,
        travelMode: TravelMode
    ): List<LatLng> = withContext(Dispatchers.IO) {

        val requestBody = createRoutingRequestBody(startLocation, endLocation, travelMode)
        val httpRequest = createHttpRequest(requestBody)

        val networkResponse = networkClient.newCall(httpRequest).execute()

        if (!networkResponse.isSuccessful) return@withContext emptyList()

        val responseBodyString = networkResponse.body.string()
        val parsedResponse = jsonSerializer.decodeFromString<ComputeRouteResponse>(responseBodyString)

        val encodedPathString = parsedResponse.routesList.firstOrNull()?.polylinePath?.encodedPolylinePathString

        return@withContext if (encodedPathString != null) {
            PolylineDecoder.decodeEncodedPath(encodedPathString)
        } else {
            emptyList()
        }
    }

    private fun createRoutingRequestBody(
        start: LatLng,
        end: LatLng,
        mode: TravelMode
    ): String {
        val routingRequest = ComputeRouteRequest(
            originLocation = RouteWaypoint(LocationWrapper(RouteLocation(start.latitude, start.longitude))),
            destinationLocation = RouteWaypoint(LocationWrapper(RouteLocation(end.latitude, end.longitude))),
            travelModeType = if (mode == TravelMode.WALK) "WALK" else "DRIVE",
            routingPreference = if (mode == TravelMode.DRIVE) "TRAFFIC_AWARE" else null
        )
        return jsonSerializer.encodeToString(ComputeRouteRequest.serializer(), routingRequest)
    }

    private fun createHttpRequest(jsonBody: String): Request {
        val mediaType = "application/json".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        return Request.Builder()
            .url(routesApiEndpoint)
            .post(requestBody)
            .addHeader("X-Goog-Api-Key", directionsApiKey)
            .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline")
            .build()
    }
}