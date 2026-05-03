package com.ekhonavigator.core.canvas.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CanvasHeadersInterceptorTest {

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `every request carries canvas Accept and identifying User-Agent`() {
        server.enqueue(MockResponse())
        OkHttpClient.Builder()
            .addInterceptor(CanvasHeadersInterceptor())
            .build()
            .newCall(Request.Builder().url(server.url("/")).build())
            .execute()
            .close()

        val recorded = server.takeRequest()
        assertEquals("application/json+canvas-string-ids", recorded.getHeader("Accept"))
        assertEquals("EkhoNavigator/0.x (csuci-pilot)", recorded.getHeader("User-Agent"))
    }
}
