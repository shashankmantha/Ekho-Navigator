package com.ekhonavigator.core.canvas.network

import okhttp3.Headers

/**
 * Extracts the URL marked `rel="next"` from an RFC 5988 Link header.
 *
 * Canvas's pagination uses opaque cursor URLs that must be followed verbatim
 * rather than constructed by the client (per Canvas API docs). Returns null when
 * there is no next page or the header is absent.
 *
 * Example header:
 * ```
 * Link: <https://csuci.instructure.com/api/v1/planner/items?page=bookmark%3AWyJ...>; rel="next",
 *       <https://csuci.instructure.com/api/v1/planner/items?page=first>; rel="first",
 *       <https://csuci.instructure.com/api/v1/planner/items?page=current>; rel="current"
 * ```
 */
fun nextPageUrl(headers: Headers): String? {
    val raw = headers["Link"] ?: headers["link"] ?: return null
    return raw.splitToSequence(',')
        .map { it.trim() }
        .firstOrNull { LINK_REL_NEXT.containsMatchIn(it) }
        ?.let { LINK_URL.find(it)?.groupValues?.getOrNull(1) }
}

private val LINK_REL_NEXT = Regex("""rel\s*=\s*"?next"?""")
private val LINK_URL = Regex("""<([^>]+)>""")
