package com.ekhonavigator.core.canvas.network

import okhttp3.Headers

// Canvas pagination cursors are opaque — follow verbatim, never construct.
fun nextPageUrl(headers: Headers): String? {
    val raw = headers["Link"] ?: headers["link"] ?: return null
    return raw.splitToSequence(',')
        .map { it.trim() }
        .firstOrNull { LINK_REL_NEXT.containsMatchIn(it) }
        ?.let { LINK_URL.find(it)?.groupValues?.getOrNull(1) }
}

private val LINK_REL_NEXT = Regex("""rel\s*=\s*"?next"?""")
private val LINK_URL = Regex("""<([^>]+)>""")
