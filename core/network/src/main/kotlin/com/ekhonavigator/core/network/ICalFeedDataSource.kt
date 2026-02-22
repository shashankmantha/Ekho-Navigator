package com.ekhonavigator.core.network

import android.util.Log
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Categories
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Url
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ICalFeedDataSource"

/**
 * Fetches and parses .ics (iCalendar) feeds from 25Live Publisher.
 *
 * The feed URL is expected to be a publicly accessible 25Live Publisher URL like:
 * https://25livepub.collegenet.com/calendars/{calendar-name}.ics
 */
@Singleton
class ICalFeedDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    /**
     * Fetches the .ics feed at [feedUrl] and parses all VEVENT components
     * into [NetworkCalendarEvent] objects. Runs on [Dispatchers.IO].
     *
     * @throws ICalFetchException on network or parsing failures.
     */
    suspend fun fetchEvents(feedUrl: String): List<NetworkCalendarEvent> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Fetching feed from: $feedUrl")
            val request = Request.Builder().url(feedUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Fetch failed: HTTP ${response.code}")
                throw ICalFetchException("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body.string()
            Log.d(TAG, "Fetched ${body.length} characters of iCal data")

            val events = parseICalFeed(body)
            Log.d(TAG, "Parsed ${events.size} events from feed")
            events
        }

    /**
     * iCal4j 4.x tries to register a custom ZoneRulesProvider when parsing
     * VTIMEZONE components. Android blocks this API (hidden platform method).
     * Stripping VTIMEZONE blocks avoids the crash. Date parsing still works
     * because temporalToInstant() handles all Temporal types, and Android's
     * built-in timezone database knows "America/Los_Angeles" natively.
     */
    private val vtimezonePattern = Regex(
        "BEGIN:VTIMEZONE.*?END:VTIMEZONE\\r?\\n",
        RegexOption.DOT_MATCHES_ALL,
    )

    private fun parseICalFeed(icsContent: String): List<NetworkCalendarEvent> {
        return try {
            val cleanedContent = vtimezonePattern.replace(icsContent, "")
            val builder = CalendarBuilder()
            val calendar = builder.build(StringReader(cleanedContent))

            calendar.getComponents<VEvent>(Component.VEVENT).mapNotNull { event ->
                try {
                    parseEvent(event).also { parsed ->
                        if (parsed == null) Log.w(TAG, "parseEvent returned null for event")
                    }
                } catch (e: Exception) {
                    val eventUid = event.getProperty<Uid>(Property.UID).orElse(null)?.value
                    Log.w(TAG, "Failed to parse event uid=$eventUid: ${e.message}")
                    null // Skip malformed events rather than failing the whole sync
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse iCal feed content", e)
            emptyList()
        }
    }

    /**
     * iCal4j 4.x returns Optional<T> from property accessors.
     */
    private fun parseEvent(event: VEvent): NetworkCalendarEvent? {
        val uid = event.getProperty<Uid>(Property.UID).orElse(null)?.value ?: return null
        val summary = event.getProperty<Summary>(Property.SUMMARY).orElse(null)?.value.orEmpty()
        val description = event.getProperty<Description>(Property.DESCRIPTION).orElse(null)?.value.orEmpty()
        val location = event.getProperty<Location>(Property.LOCATION).orElse(null)?.value.orEmpty()

        val dtStart = event.getProperty<DtStart<*>>(Property.DTSTART).orElse(null)?.date
            ?.let { temporalToInstant(it) } ?: return null
        val dtEnd = event.getProperty<DtEnd<*>>(Property.DTEND).orElse(null)?.date
            ?.let { temporalToInstant(it) } ?: dtStart

        val categories = event.getProperties<Categories>(Property.CATEGORIES)
            .flatMap { prop ->
                prop.value.split(",").map { categoryName ->
                    EventCategory.fromICalCategory(categoryName)
                }
            }
            .ifEmpty { listOf(EventCategory.GENERAL) }

        val url = event.getProperty<Url>(Property.URL).orElse(null)?.value.orEmpty()
        val status = event.getProperty<Status>(Property.STATUS).orElse(null)?.value ?: "CONFIRMED"

        return NetworkCalendarEvent(
            uid = uid,
            summary = summary,
            description = description,
            location = location,
            dtStart = dtStart,
            dtEnd = dtEnd,
            categories = categories,
            url = url,
            status = status,
        )
    }
    /**
     * iCal4j can return different Temporal types depending on how the .ics
     * encodes dates:
     *   - ZonedDateTime → DTSTART;TZID=America/Los_Angeles:20260105T110000
     *   - Instant       → DTSTART:20260105T190000Z (UTC suffix)
     *   - LocalDateTime  → DTSTART:20260105T110000 (no timezone, floating)
     *   - LocalDate      → DTSTART;VALUE=DATE:20260105 (all-day event)
     *
     * This safely converts any of them to Instant.
     */
    private fun temporalToInstant(temporal: Temporal): Instant = when (temporal) {
        is Instant -> temporal
        is ZonedDateTime -> temporal.toInstant()
        is LocalDateTime -> temporal.atZone(ZoneId.systemDefault()).toInstant()
        is LocalDate -> temporal.atStartOfDay(ZoneId.systemDefault()).toInstant()
        else -> Instant.from(temporal) // fallback, may throw
    }
}

class ICalFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)
