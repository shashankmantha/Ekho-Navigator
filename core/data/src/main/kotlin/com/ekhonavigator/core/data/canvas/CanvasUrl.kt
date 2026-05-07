package com.ekhonavigator.core.data.canvas

/**
 * Canvas REST endpoints return `html_url` (and similar) as relative paths
 * — e.g. `/courses/123/assignments/456`. Those parse into a [android.net.Uri]
 * but `startActivity` rejects them because there's no `http(s)` scheme bound
 * to a browser, crashing with `ActivityNotFoundException`.
 *
 * Resolves a possibly-relative Canvas URL against the user's institution
 * domain so the stored value is always launchable. Idempotent: an already-
 * absolute URL is returned unchanged.
 */
internal fun absolutizeCanvasUrl(url: String, domain: String): String {
    if (url.isBlank()) return url
    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
        return url
    }
    val host = domain.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
    val path = if (url.startsWith("/")) url else "/$url"
    return "https://$host$path"
}
