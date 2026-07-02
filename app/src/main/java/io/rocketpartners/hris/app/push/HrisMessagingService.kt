package io.rocketpartners.hris.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.rocketpartners.hris.BuildConfig
import io.rocketpartners.hris.app.AppEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives FCM lifecycle callbacks. On a new token it registers with the backend; on a message it
 * posts a notification whose tap intent carries the deep-link reference. Builds a lightweight
 * [AppEnvironment] on demand — it shares the encrypted token store with the app, so registration is
 * authenticated whenever the user has a live session. Mirrors iOS `AppDelegate` push handling.
 */
class HrisMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val environment = AppEnvironment(applicationContext, mock = false, autoLogin = false)
        val push = PushService.from(applicationContext, environment.notificationRepository, BuildConfig.DEBUG)
        scope.launch { push.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"]
        val body = message.notification?.body ?: data["body"] ?: data["message"]
        PushNotifications.show(
            context = applicationContext,
            title = title,
            body = body,
            referenceType = data["referenceType"],
            referenceId = data["referenceId"],
            type = data["type"],
        )
    }
}
