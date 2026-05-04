package com.ekhonavigator.core.canvas.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : CanvasTokenStore {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // replay = 1 so a late collector picks up the current state without a mutation;
    // DROP_OLDEST keeps the stream non-suspending for synchronous put/delete callers.
    private val mutations = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    override fun get(account: CanvasAccount): String? =
        prefs.getString(account.toKey(), null)

    override fun put(account: CanvasAccount, token: String) {
        prefs.edit().putString(account.toKey(), token).apply()
        mutations.tryEmit(Unit)
    }

    override fun delete(account: CanvasAccount) {
        prefs.edit().remove(account.toKey()).apply()
        mutations.tryEmit(Unit)
    }

    override fun deleteAll() {
        prefs.edit().clear().apply()
        mutations.tryEmit(Unit)
    }

    override fun changes(): Flow<Unit> = mutations.asSharedFlow()

    private fun CanvasAccount.toKey(): String = "pat__${firebaseUid}__$domain"

    companion object {
        // Filename also referenced by backup_rules.xml and data_extraction_rules.xml — keep in sync.
        private const val FILE_NAME = "canvas_credentials"
    }
}
