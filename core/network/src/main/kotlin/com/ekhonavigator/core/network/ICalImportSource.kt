package com.ekhonavigator.core.network

import com.ekhonavigator.core.network.model.StagedImportedEvent
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.WeekDay
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import java.io.InputStream
import java.io.InputStreamReader
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses a user-supplied .ics file into [StagedImportedEvent]s. Mirrors the
 * VTIMEZONE strip + parseEvent shape of [ICalFeedDataSource] — Android's iCal4j
 * can't resolve Olson VTIMEZONE chunks and crashes if they're left in.
 *
 * Only `FREQ=WEEKLY` recurrences are surfaced — that covers class schedules and
 * weekly meetings; daily/monthly/yearly rules get dropped to one-off events so
 * we don't accidentally explode a yearly birthday into 50 calendar rows.
 */
@Singleton
class ICalImportSource @Inject constructor() {

    fun parse(stream: InputStream): List<StagedImportedEvent> = try {
        val raw = InputStreamReader(stream).use { it.readText() }
        val cleaned = vtimezonePattern.replace(raw, "")
        val calendar = CalendarBuilder().build(cleaned.reader())
        calendar.getComponents<VEvent>(Component.VEVENT)
            .mapNotNull { runCatching { parseEvent(it) }.getOrNull() }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseEvent(event: VEvent): StagedImportedEvent? {
    val uid = event.getProperty<Uid>(Property.UID).orElse(null)?.value
        ?: return null
    val summary = event.getProperty<Summary>(Property.SUMMARY).orElse(null)?.value.orEmpty()
    val description = event.getProperty<Description>(Property.DESCRIPTION).orElse(null)?.value.orEmpty()
    val location = event.getProperty<Location>(Property.LOCATION).orElse(null)?.value.orEmpty()

    val dtStart = event.getProperty<DtStart<*>>(Property.DTSTART).orElse(null)?.date
        ?.let { temporalToInstant(it) } ?: return null
    val dtEnd = event.getProperty<DtEnd<*>>(Property.DTEND).orElse(null)?.date
        ?.let { temporalToInstant(it) } ?: dtStart

    val recurrence = event.getProperty<RRule<*>>(Property.RRULE).orElse(null)
        ?.let { parseWeeklyRecurrence(it, dtStart) }

    return StagedImportedEvent(
        sourceUid = uid,
        title = summary,
        description = description,
        location = location,
        startTime = dtStart,
        endTime = dtEnd,
        recurrenceDays = recurrence?.days.orEmpty(),
        recurrenceEndDate = recurrence?.endDate,
        // Surface a course code from the summary — CSUCI section names land here
        // ("COMP-262 Sec 001 - Operating Systems"), so a strict-prefix regex
        // catches them without false positives on regular event titles.
        inferredCourseLabel = courseLabelPattern.find(summary.trim())?.groupValues?.get(1),
    )
}

private data class WeeklyRecurrence(val days: Set<DayOfWeek>, val endDate: LocalDate)

private fun parseWeeklyRecurrence(rrule: RRule<*>, anchor: java.time.Instant): WeeklyRecurrence? {
    val recur = rrule.recur ?: return null
    val freq = recur.frequency?.name ?: return null
    if (freq != "WEEKLY") return null

    val zone = ZoneId.systemDefault()
    val anchorDate = anchor.atZone(zone).toLocalDate()

    val days: Set<DayOfWeek> = recur.dayList
        ?.mapNotNull { it.day?.let(::iCal4jDayToJava) }
        ?.toSet()
        .orEmpty()
        .ifEmpty { setOf(anchorDate.dayOfWeek) }

    val endDate: LocalDate = recur.until?.let { temporalToInstant(it).atZone(zone).toLocalDate() }
        ?: recur.count.takeIf { it > 0 }?.let { count ->
            // COUNT counts occurrences, not weeks — approximate by spanning
            // count-1 weeks from the anchor. Good enough for V1.
            anchorDate.plus(((count - 1) / days.size.coerceAtLeast(1)).toLong(), ChronoUnit.WEEKS)
        }
        ?: anchorDate.plusMonths(4)  // No UNTIL/COUNT: cap at a term-ish length so we don't run forever.

    return WeeklyRecurrence(days, endDate)
}

private fun iCal4jDayToJava(day: WeekDay.Day): DayOfWeek? = when (day) {
    WeekDay.Day.MO -> DayOfWeek.MONDAY
    WeekDay.Day.TU -> DayOfWeek.TUESDAY
    WeekDay.Day.WE -> DayOfWeek.WEDNESDAY
    WeekDay.Day.TH -> DayOfWeek.THURSDAY
    WeekDay.Day.FR -> DayOfWeek.FRIDAY
    WeekDay.Day.SA -> DayOfWeek.SATURDAY
    WeekDay.Day.SU -> DayOfWeek.SUNDAY
}

private val vtimezonePattern = Regex(
    "BEGIN:VTIMEZONE.*?END:VTIMEZONE\\r?\\n",
    RegexOption.DOT_MATCHES_ALL,
)

// Course-code shapes like "COMP-262", "MATH 350", "ASL101". Anchored at start
// so a regular event titled "262 Reasons to Love..." doesn't accidentally tag.
private val courseLabelPattern = Regex("^([A-Z]{2,5}-?\\s?\\d{2,4})\\b")

private fun temporalToInstant(temporal: java.time.temporal.Temporal): java.time.Instant = when (temporal) {
    is java.time.Instant -> temporal
    is java.time.ZonedDateTime -> temporal.toInstant()
    is java.time.OffsetDateTime -> temporal.toInstant()
    is java.time.LocalDateTime -> temporal.atZone(ZoneId.systemDefault()).toInstant()
    is LocalDate -> temporal.atStartOfDay(ZoneId.systemDefault()).toInstant()
    else -> java.time.Instant.from(temporal)
}

// Public helpers so the UI layer can show a single-line "X starts at Y" summary.
val StagedImportedEvent.durationMinutes: Long
    get() = Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0)
