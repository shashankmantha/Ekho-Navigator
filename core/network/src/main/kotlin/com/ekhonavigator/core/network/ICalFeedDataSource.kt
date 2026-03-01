package com.ekhonavigator.core.network

import android.text.Html
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
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
            val request = Request.Builder().url(feedUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw ICalFetchException("HTTP ${response.code}: ${response.message}")
            }

            parseICalFeed(response.body.string())
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
                    parseEvent(event)
                } catch (_: Exception) {
                    null // Skip malformed events rather than failing the whole sync
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Decodes HTML entities (e.g. `&#39;` → `'`, `&amp;` → `&`) from plain-text
     * fields. The 25Live Publisher feed embeds HTML entities in SUMMARY, LOCATION, etc.
     * Uses Android's built-in Html parser which handles all standard HTML entities.
     */
    private fun decodeHtmlEntities(text: String): String =
        Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT).toString().trim()

    /**
     * iCal4j 4.x returns Optional<T> from property accessors.
     */
    private fun parseEvent(event: VEvent): NetworkCalendarEvent? {
        val uid = event.getProperty<Uid>(Property.UID).orElse(null)?.value ?: return null
        val summary = event.getProperty<Summary>(Property.SUMMARY).orElse(null)?.value.orEmpty()
            .let { decodeHtmlEntities(it) }
        val description =
            event.getProperty<Description>(Property.DESCRIPTION).orElse(null)?.value.orEmpty()
        val location = event.getProperty<Location>(Property.LOCATION).orElse(null)?.value.orEmpty()
            .let { decodeHtmlEntities(it) }

        val dtStart = event.getProperty<DtStart<*>>(Property.DTSTART).orElse(null)?.date
            ?.let { temporalToInstant(it) } ?: return null
        val dtEnd = event.getProperty<DtEnd<*>>(Property.DTEND).orElse(null)?.date
            ?.let { temporalToInstant(it) } ?: dtStart

        // The standard CATEGORIES property just says "CSUCI Events Calendar 25Live"
        // for every event (useless). The real categories live in Trumba custom fields:
        //   X-TRUMBA-CUSTOMFIELD;NAME="Categories";ID=23227;TYPE=Enumeration:Staff
        // Multi-category events use escaped commas in the raw .ics (\,) which iCal4j
        // unescapes to regular commas, e.g. "Student Organizations, University Life, Alumni"
        val categories = event.getProperties<net.fortuna.ical4j.model.property.XProperty>(
            "X-TRUMBA-CUSTOMFIELD",
        ).filter { prop ->
            prop.getParameter<net.fortuna.ical4j.model.Parameter>("NAME")
                ?.orElse(null)?.value == "Categories"
        }.flatMap { prop ->
            // Values may contain HTML entities (e.g. "Academics &amp; Research")
            // so decode before matching against our enum.
            prop.value.split(",").map { decodeHtmlEntities(it) }
                .map { EventCategory.fromTrumbaCategory(it) }
        }.distinct()
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
