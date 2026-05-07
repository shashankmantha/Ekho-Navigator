package com.ekhonavigator.core.canvas.network

import kotlinx.serialization.json.Json

/**
 * Canvas adds fields without API-version bumps and student-PAT responses
 * silently omit teacher-only fields. Tolerate both rather than failing the parse.
 */
internal val canvasJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
