package com.ekhonavigator.core.data.di

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.auth.FirebaseAuthRepository
import com.ekhonavigator.core.data.profile.FirestoreProfileRepository
import com.ekhonavigator.core.data.profile.ProfileRepository
import com.ekhonavigator.core.data.repository.DefaultPresenceRepository
import com.ekhonavigator.core.data.repository.PresenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPresenceRepository(
        defaultPresenceRepository: DefaultPresenceRepository
    ): PresenceRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        firestoreProfileRepository: FirestoreProfileRepository
    ): ProfileRepository
}
