package com.ekhonavigator.feature.home

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-device dismissal state for Home onboarding nudges. Device-wide (not
 * uid-keyed) on purpose — once a user dismisses a nudge, signing out and
 * back in shouldn't re-pester them. New nudges (e.g. notifications, profile
 * complete) add another key here rather than another store.
 */
@Singleton
class HomeNudgeStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun isCanvasNudgeDismissed(): Boolean =
        prefs.getBoolean(KEY_CANVAS_NUDGE_DISMISSED, false)

    fun dismissCanvasNudge() {
        prefs.edit { putBoolean(KEY_CANVAS_NUDGE_DISMISSED, true) }
    }

    companion object {
        private const val FILE_NAME = "home_nudge_prefs"
        private const val KEY_CANVAS_NUDGE_DISMISSED = "canvas_nudge_dismissed"
    }
}
