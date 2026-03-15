package com.ekhonavigator.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

// --- MODELS ---
enum class PlaceCategory(val label: String) {
    ALL("All"), PARKING("Parking"), BUILDINGS("Buildings"), FOOD("Food")
}

data class CampusPlace(
    val name: String,
    val position: LatLng,
    val category: PlaceCategory,
    val details: String
)

data class UserMarker(
    val id: Long,
    val position: LatLng,
    val comment: String = ""
)

// - MAP CONTROLS
@Composable
fun MapLocationControls(
    cameraPositionState: CameraPositionState,
    context: Context,
    csuciCenter: LatLng,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    // Local state to toggle between User and CSUCI
    var nextTargetIsCampus by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.size(40.dp),
        shape = MaterialTheme.shapes.extraSmall, // Square look
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        IconButton(onClick = {
            scope.launch {
                if (nextTargetIsCampus) {
                    // Action: Slides to CSUCI
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(csuciCenter, 15f)
                    )
                    nextTargetIsCampus = false
                } else {
                    // Action: Slides to User Location
                    val fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(context)
                    try {
                        @SuppressLint("MissingPermission")
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val userLatLng = LatLng(it.latitude, it.longitude)
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(userLatLng, 16f)
                                    )
                                    nextTargetIsCampus = true
                                }
                            }
                        }
                    } catch (_: SecurityException) {
                    }
                }
            }
        }) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center Map",
                tint = if (nextTargetIsCampus) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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

    var requestLocationPermission by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        requestLocationPermission = false
    }

    LaunchedEffect(requestLocationPermission) {
        if (requestLocationPermission && !hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val droppedMarkers = remember { mutableStateListOf<UserMarker>() }

    val campusPlaces = remember {
        listOf(
            CampusPlace(
                "Library",
                LatLng(34.16283679848678, -119.04096318400194),
                PlaceCategory.BUILDINGS,
                "Study & books"
            ),
            CampusPlace(
                "Enrollment Center",
                LatLng(34.164163977501765, -119.0422077290137),
                PlaceCategory.BUILDINGS,
                "Student services"
            ),
            CampusPlace(
                "Cafeteria / Food Court",
                LatLng(34.1604931003304, -119.04159618532805),
                PlaceCategory.FOOD,
                "Food & drinks"
            ),
            CampusPlace(
                "Bell Tower",
                LatLng(34.161095875886836, -119.04307244420636),
                PlaceCategory.BUILDINGS,
                "Bell Tower"
            ),
            CampusPlace(
                "Parking Lot A3",
                LatLng(34.16667136314828, -119.0470635228976),
                PlaceCategory.PARKING,
                "Student parking"
            ),
            CampusPlace(
                "Parking Lot A4",
                LatLng(34.164284810452386, -119.046471206339),
                PlaceCategory.PARKING,
                "Permit parking"
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
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false, // hide default to use custom square one
                zoomControlsEnabled = true
            ),
            onMapLongClick = { latLng ->
                droppedMarkers.add(UserMarker(id = System.currentTimeMillis(), position = latLng))
            }
        ) {
            key("csuci-main") {
                Marker(state = rememberMarkerState(position = csuciCenter), title = "CSUCI")
            }

            visiblePlaces.forEach { place ->
                key("campus-place-${place.name}") {
                    Marker(
                        state = rememberMarkerState(position = place.position),
                        title = place.name,
                        snippet = "${place.category.label} • ${place.details}",
                        onInfoWindowClick = {
                            onEventClick(place.name)
                        }
                    )
                }
            }

            droppedMarkers.forEach { droppedMarker ->
                key("user-marker-${droppedMarker.id}") {
                    Marker(
                        state = rememberMarkerState(position = droppedMarker.position),
                        title = droppedMarker.comment.ifBlank { "Dropped Marker" },
                        snippet = "Tap bubble for options (edit/remove)",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        // tap marker = show bubble
                        // tap/long-press bubble = open dialog
                        onInfoWindowClick = {
                            selectedDroppedMarkerForOptions = droppedMarker
                        },
                        onInfoWindowLongClick = {
                            selectedDroppedMarkerForOptions = droppedMarker
                        }
                    )
                }
            }
        }

        // --- Custom Center Button ---
        if (hasLocationPermission) {
            MapLocationControls(
                cameraPositionState = cameraPositionState,
                context = context,
                csuciCenter = csuciCenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 115.dp) // Sits exactly above zoom controls
            )
        }

        // Search Card UI
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
                        text = if (isPanelExpanded) "Search & Filter (tap to collapse)" else "Search & Filter",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(text = if (isPanelExpanded) "▲" else "▼")
                }

                if (isPanelExpanded) {
                    if (!hasLocationPermission) {
                        TextButton(onClick = {
                            requestLocationPermission = true
                        }) { Text("Enable Location") }
                    }

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search campus places") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PlaceCategory.entries) { category ->
                            FilterChip(
                                selected = (selectedCategory == category),
                                onClick = { selectedCategory = category },
                                label = { Text(category.label) }
                            )
                        }
                    }
                }
            }
        }

        // --- DIALOGS ---
        if (selectedDroppedMarkerForOptions != null) {
            val selectedMarker = selectedDroppedMarkerForOptions!!
            AlertDialog(
                onDismissRequest = { selectedDroppedMarkerForOptions = null },
                title = { Text("Marker options") },
                text = {
                    Column {
                        Text(text = selectedMarker.comment.ifBlank { "Details: (none)" })
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
                    TextButton(onClick = { markerPendingRemoval = null }) { Text("Cancel") }
                }
            )
        }
    }
}