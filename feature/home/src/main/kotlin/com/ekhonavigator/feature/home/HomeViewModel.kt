package com.ekhonavigator.feature.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CalendarRepository,
) : ViewModel() {

    // ---- Event filter state ----

    /** When true (default), show every event. When false, only bookmarked. */
    private val _showAll = MutableStateFlow(true)
    val showAll: StateFlow<Boolean> = _showAll.asStateFlow()

    /** All events, optionally filtered to bookmarked-only. */
    val events: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _showAll,
    ) { allEvents, showAll ->
        if (showAll) allEvents else allEvents.filter { it.isBookmarked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleShowAll() {
        _showAll.value = !_showAll.value
    }

    // ---- Weather state ----

    private val _weatherState = MutableStateFlow(WeatherUiState())
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    fun loadWeather(context: Context) {
        // Skip if already loaded or currently loading
        val current = _weatherState.value
        if (!current.isLoading && current.currentTemperature != "--°F") return

        _weatherState.value = current.copy(isLoading = true)
        viewModelScope.launch {
            _weatherState.value = fetchWeatherState(context)
        }
    }

    fun onPermissionDenied() {
        _weatherState.value = WeatherUiState(
            isLoading = false,
            conditionLabel = "Location permission needed",
            locationLabel = "Tap Allow to see your local weather",
            errorMessage = "Allow location to load current weather.",
        )
    }

    // ---- Weather data loading ----

    private suspend fun fetchWeatherState(context: Context): WeatherUiState {
        return try {
            val location = getLastLocation(context)
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
                locationLabel = "\uD83D\uDCCD ${formatLocationLabel(location.latitude, location.longitude)}",
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
    private suspend fun getLastLocation(context: Context): android.location.Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            client.lastLocation.await()
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
                    "&timezone=auto",
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

            if (dateTime.toLocalDate() != today) return@mapNotNull null

            HourlyForecastUi(
                timeLabel = dateTime.format(formatter),
                temperatureLabel = fahrenheitLabel(response.hourly.temperature2m[index]),
                conditionLabel = weatherCodeToLabel(response.hourly.weatherCode[index]),
            )
        }
    }

    private fun buildHighLowLabel(hours: List<HourlyForecastUi>): String {
        val temps = hours.mapNotNull { it.temperatureLabel.removeSuffix("°F").toIntOrNull() }
        if (temps.isEmpty()) return "H --°  L --°"
        return "H ${temps.max()}°  L ${temps.min()}°"
    }
}

// ---- Weather UI state ----

data class WeatherUiState(
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

data class HourlyForecastUi(
    val timeLabel: String,
    val temperatureLabel: String,
    val conditionLabel: String,
)

// ---- Network models (private to this file) ----

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

// ---- Helpers ----

private fun fahrenheitLabel(value: Double): String = "${value.roundToInt()}°F"

private fun formatLocationLabel(latitude: Double, longitude: Double): String =
    "${"%.3f".format(latitude)}, ${"%.3f".format(longitude)}"

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
