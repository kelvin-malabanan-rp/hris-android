package io.rocketpartners.hris.core.auth

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.model.AuthTokens
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AuthCoordinatorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun coordinator(store: TokenStore) =
        AuthCoordinator(server.url("/").toString(), OkHttpClient(), store)

    @Test
    fun accessToken_readsFromStore() = runTest {
        val store = InMemoryTokenStore(AuthTokens("acc", "ref"))
        assertEquals("acc", coordinator(store).accessToken())
    }

    @Test
    fun refresh_persistsNewTokens() = runTest {
        val store = InMemoryTokenStore(AuthTokens("old-acc", "old-ref"))
        server.enqueue(
            MockResponse().setBody("""{"status":"success","data":{"accessToken":"new-acc","refreshToken":"new-ref"}}"""),
        )
        coordinator(store).refresh()
        assertEquals(AuthTokens("new-acc", "new-ref"), store.tokens())
    }

    @Test
    fun refresh_coalescesConcurrentCallsIntoOneNetworkHit() = runTest {
        val store = InMemoryTokenStore(AuthTokens("old-acc", "old-ref"))
        // Single delayed response; a second network hit would consume the second enqueue.
        server.enqueue(
            MockResponse()
                .setBody("""{"status":"success","data":{"accessToken":"a","refreshToken":"b"}}""")
                .setBodyDelay(300, TimeUnit.MILLISECONDS),
        )
        server.enqueue(MockResponse().setBody("""{"status":"success","data":{"accessToken":"x","refreshToken":"y"}}"""))

        val coordinator = coordinator(store)
        coroutineScope {
            val a = async { coordinator.refresh() }
            val b = async { coordinator.refresh() }
            awaitAll(a, b)
        }
        assertEquals(1, server.requestCount)
        assertEquals(AuthTokens("a", "b"), store.tokens())
    }

    @Test
    fun refresh_withNoRefreshTokenThrowsUnauthorized() = runTest {
        try {
            coordinator(InMemoryTokenStore(null)).refresh()
            fail("expected Unauthorized")
        } catch (e: ApiError) {
            assertTrue(e is ApiError.Unauthorized)
        }
    }

    @Test
    fun refresh_with401ThrowsUnauthorizedAndKeepsOldTokens() = runTest {
        val store = InMemoryTokenStore(AuthTokens("old-acc", "old-ref"))
        server.enqueue(MockResponse().setResponseCode(401))
        try {
            coordinator(store).refresh()
            fail("expected Unauthorized")
        } catch (e: ApiError) {
            assertTrue(e is ApiError.Unauthorized)
        }
        assertEquals(AuthTokens("old-acc", "old-ref"), store.tokens())
    }

    @Test
    fun refresh_allowsAnotherRefreshAfterCompletion() = runTest {
        val store = InMemoryTokenStore(AuthTokens("old", "old"))
        server.enqueue(MockResponse().setBody("""{"status":"success","data":{"accessToken":"a1","refreshToken":"r1"}}"""))
        server.enqueue(MockResponse().setBody("""{"status":"success","data":{"accessToken":"a2","refreshToken":"r2"}}"""))
        val coordinator = coordinator(store)
        coordinator.refresh()
        coordinator.refresh()
        assertEquals(AuthTokens("a2", "r2"), store.tokens())
        assertEquals(2, server.requestCount)
    }
}
