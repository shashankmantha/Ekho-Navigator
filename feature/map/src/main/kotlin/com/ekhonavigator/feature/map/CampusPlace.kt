package com.ekhonavigator.feature.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

val PlaceCategory.icon: ImageVector
    get() = when (this) {
        PlaceCategory.PARKING -> Icons.Default.LocalParking
        PlaceCategory.FOOD -> Icons.Default.Restaurant
        PlaceCategory.HOUSING -> Icons.Default.Home
        PlaceCategory.BUILDINGS -> Icons.Default.Apartment
        PlaceCategory.SERVICES -> Icons.Default.Handyman
        PlaceCategory.ALL -> Icons.Default.Place
    }

val PlaceCategory.color: Color
    @Composable
    get() = when (this) {
        PlaceCategory.PARKING -> Color(0xFF2196F3)     // blue
        PlaceCategory.FOOD -> Color(0xFFFF9800)       // orange
        PlaceCategory.HOUSING -> Color(0xFF9C27B0)    // purple
        PlaceCategory.SERVICES -> Color(0xFF4CAF50)   // green
        PlaceCategory.BUILDINGS -> MaterialTheme.colorScheme.primary
        PlaceCategory.ALL -> MaterialTheme.colorScheme.secondary
    }