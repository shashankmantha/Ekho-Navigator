package com.ekhonavigator.core.canvas.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasAccountSource @Inject constructor(
    private val identitySource: CanvasIdentitySource,
    private val institutionStore: CanvasInstitutionStore,
) : CanvasAccountSource {

    override fun currentOrNull(): CanvasAccount? {
        val uid = identitySource.currentUid() ?: return null
        val domain = institutionStore.getDomain(uid) ?: return null
        return CanvasAccount(firebaseUid = uid, domain = domain)
    }
}
