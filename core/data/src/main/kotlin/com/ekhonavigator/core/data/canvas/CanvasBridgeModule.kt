package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.auth.CanvasIdentitySource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CanvasBridgeModule {

    @Binds
    internal abstract fun bindCanvasIdentitySource(impl: FirebaseCanvasIdentitySource): CanvasIdentitySource

    @Binds
    internal abstract fun bindCanvasCourseRepository(
        impl: DefaultCanvasCourseRepository,
    ): CanvasCourseRepository
}
