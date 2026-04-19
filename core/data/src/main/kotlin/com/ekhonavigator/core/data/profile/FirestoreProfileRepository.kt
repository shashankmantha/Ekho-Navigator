package com.ekhonavigator.core.data.profile

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProfileRepository @Inject constructor() : ProfileRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun getProfile(uid: String): UserProfile? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.toObject<UserProfile>()
    }

    override suspend fun saveProfile(uid: String, profile: UserProfile) {
        val normalizedProfile = profile.copy(
            displayNameLower = profile.displayName.trim().lowercase(),
            emailLower = profile.email.trim().lowercase(),
            majorLower = profile.major.trim().lowercase(),
        )

        firestore.collection("users")
            .document(uid)
            .set(normalizedProfile, SetOptions.merge())
            .await()
    }
}
