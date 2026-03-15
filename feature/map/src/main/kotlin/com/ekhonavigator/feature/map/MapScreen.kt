package com.ekhonavigator.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


// Categories
enum class PlaceCategory(val label: String) {
    ALL("All"),
    PARKING("Parking"),
    BUILDINGS("Buildings"),
    FOOD("Food")
}

// POI model
data class CampusPlace(
    val name: String,
    val position: LatLng,
    val category: PlaceCategory,
    val details: String
)

// User dropped marker model
data class UserMarker(
    val id: Long,
    val position: LatLng,
    val comment: String = ""
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val csuciCenter = LatLng(34.162120, -119.043167)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(csuciCenter, 15f)
    }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCenteredOnUserOnce by remember { mutableStateOf(false) }
    var requestLocationPermission by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        requestLocationPermission = false
        if (granted) hasCenteredOnUserOnce = false
    }

    LaunchedEffect(requestLocationPermission) {
        if (requestLocationPermission && !hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !hasCenteredOnUserOnce) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null && !hasCenteredOnUserOnce) {
                        val myLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(myLatLng, 16f)
                        hasCenteredOnUserOnce = true
                    }
                }
            } catch (e: SecurityException) {
                // Handle case where permission was revoked
            }
        }
    }

    val droppedMarkers = remember { mutableStateListOf<UserMarker>() }

    val campusPlaces = remember {
        listOf(
            CampusPlace(
                name = "Library",
                position = LatLng(34.16283679848678, -119.04096318400194),
                category = PlaceCategory.BUILDINGS,
                details = "Study & books"
            ),
            CampusPlace(
                name = "Enrollment Center",
                position = LatLng(34.164163977501765, -119.0422077290137),
                category = PlaceCategory.BUILDINGS,
                details = "Student services"
            ),
            CampusPlace(
                name = "Cafeteria / Food Court",
                position = LatLng(34.1604931003304, -119.04159618532805),
                category = PlaceCategory.FOOD,
                details = "Food & drinks"
            ),
            CampusPlace(
                name = "Bell Tower",
                position = LatLng(34.161095875886836, -119.04307244420636),
                category = PlaceCategory.BUILDINGS,
                details = "Bell Tower"
            ),
            CampusPlace(
                name = "Parking Lot A3",
                position = LatLng(34.16667136314828, -119.0470635228976),
                category = PlaceCategory.PARKING,
                details = "Student parking"
            ),
            CampusPlace(
                name = "Parking Lot A4",
                position = LatLng(34.164284810452386, -119.046471206339),
                category = PlaceCategory.PARKING,
                details = "Permit parking"
            )
        )
    }

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.ALL) }
    var isPanelExpanded by remember { mutableStateOf(true) }

    var selectedDroppedMarkerForOptions by remember { mutableStateOf<UserMarker?>(null) }
    var markerBeingEdited by remember { mutableStateOf<UserMarker?>(null) }
    var editLabelText by remember { mutableStateOf("") }
    var markerPendingRemoval by remember { mutableStateOf<UserMarker?>(null) }

    val visiblePlaces = campusPlaces.filter { place ->
        val matchesCategory =
            (selectedCategory == PlaceCategory.ALL) || (place.category == selectedCategory)
        val matchesSearch = place.name.contains(searchText, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission),
            onMapClick = { latLng ->
                droppedMarkers.add(UserMarker(id = System.currentTimeMillis(), position = latLng))
            }
        ) {
            Marker(state = rememberMarkerState(position = csuciCenter), title = "CSUCI")

            visiblePlaces.forEach { place ->
                Marker(
                    state = rememberMarkerState(position = place.position),
                    title = place.name,
                    snippet = "${place.category.label} • ${place.details}"
                )
            }

            droppedMarkers.forEach { droppedMarker ->
                Marker(
                    state = rememberMarkerState(position = droppedMarker.position),
                    title = droppedMarker.comment.ifBlank { "Dropped Marker" },
                    snippet = "Tap for options (edit/remove)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {
                        selectedDroppedMarkerForOptions = droppedMarker
                        true
                    }
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPanelExpanded = !isPanelExpanded }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPanelExpanded) "Search & Filter (tap to collapse)"
                        else "Search & Filter (tap to expand)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = if (isPanelExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (isPanelExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!hasLocationPermission) {
                        TextButton(onClick = { requestLocationPermission = true }) {
                            Text("Enable Location")
                        }
                        Text(
                            "Location is off until you allow it.",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search campus places") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Filter:", style = MaterialTheme.typography.labelLarge)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PlaceCategory.entries) { category ->
                            FilterChip(
                                selected = (selectedCategory == category),
                                onClick = { selectedCategory = category },
                                label = { Text(category.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Showing ${visiblePlaces.size} place(s) • Dropped markers: ${droppedMarkers.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Dialogs (Edit/Remove) - Implementation stays the same as your mockup...
        if (selectedDroppedMarkerForOptions != null) {
            val selectedMarker = selectedDroppedMarkerForOptions!!
            AlertDialog(
                onDismissRequest = { selectedDroppedMarkerForOptions = null },
                title = { Text("Marker options") },
                text = {
                    Column {
                        Text(text = selectedMarker.comment.ifBlank { "Details: (none)" })
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = {
                            selectedDroppedMarkerForOptions = null
                            markerBeingEdited = selectedMarker
                            editLabelText = selectedMarker.comment
                        }) { Text("Edit label") }
                        TextButton(onClick = {
                            selectedDroppedMarkerForOptions = null
                            markerPendingRemoval = selectedMarker
                        }) { Text("Remove") }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDroppedMarkerForOptions = null
                    }) { Text("Cancel") }
                }
            )
        }

        if (markerBeingEdited != null) {
            AlertDialog(
                onDismissRequest = { markerBeingEdited = null },
                title = { Text("Edit marker label") },
                text = {
                    OutlinedTextField(
                        value = editLabelText,
                        onValueChange = { editLabelText = it },
                        label = { Text("Comment / detail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val index = droppedMarkers.indexOfFirst { it.id == markerBeingEdited?.id }
                        if (index != -1) droppedMarkers[index] =
                            droppedMarkers[index].copy(comment = editLabelText.trim())
                        markerBeingEdited = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        markerBeingEdited = null
                    }) { Text("Cancel") }
                }
            )
        }

        if (markerPendingRemoval != null) {
            AlertDialog(
                onDismissRequest = { markerPendingRemoval = null },
                title = { Text("Remove marker?") },
                text = { Text("Do you want to remove this dropped marker?") },
                confirmButton = {
                    TextButton(onClick = {
                        droppedMarkers.remove(markerPendingRemoval)
                        markerPendingRemoval = null
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        markerPendingRemoval = null
                    }) { Text("Cancel") }
                }
            )
        }
    }
}