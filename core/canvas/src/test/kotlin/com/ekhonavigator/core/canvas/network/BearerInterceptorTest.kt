package com.ekhonavigator.core.canvas.network

import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.canvas.auth.FakeCanvasAccountSource
import com.ekhonavigator.core.canvas.auth.FakeCanvasTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BearerInterceptorTest {

    private val server = MockWebServer()
    private val account = CanvasAccount(firebaseUid = "uid-1", domain = "csuci.instructure.com")

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `no signed-in account omits Authorization header`() {
        executeWith(accountSource = FakeCanvasAccountSource(account = null), tokens = FakeCanvasTokenStore())

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `signed-in account without stored token omits Authorization header`() {
        executeWith(accountSource = FakeCanvasAccountSource(account = account), tokens = FakeCanvasTokenStore())

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `signed-in account with stored token attaches Bearer header`() {
        val tokens = FakeCanvasTokenStore().apply { put(account, "secret-pat") }

        executeWith(accountSource = FakeCanvasAccountSource(account = account), tokens = tokens)

        assertEquals("Bearer secret-pat", server.takeRequest().getHeader("Authorization"))
    }

    private fun executeWith(accountSource: FakeCanvasAccountSource, tokens: FakeCanvasTokenStore) {
        server.enqueue(MockResponse())
        OkHttpClient.Builder()
            .addInterceptor(BearerInterceptor(accountSource, tokens))
            .build()
            .newCall(Request.Builder().url(server.url("/")).build())
            .execute()
            .close()
    }
}
