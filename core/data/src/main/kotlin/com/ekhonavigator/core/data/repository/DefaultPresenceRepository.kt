package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.model.PresenceStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPresenceRepository @Inject constructor() : PresenceRepository {

    private val database = FirebaseDatabase.getInstance()
    
    // Track the active listener and UID to prevent duplicates
    private var activeUid: String? = null
    private var connectedListener: ValueEventListener? = null

    override fun startPresence(uid: String) {
        // Guard: don't restart if already running for the same user
        if (activeUid == uid) return

        // Clean up any existing listener before starting a new one
        stopPresence()

        activeUid = uid
        val userStatusRef = database.getReference("status").child(uid)
        val connectedRef = database.getReference(".info/connected")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    val offlineStatus = mapOf(
                        "state" to "offline",
                        "lastChanged" to ServerValue.TIMESTAMP
                    )

                    val onlineStatus = mapOf(
                        "state" to "online",
                        "lastChanged" to ServerValue.TIMESTAMP
                    )

                    // Set onDisconnect before marking online to avoid race conditions
                    userStatusRef.onDisconnect().setValue(offlineStatus)
                    userStatusRef.setValue(onlineStatus)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        connectedListener = listener
        connectedRef.addValueEventListener(listener)
    }

    override fun stopPresence() {
        connectedListener?.let {
            database.getReference(".info/connected").removeEventListener(it)
        }
        connectedListener = null
        activeUid = null
    }

    override suspend fun setOfflineNow(uid: String) {
        stopPresence()
        
        val userStatusRef = database.getReference("status").child(uid)
        val offlineStatus = mapOf(
            "state" to "offline",
            "lastChanged" to ServerValue.TIMESTAMP
        )
        userStatusRef.setValue(offlineStatus).await()
    }

    override fun observePresence(uid: String): Flow<PresenceStatus> = callbackFlow {
        val ref = database.getReference("status").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // ValueEventListener returns Long for TIMESTAMP, which matches PresenceStatus
                val status = snapshot.getValue(PresenceStatus::class.java) ?: PresenceStatus()
                trySend(status)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
