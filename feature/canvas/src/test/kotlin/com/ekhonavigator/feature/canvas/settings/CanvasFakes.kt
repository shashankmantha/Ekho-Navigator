package com.ekhonavigator.feature.canvas.settings

import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.canvas.auth.CanvasAuthError
import com.ekhonavigator.core.canvas.auth.CanvasAuthValidator
import com.ekhonavigator.core.canvas.auth.CanvasIdentitySource
import com.ekhonavigator.core.canvas.auth.CanvasInstitutionStore
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import com.ekhonavigator.core.canvas.model.CanvasProfile

internal class FakeCanvasIdentitySource(var uid: String? = null) : CanvasIdentitySource {
    override fun currentUid(): String? = uid
}

internal class FakeCanvasInstitutionStore : CanvasInstitutionStore {
    private val domains = mutableMapOf<String, String>()
    override fun getDomain(uid: String): String? = domains[uid]
    override fun setDomain(uid: String, domain: String) { domains[uid] = domain }
    override fun clearDomain(uid: String) { domains.remove(uid) }
    override fun clearAll() { domains.clear() }
}

internal class FakeCanvasTokenStore : CanvasTokenStore {
    private val tokens = mutableMapOf<CanvasAccount, String>()
    override fun get(account: CanvasAccount): String? = tokens[account]
    override fun put(account: CanvasAccount, token: String) { tokens[account] = token }
    override fun delete(account: CanvasAccount) { tokens.remove(account) }
    override fun deleteAll() { tokens.clear() }
}

internal class FakeCanvasAuthValidator : CanvasAuthValidator {
    var nextResult: Result<CanvasProfile> = Result.success(
        CanvasProfile(id = "1", name = "Test User", shortName = null, primaryEmail = null, avatarUrl = null),
    )
    val calls = mutableListOf<Pair<String, String>>()

    override suspend fun validate(domain: String, token: String): Result<CanvasProfile> {
        calls += domain to token
        return nextResult
    }

    fun returnInvalidToken() {
        nextResult = Result.failure(CanvasAuthError.InvalidToken)
    }
}
