package com.ekhonavigator.feature.map

import com.google.android.gms.maps.model.LatLng

enum class PlaceCategory(val label: String) {
    ALL("All"), PARKING("Parking"), BUILDINGS("Buildings"),
    FOOD("Food"), HOUSING("Housing"), SERVICES("Services")
}

data class CampusPlace(
    val name: String,
    val position: LatLng,
    val category: PlaceCategory,
    val fullLocationDescription: String,
    val quickPreviewSummary: String = "",
    val studentVisitReasons: String = "",
    val keyServicesOffered: String = "",
    val studentProTip: String = "",
    val campusOfficePhoneNumber: String? = null
)