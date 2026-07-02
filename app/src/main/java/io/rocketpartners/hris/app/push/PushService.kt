package io.rocketpartners.hris.app.push

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import io.rocketpartners.hris.feature.notifications.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Persists the last device token we registered, so we only re-POST when it changes. */
interface TokenCache {
    fun lastToken(): String?
    fun setLastToken(token: String?)
}

/**
 * Registers this device's FCM token with the backend, de-duplicating so we only POST when the token
 * actually changes. Mirrors iOS `PushService`: token cached via [TokenCache],
 * `POST /notifications/devices` on change, `DELETE` on sign-out. FCM stands in for APNs; the
 * `environment` string is still sent for parity with the iOS contract.
 */
class PushService(
    private val repository: NotificationRepository,
    private val cache: TokenCache,
    private val debug: Boolean,
) {
    /** Fetches the current FCM token and registers it with the backend if it changed. */
    suspend fun registerIfNeeded() {
        val token = runCatching { currentToken() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        register(token)
    }

    /** Registers a known [token] (e.g. from `onNewToken`) with the backend if it changed. */
    suspend fun register(token: String) {
        if (!shouldRegister(token, cache.lastToken())) return
        runCatching { repository.registerDevice(token, pushEnvironment(debug)) }
            .onSuccess { cache.setLastToken(token) }
    }

    /** Unregisters the last-known token (call on sign-out) and forgets it locally. */
    suspend fun unregister() {
        val stored = cache.lastToken() ?: return
        runCatching { repository.unregisterDevice(stored) }
        cache.setLastToken(null)
    }

    private suspend fun currentToken(): String =
        withContext(Dispatchers.IO) { Tasks.await(FirebaseMessaging.getInstance().token) }

    companion object {
        const val LAST_TOKEN_KEY = "io.rocketpartners.hris.lastDeviceToken"

        /** Register only for a non-blank token that differs from the last one we sent. */
        fun shouldRegister(new: String, stored: String?): Boolean = new.isNotBlank() && new != stored

        /** Mirrors iOS: debug builds report "sandbox", release builds "production". */
        fun pushEnvironment(debug: Boolean): String = if (debug) "sandbox" else "production"

        fun from(context: Context, repository: NotificationRepository, debug: Boolean): PushService =
            PushService(repository, PrefsTokenCache(context), debug)
    }
}

/** [TokenCache] backed by a private [android.content.SharedPreferences] file. */
private class PrefsTokenCache(context: Context) : TokenCache {
    private val prefs = context.getSharedPreferences("hris_push", Context.MODE_PRIVATE)
    override fun lastToken(): String? = prefs.getString(PushService.LAST_TOKEN_KEY, null)
    override fun setLastToken(token: String?) {
        prefs.edit().apply { if (token == null) remove(PushService.LAST_TOKEN_KEY) else putString(PushService.LAST_TOKEN_KEY, token) }.apply()
    }
}
