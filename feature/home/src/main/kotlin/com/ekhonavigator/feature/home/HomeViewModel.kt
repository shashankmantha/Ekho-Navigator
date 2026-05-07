package com.ekhonavigator.feature.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
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
    private val nudgeStore: HomeNudgeStore,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    // customEventRepository.startSync() removed when AuthLifecycleObserver took
    // over the boot lifecycle — observer fires startSync on every uid != null
    // transition, so VMs no longer need to.

    private val _canvasNudgeDismissed = MutableStateFlow(nudgeStore.isCanvasNudgeDismissed())
    val canvasNudgeDismissed: StateFlow<Boolean> = _canvasNudgeDismissed.asStateFlow()

    fun dismissCanvasNudge() {
        nudgeStore.dismissCanvasNudge()
        _canvasNudgeDismissed.value = true
    }

    /**
     * When true, hides "noisy" non-bookmarked campus iCal events so Home shows
     * only what the user has flagged worth attention. Canvas assignments,
     * user-created events, and shared invites always show in both states —
     * they're never campus noise. Default off (show everything).
     */
    private val _importantOnly = MutableStateFlow(true)
    val importantOnly: StateFlow<Boolean> = _importantOnly.asStateFlow()

    /**
     * Events for Home, optionally filtered to "important" — drops non-bookmarked
     * iCal events. Bookmarked iCal stays; Canvas / custom / shared always show.
     */
    val events: StateFlow<List<CalendarEvent>> = combine(
        repository.observeEvents(),
        _importantOnly,
    ) { allEvents, importantOnly ->
        if (!importantOnly) allEvents else allEvents.filter { event ->
            event.source != EventSource.ICAL_FEED || event.isBookmarked
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleImportantOnly() {
        _importantOnly.value = !_importantOnly.value
    }

    fun toggleBookmark(eventId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(eventId)
        }
    }

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
                locationLabel = "CSUCI Campus",
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
                json.decodeFromString<OpenMeteoResponse>(body)
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
