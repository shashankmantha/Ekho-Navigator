package com.ekhonavigator.core.network

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

            val body = response.body.string()

            parseICalFeed(body)
        }

    private fun parseICalFeed(icsContent: String): List<NetworkCalendarEvent> {
        val builder = CalendarBuilder()
        val calendar = builder.build(StringReader(icsContent))

        return calendar.getComponents<VEvent>(Component.VEVENT).mapNotNull { event ->
            try {
                parseEvent(event)
            } catch (_: Exception) {
                null // Skip malformed events rather than failing the whole sync
            }
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

        val dtStart = event.getProperty<DtStart<*>>(Property.DTSTART).orElse(null)?.date?.let { Instant.from(it) } ?: return null
        val dtEnd = event.getProperty<DtEnd<*>>(Property.DTEND).orElse(null)?.date?.let { Instant.from(it) } ?: dtStart

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
}

class ICalFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)
