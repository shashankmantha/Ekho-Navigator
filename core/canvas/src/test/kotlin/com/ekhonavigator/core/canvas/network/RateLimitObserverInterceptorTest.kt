package com.ekhonavigator.core.canvas.network

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RateLimitObserverInterceptorTest {

    private val server = MockWebServer()
    private val interceptor = RateLimitObserverInterceptor()
    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `emits sample with parsed cost remaining path and status`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("X-Request-Cost", "12.5")
                .setHeader("X-Rate-Limit-Remaining", "687.5")
                .setResponseCode(200),
        )

        interceptor.samples.test {
            client.newCall(Request.Builder().url(server.url("/api/v1/users/self/profile")).build())
                .execute()
                .close()

            val sample = awaitItem()
            assertEquals(12.5, sample.requestCost!!, 0.0)
            assertEquals(687.5, sample.remaining!!, 0.0)
            assertEquals("/api/v1/users/self/profile", sample.urlPath)
            assertEquals(200, sample.statusCode)
        }
    }

    @Test
    fun `emits sample with null cost and remaining when headers absent`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        interceptor.samples.test {
            client.newCall(Request.Builder().url(server.url("/x")).build())
                .execute()
                .close()

            val sample = awaitItem()
            assertNull(sample.requestCost)
            assertNull(sample.remaining)
            assertEquals(200, sample.statusCode)
        }
    }
}
