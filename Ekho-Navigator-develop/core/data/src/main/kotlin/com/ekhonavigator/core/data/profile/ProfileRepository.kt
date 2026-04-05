package com.ekhonavigator.core.data.profile

interface ProfileRepository {
    suspend fun getProfile(uid: String): UserProfile?
    suspend fun saveProfile(uid: String, profile: UserProfile)
}