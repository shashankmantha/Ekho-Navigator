package com.ekhonavigator.core.canvas.auth

import com.ekhonavigator.core.canvas.network.canvasJson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultCanvasAuthValidatorTest {

    private val server = MockWebServer()
    private val validator = DefaultCanvasAuthValidator(
        okHttpClient = OkHttpClient(),
        json = canvasJson,
    )

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `success returns parsed profile`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"123","name":"Nic V","short_name":"Nic","primary_email":"nic@csuci.edu"}""",
            ),
        )

        val result = validator.validate(domain = serverDomain(), token = "good-pat")

        val profile = result.getOrThrow()
        assertEquals("123", profile.id)
        assertEquals("Nic V", profile.name)
        assertEquals("Nic", profile.shortName)
        assertEquals("nic@csuci.edu", profile.primaryEmail)
        val recorded = server.takeRequest()
        assertEquals("Bearer good-pat", recorded.getHeader("Authorization"))
        assertEquals("/api/v1/users/self/profile", recorded.path)
    }

    @Test
    fun `401 maps to InvalidToken`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = validator.validate(domain = serverDomain(), token = "bad-pat")

        assertTrue(result.exceptionOrNull() is CanvasAuthError.InvalidToken)
    }

    @Test
    fun `non-2xx maps to HttpError with code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = validator.validate(domain = serverDomain(), token = "any-pat")

        val error = result.exceptionOrNull() as CanvasAuthError.HttpError
        assertEquals(503, error.code)
    }

    @Test
    fun `malformed json maps to ParseError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        val result = validator.validate(domain = serverDomain(), token = "any-pat")

        assertTrue(result.exceptionOrNull() is CanvasAuthError.ParseError)
    }

    @Test
    fun `unparseable domain maps to InvalidDomain`() = runTest {
        val result = validator.validate(domain = "not a host", token = "any-pat")

        assertTrue(result.exceptionOrNull() is CanvasAuthError.InvalidDomain)
    }

    /** MockWebServer URL with the scheme — what the validator builds the URL against. */
    private fun serverDomain(): String = server.url("").toString().removeSuffix("/")
}
