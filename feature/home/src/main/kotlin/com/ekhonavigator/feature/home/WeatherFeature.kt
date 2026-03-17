package com.ekhonavigator.feature.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ekhonavigator.core.designsystem.icon.EkhoIcons
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private data class WeatherUiState(
    val isLoading: Boolean = true,
    val currentTemperature: String = "--°F",
    val conditionLabel: String = "Loading weather...",
    val locationLabel: String = "Getting your location",
    val windLabel: String = "-- mph",
    val humidityLabel: String = "--%",
    val highLowLabel: String = "H --°  L --°",
    val hourlyForecast: List<HourlyForecastUi> = emptyList(),
    val errorMessage: String? = null,
)

private data class HourlyForecastUi(
    val timeLabel: String,
    val temperatureLabel: String,
    val conditionLabel: String,
)

@Serializable
private data class OpenMeteoResponse(
    @SerialName("current") val current: CurrentWeather,
    @SerialName("hourly") val hourly: HourlyWeather,
)

@Serializable
private data class CurrentWeather(
    @SerialName("temperature_2m") val temperature2m: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int,
    @SerialName("wind_speed_10m") val windSpeed10m: Double,
    @SerialName("weather_code") val weatherCode: Int,
)

@Serializable
private data class HourlyWeather(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
)

@Composable
internal fun WeatherSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val weatherState by produceState(
        initialValue = WeatherUiState(
            isLoading = hasLocationPermission,
            conditionLabel = if (hasLocationPermission) "Loading weather..." else "Location permission needed",
            locationLabel = if (hasLocationPermission) "Getting your location" else "Tap Allow to see your local weather",
        ),
        key1 = hasLocationPermission,
    ) {
        value = if (!hasLocationPermission) {
            WeatherUiState(
                isLoading = false,
                conditionLabel = "Location permission needed",
                locationLabel = "Tap Allow to see your local weather",
                errorMessage = "Allow location to load current weather.",
            )
        } else {
            loadWeatherState(context)
        }
    }

    var showFullDayForecast by remember { mutableStateOf(false) }

    WeatherCard(
        state = weatherState,
        onClick = {
            if (weatherState.hourlyForecast.isNotEmpty()) {
                showFullDayForecast = true
            }
        },
        modifier = modifier,
    )

    if (showFullDayForecast) {
        FullDayForecastDialog(
            state = weatherState,
            onDismiss = { showFullDayForecast = false },
        )
    }
}

@Composable
private fun WeatherCard(
    state: WeatherUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = state.hourlyForecast.isNotEmpty(), onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1565C0), Color(0xFF1E88E5), Color(0xFF42A5F5)),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentTemperature,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = EkhoIcons.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = state.conditionLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.locationLabel,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        WeatherPill(EkhoIcons.Air, state.windLabel)
                        Spacer(Modifier.height(6.dp))
                        WeatherPill(EkhoIcons.WaterDrop, state.humidityLabel)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (state.hourlyForecast.isNotEmpty()) {
                            "Tap for full-day forecast"
                        } else {
                            state.errorMessage ?: "Weather unavailable"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = state.highLowLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun WeatherPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White.copy(alpha = 0.9f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FullDayForecastDialog(
    state: WeatherUiState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Today's Forecast")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.locationLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.hourlyForecast) { hour ->
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = hour.timeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = hour.temperatureLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = hour.conditionLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private suspend fun loadWeatherState(context: Context): WeatherUiState {
    return try {
        val location = getCurrentLocation(context)
            ?: return WeatherUiState(
                isLoading = false,
                conditionLabel = "Location unavailable",
                locationLabel = "Turn on location and try again",
                errorMessage = "Couldn't get current location.",
            )

        val response = fetchWeather(location.latitude, location.longitude)
        val todayForecast = buildTodayForecast(response)

        WeatherUiState(
            isLoading = false,
            currentTemperature = fahrenheitLabel(response.current.temperature2m),
            conditionLabel = weatherCodeToLabel(response.current.weatherCode),
            locationLabel = "📍 ${formatLocationLabel(location.latitude, location.longitude)}",
            windLabel = "${response.current.windSpeed10m.roundToInt()} mph",
            humidityLabel = "${response.current.relativeHumidity2m}%",
            highLowLabel = buildHighLowLabel(todayForecast),
            hourlyForecast = todayForecast,
        )
    } catch (error: Exception) {
        WeatherUiState(
            isLoading = false,
            conditionLabel = "Weather unavailable",
            locationLabel = "Unable to reach weather service",
            errorMessage = error.message ?: "Unknown error",
        )
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: Context): android.location.Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    return try {
        fusedLocationClient.lastLocation.await()
    } catch (_: Exception) {
        null
    }
}

private suspend fun fetchWeather(latitude: Double, longitude: Double): OpenMeteoResponse {
    return withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code" +
                "&hourly=temperature_2m,weather_code" +
                "&temperature_unit=fahrenheit" +
                "&wind_speed_unit=mph" +
                "&timezone=auto"
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
        }

        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<OpenMeteoResponse>(body)
        } finally {
            connection.disconnect()
        }
    }
}

private fun buildTodayForecast(response: OpenMeteoResponse): List<HourlyForecastUi> {
    val deviceZone = ZoneId.systemDefault()
    val today = LocalDateTime.now(deviceZone).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("h:mm a")

    return response.hourly.time.indices.mapNotNull { index ->
        val dateTime = runCatching {
            LocalDateTime.parse(response.hourly.time[index])
        }.getOrNull() ?: return@mapNotNull null

        if (dateTime.toLocalDate() != today) {
            return@mapNotNull null
        }

        HourlyForecastUi(
            timeLabel = dateTime.format(formatter),
            temperatureLabel = fahrenheitLabel(response.hourly.temperature2m[index]),
            conditionLabel = weatherCodeToLabel(response.hourly.weatherCode[index]),
        )
    }
}

private fun buildHighLowLabel(hours: List<HourlyForecastUi>): String {
    val temps = hours.mapNotNull { hour ->
        hour.temperatureLabel.removeSuffix("°F").toIntOrNull()
    }
    if (temps.isEmpty()) {
        return "H --°  L --°"
    }
    return "H ${temps.maxOrNull()}°  L ${temps.minOrNull()}°"
}

private fun fahrenheitLabel(value: Double): String = "${value.roundToInt()}°F"

private fun formatLocationLabel(latitude: Double, longitude: Double): String {
    return "${"%.3f".format(latitude)}, ${"%.3f".format(longitude)}"
}

private fun weatherCodeToLabel(code: Int): String = when (code) {
    0 -> "Clear sky"
    1, 2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
    71, 73, 75, 77, 85, 86 -> "Snow"
    95, 96, 99 -> "Thunderstorm"
    else -> "Weather update"
}
