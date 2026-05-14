package com.ekhonavigator.core.data.canvas

// Canvas returns relative html_urls that crash startActivity if launched as-is.
// Idempotent — already-absolute URLs return unchanged.
internal fun absolutizeCanvasUrl(url: String, domain: String): String {
    if (url.isBlank()) return url
    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
        return url
    }
    val host = domain.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
    val path = if (url.startsWith("/")) url else "/$url"
    return "https://$host$path"
}
