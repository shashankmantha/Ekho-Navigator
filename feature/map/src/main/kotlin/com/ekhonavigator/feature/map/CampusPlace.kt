package com.ekhonavigator.feature.map

import com.ekhonavigator.core.model.Place
import com.google.android.gms.maps.model.LatLng
import com.ekhonavigator.core.model.PlaceCategory as CorePlaceCategory

enum class PlaceCategory(val label: String) {
    ALL("All"), PARKING("Parking"), BUILDINGS("Buildings"),
    FOOD("Food"), HOUSING("Housing"), SERVICES("Services")
}

data class CampusPlace(
    val id: String,
    val name: String,
    val position: LatLng,
    val category: PlaceCategory,
    val fullLocationDescription: String,
    val quickPreviewSummary: String = "",
    val studentVisitReasons: String = "",
    val keyServicesOffered: String = "",
    val studentProTip: String = "",
    val campusOfficePhoneNumber: String? = null,
    val aliases: List<String> = emptyList(),
)

fun CampusPlace.toPlace(): Place = Place(
    id = id,
    name = name,
    latitude = position.latitude,
    longitude = position.longitude,
    category = category.toCoreCategory(),
    aliases = aliases,
    isCustom = false,
)

private fun PlaceCategory.toCoreCategory(): CorePlaceCategory = when (this) {
    PlaceCategory.PARKING -> CorePlaceCategory.PARKING
    PlaceCategory.BUILDINGS -> CorePlaceCategory.BUILDINGS
    PlaceCategory.FOOD -> CorePlaceCategory.FOOD
    PlaceCategory.HOUSING -> CorePlaceCategory.HOUSING
    PlaceCategory.SERVICES -> CorePlaceCategory.SERVICES
    PlaceCategory.ALL -> CorePlaceCategory.GENERAL
}
