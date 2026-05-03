package com.ekhonavigator.feature.canvas.settings

import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.auth.CanvasAuthError
import com.ekhonavigator.core.canvas.auth.CanvasAuthValidator
import com.ekhonavigator.core.canvas.auth.CanvasIdentitySource
import com.ekhonavigator.core.canvas.auth.CanvasInstitutionStore
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.model.CanvasProfile
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

internal class FakeCanvasIdentitySource(var uid: String? = null) : CanvasIdentitySource {
    override fun currentUid(): String? = uid
}

internal class FakeCanvasAccountSource(var account: CanvasAccount? = null) : CanvasAccountSource {
    override fun currentOrNull(): CanvasAccount? = account
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

internal class FakeCanvasCourseRepository : CanvasCourseRepository {
    var syncCalls = 0
    var clearAllCalls = 0
    override fun observeCourses(): Flow<List<CanvasCourse>> = flowOf(emptyList())
    override suspend fun sync(): Result<Unit> {
        syncCalls++
        return Result.success(Unit)
    }
    override suspend fun clearAll() { clearAllCalls++ }
}

internal class FakeCanvasPlannerRepository : CanvasPlannerRepository {
    val syncCalls = mutableListOf<Pair<Instant, Instant>>()
    var clearAllCalls = 0
    override fun observeItems(start: Instant, end: Instant): Flow<List<PlannerItem>> = flowOf(emptyList())
    override fun observeAllItems(): Flow<List<PlannerItem>> = flowOf(emptyList())
    override suspend fun sync(start: Instant, end: Instant): Result<Unit> {
        syncCalls += start to end
        return Result.success(Unit)
    }
    override suspend fun clearAll() { clearAllCalls++ }
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
