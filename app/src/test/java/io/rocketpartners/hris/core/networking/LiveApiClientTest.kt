package io.rocketpartners.hris.core.networking

import io.rocketpartners.hris.core.auth.TokenProviding
import io.rocketpartners.hris.model.LeaveBalance
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LiveApiClientTest {

    private lateinit var server: MockWebServer

    /** Records refresh calls and the token handed to requests. */
    private class StubTokenProvider(
        private var token: String? = "t0",
        val refreshCount: AtomicInteger = AtomicInteger(0),
        private val refreshSucceeds: Boolean = true,
    ) : TokenProviding {
        override suspend fun accessToken(): String? = token
        override suspend fun refresh() {
            refreshCount.incrementAndGet()
            if (!refreshSucceeds) throw ApiError.Unauthorized
            token = "t1"
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(provider: TokenProviding? = StubTokenProvider()) =
        LiveApiClient(server.url("/api/v1").toString(), OkHttpClient(), provider)

    @Test
    fun send_decodesDataFromStatusSuccessEnvelope() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"status":"success","message":"ok","data":[{"id":1,"leaveTypeName":"Vacation","remainingDays":5.0}]}""",
            ),
        )
        val result: List<LeaveBalance> =
            client().send(Endpoint("leave-applications/balances/my"), ListSerializer(LeaveBalance.serializer()))
        assertEquals(1, result.size)
        assertEquals("Vacation", result.first().leaveTypeName)
    }

    @Test
    fun send_acceptsLegacyBooleanSuccessEnvelope() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"data":{"id":9,"remainingDays":3.0}}"""))
        val result: LeaveBalance = client().send(Endpoint("x"), LeaveBalance.serializer())
        assertEquals(9, result.id)
    }

    @Test
    fun send_401ThenSuccessfulRefreshRetries() = runTest {
        val provider = StubTokenProvider()
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"expired"}"""))
        server.enqueue(MockResponse().setBody("""{"status":"success","data":{"id":1}}"""))

        val result: LeaveBalance = LiveApiClient(server.url("/").toString(), OkHttpClient(), provider)
            .send(Endpoint("x"), LeaveBalance.serializer())

        assertEquals(1, result.id)
        assertEquals(1, provider.refreshCount.get())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun send_401AfterRetryThrowsUnauthorized() = runTest {
        val provider = StubTokenProvider()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        try {
            LiveApiClient(server.url("/").toString(), OkHttpClient(), provider)
                .send(Endpoint("x"), LeaveBalance.serializer())
            fail("expected Unauthorized")
        } catch (e: ApiError) {
            assertTrue(e is ApiError.Unauthorized)
        }
        assertEquals(1, provider.refreshCount.get())
    }

    @Test
    fun send_serverErrorSurfacesMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"message":"boom"}"""))
        try {
            client().send(Endpoint("x"), LeaveBalance.serializer())
            fail("expected Server error")
        } catch (e: ApiError.Server) {
            assertEquals("boom", e.serverMessage)
            assertEquals(500, e.status)
        }
    }

    @Test
    fun sendVoid_succeedsOnNullData() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"success","data":null}"""))
        client().sendVoid(Endpoint("x", Endpoint.Method.POST))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun sendData_returnsRawBytes() = runTest {
        server.enqueue(MockResponse().setBody("%PDF-1.4 raw"))
        val bytes = client().sendData(Endpoint("payslips/1/download"))
        assertEquals("%PDF-1.4 raw", bytes.decodeToString())
    }

    @Test
    fun send_attachesBearerTokenWhenAuthed() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"success","data":{"id":1}}"""))
        client(StubTokenProvider(token = "abc")).send(Endpoint("x"), LeaveBalance.serializer())
        val recorded = server.takeRequest()
        assertEquals("Bearer abc", recorded.getHeader("Authorization"))
    }
}
