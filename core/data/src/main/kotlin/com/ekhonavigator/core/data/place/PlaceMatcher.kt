package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.model.Place
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceMatcher @Inject constructor() {

    fun resolve(raw: String, places: List<Place>): String? {
        val normalizedRaw = normalize(raw)
        if (normalizedRaw.isEmpty()) return null

        return places.firstOrNull { place ->
            place.candidates().any { candidate ->
                val normalizedCandidate = normalize(candidate)
                normalizedCandidate.isNotEmpty() && normalizedRaw.containsWhole(normalizedCandidate)
            }
        }?.id
    }

    private fun Place.candidates(): Sequence<String> =
        sequenceOf(name) + aliases.asSequence()

    // Word-boundary contains: plain substring would match "Sierrawood" against "Sierra".
    private fun String.containsWhole(candidate: String): Boolean {
        if (this == candidate) return true
        if (!contains(candidate)) return false
        val regex = Regex("(?:^|\\s)${Regex.escape(candidate)}(?:\\s|$)")
        return regex.containsMatchIn(this)
    }

    private fun normalize(s: String): String = buildString {
        var lastWasSpace = true
        for (c in s) {
            when {
                c.isLetterOrDigit() -> {
                    append(c.lowercaseChar())
                    lastWasSpace = false
                }
                !lastWasSpace -> {
                    append(' ')
                    lastWasSpace = true
                }
            }
        }
    }.trim()
}
