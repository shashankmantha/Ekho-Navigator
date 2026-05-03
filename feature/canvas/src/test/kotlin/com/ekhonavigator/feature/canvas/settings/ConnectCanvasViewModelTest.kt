package com.ekhonavigator.feature.canvas.settings

import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConnectCanvasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val identity = FakeCanvasIdentitySource()
    private val institutions = FakeCanvasInstitutionStore()
    private val tokens = FakeCanvasTokenStore()
    private val validator = FakeCanvasAuthValidator()
    private val courseRepo = FakeCanvasCourseRepository()
    private val plannerRepo = FakeCanvasPlannerRepository()

    @Test
    fun `signed-out user starts in NotConnected with default domain`() {
        identity.uid = null

        val state = newViewModel().uiState.value as ConnectCanvasUiState.NotConnected

        assertEquals(DEFAULT_CANVAS_DOMAIN, state.domain)
    }

    @Test
    fun `signed-in user with stored institution and PAT starts in Connected`() {
        identity.uid = "uid-1"
        institutions.setDomain("uid-1", "csuci.instructure.com")
        tokens.put(CanvasAccount("uid-1", "csuci.instructure.com"), "stored-pat")

        val state = newViewModel().uiState.value as ConnectCanvasUiState.Connected

        assertEquals("csuci.instructure.com", state.domain)
    }

    @Test
    fun `signed-in user with stored institution but no PAT falls back to NotConnected`() {
        identity.uid = "uid-1"
        institutions.setDomain("uid-1", "csuci.instructure.com")

        val state = newViewModel().uiState.value

        assertTrue(state is ConnectCanvasUiState.NotConnected)
        // Stale institution gets cleared by the guard so the next reconnect starts fresh.
        assertNull(institutions.getDomain("uid-1"))
    }

    @Test
    fun `signed-in user without stored institution starts in NotConnected`() {
        identity.uid = "uid-1"

        val state = newViewModel().uiState.value

        assertTrue(state is ConnectCanvasUiState.NotConnected)
    }

    @Test
    fun `connect with success persists token and institution then transitions to Connected`() = runTest {
        identity.uid = "uid-1"
        val viewModel = newViewModel()
        viewModel.setToken("good-pat")

        viewModel.connect()

        val state = viewModel.uiState.value as ConnectCanvasUiState.Connected
        assertEquals(DEFAULT_CANVAS_DOMAIN, state.domain)
        assertEquals("csuci.instructure.com" to "good-pat", validator.calls.single())
        assertEquals("good-pat", tokens.get(CanvasAccount("uid-1", DEFAULT_CANVAS_DOMAIN)))
        assertEquals(DEFAULT_CANVAS_DOMAIN, institutions.getDomain("uid-1"))
        assertEquals(1, courseRepo.syncCalls)
        assertEquals(1, plannerRepo.syncCalls.size)
    }

    @Test
    fun `connect with rejected token surfaces error and leaves storage untouched`() = runTest {
        identity.uid = "uid-1"
        validator.returnInvalidToken()
        val viewModel = newViewModel()
        viewModel.setToken("bad-pat")

        viewModel.connect()

        val state = viewModel.uiState.value as ConnectCanvasUiState.NotConnected
        assertNotNull(state.error)
        assertNull(tokens.get(CanvasAccount("uid-1", DEFAULT_CANVAS_DOMAIN)))
        assertNull(institutions.getDomain("uid-1"))
    }

    @Test
    fun `connect refuses when no firebase user is signed in`() = runTest {
        identity.uid = null
        val viewModel = newViewModel()
        viewModel.setToken("any-pat")

        viewModel.connect()

        val state = viewModel.uiState.value as ConnectCanvasUiState.NotConnected
        assertNotNull(state.error)
        assertTrue(validator.calls.isEmpty())
    }

    @Test
    fun `disconnect clears token, institution, and Canvas caches then transitions to NotConnected`() = runTest {
        identity.uid = "uid-1"
        institutions.setDomain("uid-1", "csuci.instructure.com")
        tokens.put(CanvasAccount("uid-1", "csuci.instructure.com"), "old-pat")

        val viewModel = newViewModel()
        viewModel.disconnect()
        // disconnect() launches the cache wipe in viewModelScope — drain it.
        kotlinx.coroutines.test.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ConnectCanvasUiState.NotConnected)
        assertNull(tokens.get(CanvasAccount("uid-1", "csuci.instructure.com")))
        assertNull(institutions.getDomain("uid-1"))
        assertEquals(1, courseRepo.clearAllCalls)
        assertEquals(1, plannerRepo.clearAllCalls)
    }

    private fun newViewModel(): ConnectCanvasViewModel = ConnectCanvasViewModel(
        identitySource = identity,
        institutionStore = institutions,
        tokenStore = tokens,
        validator = validator,
        canvasCourseRepository = courseRepo,
        canvasPlannerRepository = plannerRepo,
    )
}
