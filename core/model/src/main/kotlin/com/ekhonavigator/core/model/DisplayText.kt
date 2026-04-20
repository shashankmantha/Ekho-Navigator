package com.ekhonavigator.core.model

private val SHORT_FILLER_WORDS = setOf(
    "a", "an", "the",
    "and", "or", "but",
    "of", "in", "on", "at", "to", "by", "for",
)

/**
 * Prettifies all-uppercase display strings to standard English titlecase:
 * because ICAL events have inconsistent case/style
 */
fun String.prettifyAllCaps(): String {
    if (isBlank() || any { it.isLowerCase() }) return this

    val words = split(' ')
    return words.mapIndexed { index, word ->
        val isEdge = index == 0 || index == words.lastIndex
        when {
            word.isEmpty() -> word
            word.lowercase() in SHORT_FILLER_WORDS ->
                if (isEdge) word.titlecase() else word.lowercase()
            !word.all { it.isLetter() } -> word
            else -> word.titlecase()
        }
    }.joinToString(" ")
}

private fun String.titlecase(): String =
    lowercase().replaceFirstChar { it.uppercase() }
