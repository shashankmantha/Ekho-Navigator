package com.ekhonavigator.feature.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekhonavigator.core.data.route.TravelMode
import com.ekhonavigator.core.model.SharedLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Canvas as ComposeCanvas


import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.graphics.vector.ImageVector

data class UserMarker(
    val id: Long,
    val droppedMarkerLocation: LatLng,
    val markerLabelComment: String = "",
    val iconType: MarkerIconType = MarkerIconType.PIN
)

enum class MarkerIconType(val icon: ImageVector) {
    PIN(Icons.Default.Place),
    HOME(Icons.Default.Home),
    FOOD(Icons.Default.Restaurant),
    PARKING(Icons.Default.LocalParking),
    STAR(Icons.Default.Star),
    FAVORITE(Icons.Default.Favorite),
    FLAG(Icons.Default.Flag)
}

private const val MARKER_FOCUS_PREFIX = "marker_"

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
    onShareLocationToChat: (friendId: String, friendName: String, SharedLocation) -> Unit,
    onOpenDiscoverForPlace: (placeId: String) -> Unit,
    focusPlaceId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val activeRoutePoints by viewModel.activeRoutePoints.collectAsStateWithLifecycle()
    val activeTravelMode by viewModel.activeTravelMode.collectAsStateWithLifecycle()
    val isRouteLoading by viewModel.isRouteLoading.collectAsStateWithLifecycle()

    val csuciCenter = LatLng(34.162039205474755, -119.04347659500405)

    val focusedPlace = remember(focusPlaceId) {
        focusPlaceId?.let { id -> CampusPlacesData.places.firstOrNull { it.id == id } }
    }

    // User markers may not be loaded yet when navigation arrives; track via derivedStateOf
    // so the camera animates as soon as the matching marker streams in from Firestore.
    val focusedUserMarker by remember(focusPlaceId) {
        derivedStateOf {
            val rawId = focusPlaceId
                ?.takeIf { it.startsWith(MARKER_FOCUS_PREFIX) }
                ?.removePrefix(MARKER_FOCUS_PREFIX)
                ?.toLongOrNull()
                ?: return@derivedStateOf null
            viewModel.droppedMarkers.firstOrNull { it.id == rawId }
        }
    }

    val focusTarget: LatLng? = focusedPlace?.position ?: focusedUserMarker?.droppedMarkerLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(csuciCenter, 15f)
    }

    val mapStyle = if (isSystemInDarkTheme()) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    } else {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_light)
    }

    var isMapLoaded by remember { mutableStateOf(false) }

    // Animate (not initial position) — contentPadding is only honored on CameraUpdate moves.
    // Gate on isMapLoaded to avoid the world-view flash from racing the SDK's map init.
    LaunchedEffect(focusTarget, isMapLoaded) {
        val target = focusTarget
        if (target != null && isMapLoaded) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 17f),
            )
        }
    }

    var selectedCampusPlace by remember { mutableStateOf<CampusPlace?>(null) }
    var isAnyMarkerInfoShowing by remember { mutableStateOf(false) }

    // Permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClientForRouting =
        remember { LocationServices.getFusedLocationProviderClient(context) }
    var userCurrentLocationForRouting by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000
            ).build()

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let {
                        userCurrentLocationForRouting = LatLng(it.latitude, it.longitude)
                    }
                }
            }

            fusedLocationClientForRouting.requestLocationUpdates(
                locationRequest, locationCallback, android.os.Looper.getMainLooper()
            )
        }
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

    val droppedMarkers = viewModel.droppedMarkers

    // for campus location markers in CampusPlacesData.kt
    val campusPlaces = remember { CampusPlacesData.places }

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(focusedPlace?.category ?: PlaceCategory.BUILDINGS)
    }
    var isPanelExpanded by remember { mutableStateOf(true) }
    var showFilterTip by remember { mutableStateOf(focusPlaceId == null) }

    val mapPaddingForInfoCards by remember(isPanelExpanded, showFilterTip, selectedCategory) {
        derivedStateOf {
            val isFilterTipVisible = showFilterTip && selectedCategory == PlaceCategory.BUILDINGS

            val collapsedFilterHeight = 80.dp
            val expandedFilterHeight = 220.dp
            val expandedFilterWithTipHeight = 300.dp

            val totalTopPadding = when {
                isPanelExpanded && isFilterTipVisible -> expandedFilterWithTipHeight
                isPanelExpanded -> expandedFilterHeight
                else -> collapsedFilterHeight
            }

            PaddingValues(top = totalTopPadding)
        }
    }

    // Hides the tip automatically after 20 seconds
    LaunchedEffect(Unit) {
        delay(20000)
        showFilterTip = false
    }

    var selectedDroppedMarkerForOptions by remember { mutableStateOf<UserMarker?>(null) }
    var showFriendPickerForMarker by remember { mutableStateOf<UserMarker?>(null) }
    var markerBeingEdited by remember { mutableStateOf<UserMarker?>(null) }
    var editLabelText by remember { mutableStateOf("") }
    var markerPendingRemoval by remember { mutableStateOf<UserMarker?>(null) }

    val visiblePlaces = campusPlaces.filter { place ->
        val matchesSearch = place.name.contains(searchText, ignoreCase = true)

        // 1. If user is typing, we ignore categories (matchesCategory = true)
        // 2. If search is empty, we respect the category chips
        val matchesCategory = if (searchText.isNotBlank()) {
            true
        } else {
            (selectedCategory == PlaceCategory.ALL) || (place.category == selectedCategory)
        }

        matchesCategory && matchesSearch
    }
    // Checks if user zoomed in enough to see the building icons.
    // using derivedStateOf so the map doesn't lag when pinching/zooming.
    val zoomRevealsCampusMarkers by remember {
        derivedStateOf { cameraPositionState.position.zoom >= 16f }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = mapPaddingForInfoCards,
            properties = MapProperties(
                mapStyleOptions = mapStyle,
                isMyLocationEnabled = hasLocationPermission &&
                        !isAnyMarkerInfoShowing &&
                        selectedCampusPlace == null &&
                        selectedDroppedMarkerForOptions == null
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = true,
                mapToolbarEnabled = false
            ),
            onMapClick = { isAnyMarkerInfoShowing = false },
            onMapLongClick = { latLng ->
                viewModel.addMarker(latLng)
            },
            onMapLoaded = { isMapLoaded = true },
        ) {
            key("csuci-main") {
                Marker(
                    state = rememberMarkerState(position = csuciCenter),
                    alpha = 0f,
                    title = "CSUCI Central Mall"
                )
            }
            // Only show the campus markers if the zoom is high enough.
            // Only shows markers if zoomed in OR if the user has typed something in the search bar
            if (zoomRevealsCampusMarkers || searchText.isNotBlank()) {
                visiblePlaces.forEach { place ->
                    key("campus-place-${place.name}") {
                        val markerState = rememberMarkerState(position = place.position)
                        if (place.id == focusPlaceId) {
                            LaunchedEffect(focusPlaceId) {
                                markerState.showInfoWindow()
                            }
                        }
                        val markerIcon = rememberMarkerIcon(
                            imageVector = place.category.icon,
                            color = place.category.color
                        )
                        MarkerInfoWindow(
                            state = markerState,
                            icon = markerIcon,
                            onClick = {
                                isAnyMarkerInfoShowing = true
                                false
                            },
                            onInfoWindowClick = {
                                selectedCampusPlace = place
                            },
                            content = {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 4.dp
                                ) {
                                    CampusPlacePreviewCard(place = place)
                                }
                            }
                        )
                    }
                }
            }

            droppedMarkers.forEach { droppedMarker ->
                key("user-marker-${droppedMarker.id}") {
                    val markerState =
                        rememberMarkerState(position = droppedMarker.droppedMarkerLocation)
                    if (focusPlaceId == "$MARKER_FOCUS_PREFIX${droppedMarker.id}") {
                        LaunchedEffect(focusPlaceId) {
                            markerState.showInfoWindow()
                        }
                    }
                    val markerIcon = rememberMarkerIcon(
                        imageVector = droppedMarker.iconType.icon,
                        color = Color(0xFF2196F3) // Azure Blue for custom markers
                    )
                    MarkerInfoWindow(
                        state = markerState,
                        icon = markerIcon,
                        onInfoWindowClick = {
                            selectedDroppedMarkerForOptions = droppedMarker
                        },
                        onInfoWindowLongClick = {
                            selectedDroppedMarkerForOptions = droppedMarker
                        },
                        content = {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = droppedMarker.markerLabelComment.ifBlank { "Dropped Marker" },
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Tap bubble for options (edit/remove)",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    )
                }
            }
            if (activeRoutePoints.isNotEmpty()) {
                com.google.maps.android.compose.Polyline(
                    points = activeRoutePoints,
                    color = androidx.compose.ui.graphics.Color(0xFF2196F3),
                    width = 12f,
                    pattern = if (activeTravelMode == TravelMode.WALK) {
                        listOf(Dot(), Gap(20f))
                    } else {
                        null
                    },
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
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
                    .padding(end = 11.dp, bottom = 100.dp) // Sits exactly above zoom controls
            )
        }

        if (activeRoutePoints.isNotEmpty()) {
            androidx.compose.material3.SmallFloatingActionButton(
                onClick = { viewModel.clearActiveRoute() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 140.dp), // Sits exactly above target button
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Route"
                )
            }
        }

        // Search & Filter Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRouteLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Search Card UI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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
                                val categoryColor = category.color
                                FilterChip(
                                    selected = (selectedCategory == category),
                                    onClick = { selectedCategory = category },
                                    label = { Text(category.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = categoryColor,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.Transparent,
                                        labelColor = categoryColor
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedCategory == category,
                                        borderColor = categoryColor,
                                        selectedBorderColor = Color.Transparent,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            // FLOATING TIP MESSAGE
            // floats centered directly under the card
            AnimatedVisibility(
                visible = showFilterTip && selectedCategory == PlaceCategory.BUILDINGS,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.clickable { showFilterTip = false },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Zoom in to see points of interest. Click filters to see even more.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Hold anywhere on the map to drop a custom marker.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("✕", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (selectedDroppedMarkerForOptions != null) {
            val selectedMarkerId = selectedDroppedMarkerForOptions!!.id
            val selectedMarker = droppedMarkers.find { it.id == selectedMarkerId }
                ?: selectedDroppedMarkerForOptions!!

            AlertDialog(
                onDismissRequest = { selectedDroppedMarkerForOptions = null },
                title = { Text("Marker options") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = selectedMarker.markerLabelComment.ifBlank { "Details: (none)" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Choose Icon:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(MarkerIconType.entries) { iconType ->
                                AssistChip(
                                    onClick = {
                                        viewModel.updateMarkerIcon(selectedMarker.id, iconType)
                                    },
                                    label = {
                                        Icon(
                                            imageVector = iconType.icon,
                                            contentDescription = iconType.name,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (selectedMarker.iconType == iconType) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(7.dp))

                        TextButton(onClick = {
                            selectedDroppedMarkerForOptions = null
                            markerBeingEdited = selectedMarker
                            editLabelText = selectedMarker.markerLabelComment
                        }) { Text("Edit label") }

                        TextButton(onClick = {
                            selectedDroppedMarkerForOptions = null
                            markerPendingRemoval = selectedMarker
                        }) { Text("Remove") }

                        TextButton(onClick = {
                            selectedDroppedMarkerForOptions = null
                            showFriendPickerForMarker = selectedMarker
                        }) { Text("Send to Friend") }

                        // Navigation Row at the bottom
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                selectedDroppedMarkerForOptions = null
                                viewModel.getDirectionsToDestination(
                                    selectedMarker.droppedMarkerLocation,
                                    TravelMode.WALK,
                                    userCurrentLocationForRouting
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = "Walk"
                                )
                            }
                            IconButton(onClick = {
                                selectedDroppedMarkerForOptions = null
                                viewModel.getDirectionsToDestination(
                                    selectedMarker.droppedMarkerLocation,
                                    TravelMode.DRIVE,
                                    userCurrentLocationForRouting
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Drive"
                                )
                            }
                        }
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
                        markerBeingEdited?.let { marker ->        // ViewModel handles the update and the Firebase sync
                            viewModel.updateMarkerLabel(marker.id, editLabelText.trim())
                        }
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
                        markerPendingRemoval?.let { marker ->
                            viewModel.removeMarker(marker)           // removes marker from the screen AND Firebase
                        }
                        markerPendingRemoval = null
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { markerPendingRemoval = null }) { Text("Cancel") }
                }
            )
        }

        if (showFriendPickerForMarker != null) {
            val marker = showFriendPickerForMarker!!
            FriendPickerCard(
                markerLabel = marker.markerLabelComment.ifBlank { "Dropped Marker" },
                searchText = viewModel.searchTextForFriendPicker,
                onSearchTextChange = { newText ->
                    viewModel.updateSearchTextForFriendPicker(newText)
                },
                friends = viewModel.friendsListMatchingSearchQuery,
                onFriendSelected = { friend ->
                    showFriendPickerForMarker = null
                    viewModel.updateSearchTextForFriendPicker("") // Reset on success
                    val sharedLoc = SharedLocation(
                        title = marker.markerLabelComment.ifBlank { "Dropped Marker" },
                        latitude = marker.droppedMarkerLocation.latitude,
                        longitude = marker.droppedMarkerLocation.longitude
                    )
                    onShareLocationToChat(friend.id, friend.name, sharedLoc)
                },
                onDismiss = {
                    showFriendPickerForMarker = null
                    viewModel.updateSearchTextForFriendPicker("") // Reset on cancel
                }
            )
        }

        selectedCampusPlace?.let { place ->
            CampusPlaceDetailCard(
                place = place,
                onDismiss = { selectedCampusPlace = null },
                onViewLocationEvents = {
                    selectedCampusPlace = null
                    onOpenDiscoverForPlace(place.id)
                },
                onGetWalkingDirections = {
                    selectedCampusPlace = null
                    viewModel.getDirectionsToDestination(
                        place.position,
                        TravelMode.WALK,
                        userCurrentLocationForRouting
                    )
                },
                onGetDrivingDirections = {
                    selectedCampusPlace = null
                    viewModel.getDirectionsToDestination(
                        place.position,
                        TravelMode.DRIVE,
                        userCurrentLocationForRouting
                    )
                }
            )
        }
    }
}

@Composable
fun rememberMarkerIcon(
    imageVector: ImageVector,
    color: Color
): com.google.android.gms.maps.model.BitmapDescriptor {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(imageVector)

    return androidx.compose.runtime.remember(imageVector, color) {
        val sizePx = with(density) { 27.dp.toPx() }

        val bitmap = Bitmap.createBitmap(
            sizePx.toInt(),
            sizePx.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = ComposeCanvas(canvas),
            size = Size(sizePx, sizePx)
        ) {
            with(painter) {
                draw(
                    size = size,
                    colorFilter = ColorFilter.tint(color)
                )
            }
        }

        com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
