package com.ekhonavigator.core.canvas.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain is non-sensitive metadata, so this uses regular SharedPreferences (not encrypted)
 * and is intentionally NOT excluded from backup — surviving restore lets a returning user
 * be re-prompted only for their PAT, not the institution.
 */
@Singleton
internal class DefaultCanvasInstitutionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : CanvasInstitutionStore {

    private val prefs by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    override fun getDomain(uid: String): String? =
        prefs.getString(key(uid), null)

    override fun setDomain(uid: String, domain: String) {
        prefs.edit { putString(key(uid), domain) }
    }

    override fun clearDomain(uid: String) {
        prefs.edit { remove(key(uid)) }
    }

    override fun clearAll() {
        prefs.edit { clear() }
    }

    private fun key(uid: String): String = "domain__$uid"

    companion object {
        private const val FILE_NAME = "canvas_institutions"
    }
}
