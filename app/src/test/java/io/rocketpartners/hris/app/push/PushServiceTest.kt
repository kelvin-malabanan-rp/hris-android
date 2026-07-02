package io.rocketpartners.hris.app.push

import io.rocketpartners.hris.feature.notifications.NotificationRepository
import io.rocketpartners.hris.model.AppNotification
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushServiceTest {

    private class FakeCache(var token: String? = null) : TokenCache {
        override fun lastToken(): String? = token
        override fun setLastToken(token: String?) { this.token = token }
    }

    private class RecordingRepo : NotificationRepository {
        val registered = mutableListOf<Pair<String, String>>()
        val unregistered = mutableListOf<String>()
        var failRegister = false
        override suspend fun list(): List<AppNotification> = emptyList()
        override suspend fun unreadCount(): Int = 0
        override suspend fun markRead(id: Int): AppNotification = error("unused")
        override suspend fun markAllRead() {}
        override suspend fun registerDevice(token: String, environment: String) {
            if (failRegister) throw RuntimeException("network")
            registered.add(token to environment)
        }
        override suspend fun unregisterDevice(token: String) { unregistered.add(token) }
    }

    @Test
    fun shouldRegister_onlyForChangedNonBlankToken() {
        assertTrue(PushService.shouldRegister("a", null))
        assertTrue(PushService.shouldRegister("a", "b"))
        assertFalse(PushService.shouldRegister("a", "a"))
        assertFalse(PushService.shouldRegister("", null))
    }

    @Test
    fun pushEnvironment_matchesIosContract() {
        assertEquals("sandbox", PushService.pushEnvironment(debug = true))
        assertEquals("production", PushService.pushEnvironment(debug = false))
    }

    @Test
    fun register_postsAndCachesOnChange() = runTest {
        val repo = RecordingRepo()
        val cache = FakeCache()
        PushService(repo, cache, debug = true).register("tok-1")
        assertEquals(listOf("tok-1" to "sandbox"), repo.registered)
        assertEquals("tok-1", cache.token)
    }

    @Test
    fun register_skipsWhenTokenUnchanged() = runTest {
        val repo = RecordingRepo()
        val cache = FakeCache(token = "tok-1")
        PushService(repo, cache, debug = false).register("tok-1")
        assertTrue(repo.registered.isEmpty())
    }

    @Test
    fun register_doesNotCacheWhenPostFails() = runTest {
        val repo = RecordingRepo().apply { failRegister = true }
        val cache = FakeCache()
        PushService(repo, cache, debug = true).register("tok-1")
        assertNull(cache.token)
    }

    @Test
    fun unregister_deletesAndClearsCache() = runTest {
        val repo = RecordingRepo()
        val cache = FakeCache(token = "tok-9")
        PushService(repo, cache, debug = false).unregister()
        assertEquals(listOf("tok-9"), repo.unregistered)
        assertNull(cache.token)
    }

    @Test
    fun unregister_noopWhenNothingCached() = runTest {
        val repo = RecordingRepo()
        PushService(repo, FakeCache(token = null), debug = false).unregister()
        assertTrue(repo.unregistered.isEmpty())
    }
}
