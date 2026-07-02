package io.rocketpartners.hris.feature.notifications

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.Paged
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.AppNotification
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

interface NotificationRepository {
    suspend fun list(): List<AppNotification>
    suspend fun unreadCount(): Int
    suspend fun markRead(id: Int): AppNotification
    suspend fun markAllRead()
    suspend fun registerDevice(token: String, environment: String)
    suspend fun unregisterDevice(token: String)
}

class LiveNotificationRepository(private val client: ApiClient) : NotificationRepository {

    @Serializable private data class CountResponse(val count: Int)
    @Serializable private data class DeviceBody(val token: String, val platform: String, val environment: String)
    @Serializable private data class TokenBody(val token: String)

    override suspend fun list(): List<AppNotification> {
        val all = mutableListOf<AppNotification>()
        var page = 0
        while (true) {
            val result: Paged<AppNotification> = client.send(
                Endpoint("notifications", query = listOf("page" to "$page", "size" to "$PAGE_SIZE")),
                Paged.serializer(AppNotification.serializer()),
            )
            all.addAll(result.content)
            if (result.last != false || result.content.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun unreadCount(): Int =
        client.send<CountResponse>(Endpoint("notifications/unread-count")).count

    override suspend fun markRead(id: Int): AppNotification =
        client.send(Endpoint("notifications/$id/read", Endpoint.Method.PATCH))

    override suspend fun markAllRead() =
        client.sendVoid(Endpoint("notifications/read-all", Endpoint.Method.PATCH))

    override suspend fun registerDevice(token: String, environment: String) {
        val body = AppJson.encodeToString(DeviceBody(token, "ANDROID", environment)).encodeToByteArray()
        client.sendVoid(Endpoint("notifications/devices", Endpoint.Method.POST, body = body))
    }

    override suspend fun unregisterDevice(token: String) {
        val body = AppJson.encodeToString(TokenBody(token)).encodeToByteArray()
        client.sendVoid(Endpoint("notifications/devices", Endpoint.Method.DELETE, body = body))
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
