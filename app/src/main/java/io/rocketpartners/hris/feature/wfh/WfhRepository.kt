package io.rocketpartners.hris.feature.wfh

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.WfhSchedule
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

interface WfhRepository {
    suspend fun schedules(month: YearMonth): List<WfhSchedule>
    suspend fun weeklyUsage(): WfhWeeklyUsage
    suspend fun schedule(dates: List<LocalDate>, reason: String?): List<WfhSchedule>
    suspend fun cancel(id: Int): WfhSchedule
    suspend fun pendingApprovals(): List<WfhSchedule>
    suspend fun approve(id: Int, comments: String?): WfhSchedule
    suspend fun reject(id: Int, comments: String?): WfhSchedule
}

class LiveWfhRepository(private val client: ApiClient) : WfhRepository {

    @Serializable
    private data class ScheduleBody(val dates: List<String>, val reason: String?)

    @Serializable
    private data class CommentsBody(val comments: String)

    override suspend fun schedules(month: YearMonth): List<WfhSchedule> =
        client.send(Endpoint("wfh/schedules", query = listOf("month" to month.toString())))

    override suspend fun weeklyUsage(): WfhWeeklyUsage =
        client.send(Endpoint("wfh/weekly-usage"))

    override suspend fun schedule(dates: List<LocalDate>, reason: String?): List<WfhSchedule> {
        val body = AppJson.encodeToString(ScheduleBody(dates.map { it.toString() }, reason)).encodeToByteArray()
        return client.send(Endpoint("wfh/schedules", Endpoint.Method.POST, body = body))
    }

    override suspend fun cancel(id: Int): WfhSchedule =
        client.send(Endpoint("wfh/schedules/$id/cancel", Endpoint.Method.POST))

    override suspend fun pendingApprovals(): List<WfhSchedule> =
        client.send(Endpoint("wfh/pending-approvals"))

    override suspend fun approve(id: Int, comments: String?): WfhSchedule =
        client.send(Endpoint("wfh/schedules/$id/approve", Endpoint.Method.POST, body = reviewBody(comments)))

    override suspend fun reject(id: Int, comments: String?): WfhSchedule =
        client.send(Endpoint("wfh/schedules/$id/reject", Endpoint.Method.POST, body = reviewBody(comments)))

    /** Optional `{ "comments": ... }` body shared by approve/reject. Null/empty → no body. */
    private fun reviewBody(comments: String?): ByteArray? =
        comments?.takeIf { it.isNotEmpty() }?.let { AppJson.encodeToString(CommentsBody(it)).encodeToByteArray() }
}
