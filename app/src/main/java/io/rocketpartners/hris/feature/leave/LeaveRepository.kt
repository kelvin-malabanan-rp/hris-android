package io.rocketpartners.hris.feature.leave

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.Paged
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveApprovalStage
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.LeaveType
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Input for creating a leave application; the repository maps it to the wire body. Mirrors iOS. */
data class NewLeaveApplication(
    val leaveTypeId: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
)

interface LeaveRepository {
    suspend fun myLeaves(): List<LeaveApplication>
    suspend fun balances(): List<LeaveBalance>
    suspend fun activeLeaveTypes(): List<LeaveType>
    suspend fun apply(draft: NewLeaveApplication): LeaveApplication
    suspend fun edit(id: Int, draft: NewLeaveApplication): LeaveApplication
    suspend fun cancel(id: Int): LeaveApplication
    suspend fun requestCancellation(id: Int, reason: String): LeaveApplication
    suspend fun pendingApprovals(): List<LeaveApplication>
    suspend fun approve(id: Int, stage: LeaveApprovalStage, comments: String?): LeaveApplication
    suspend fun reject(id: Int, stage: LeaveApprovalStage, comments: String?): LeaveApplication
    suspend fun approveCancellation(id: Int, comments: String?): LeaveApplication
    suspend fun rejectCancellation(id: Int, comments: String?): LeaveApplication
}

class LiveLeaveRepository(private val client: ApiClient) : LeaveRepository {

    @Serializable
    private data class ApplyBody(
        val leaveTypeId: Int,
        val startDate: String,
        val endDate: String,
        val reason: String?,
    )

    @Serializable
    private data class CommentsBody(val comments: String)

    @Serializable
    private data class ReasonBody(val reason: String)

    /** `{ "page": <PagedResponse>, "summary": {...} }` — the page lives at `data.page`. */
    @Serializable
    private data class PendingEnvelope(val page: PendingPage)

    @Serializable
    private data class PendingPage(val content: List<LeaveApplication>, val last: Boolean? = null)

    private fun stageSegment(stage: LeaveApprovalStage) = if (stage == LeaveApprovalStage.MANAGER) "manager" else "hr"

    /** Fetches every page, accumulating `content` until `last == true` (stops if the server omits it). */
    private suspend fun <T> fetchAllPages(
        path: String,
        elementSerializer: KSerializer<T>,
        extraQuery: List<Pair<String, String>> = emptyList(),
    ): List<T> {
        val all = mutableListOf<T>()
        var page = 0
        while (true) {
            val query = extraQuery + listOf("page" to "$page", "size" to "$PAGE_SIZE")
            val result: Paged<T> =
                client.send(Endpoint(path, query = query), Paged.serializer(elementSerializer))
            all.addAll(result.content)
            if (result.last != false || result.content.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun myLeaves(): List<LeaveApplication> =
        fetchAllPages("leave-applications/my", LeaveApplication.serializer(), listOf("sort" to "createdAt,desc"))

    override suspend fun balances(): List<LeaveBalance> =
        client.send(Endpoint("leave-applications/balances/my"))

    override suspend fun activeLeaveTypes(): List<LeaveType> =
        client.send(Endpoint("leave-types/active"))

    override suspend fun apply(draft: NewLeaveApplication): LeaveApplication =
        client.send(Endpoint("leave-applications", Endpoint.Method.POST, body = applyBody(draft)))

    override suspend fun edit(id: Int, draft: NewLeaveApplication): LeaveApplication =
        client.send(Endpoint("leave-applications/$id", Endpoint.Method.PUT, body = applyBody(draft)))

    override suspend fun cancel(id: Int): LeaveApplication =
        client.send(Endpoint("leave-applications/$id/cancel", Endpoint.Method.POST))

    override suspend fun requestCancellation(id: Int, reason: String): LeaveApplication =
        client.send(
            Endpoint(
                "leave-applications/$id/request-cancellation",
                Endpoint.Method.POST,
                body = AppJson.encodeToString(ReasonBody(reason)).encodeToByteArray(),
            ),
        )

    override suspend fun pendingApprovals(): List<LeaveApplication> {
        val all = mutableListOf<LeaveApplication>()
        var page = 0
        while (true) {
            val endpoint = Endpoint(
                "leave-applications/pending-approvals",
                query = listOf("page" to "$page", "size" to "$PAGE_SIZE"),
            )
            val response: PendingEnvelope = client.send(endpoint, PendingEnvelope.serializer())
            all.addAll(response.page.content)
            if (response.page.last != false || response.page.content.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun approve(id: Int, stage: LeaveApprovalStage, comments: String?): LeaveApplication =
        client.send(
            Endpoint("leave-applications/$id/${stageSegment(stage)}/approve", Endpoint.Method.POST, body = approveBody(comments)),
        )

    override suspend fun reject(id: Int, stage: LeaveApprovalStage, comments: String?): LeaveApplication =
        client.send(
            Endpoint("leave-applications/$id/${stageSegment(stage)}/reject", Endpoint.Method.POST, body = rejectBody(comments)),
        )

    override suspend fun approveCancellation(id: Int, comments: String?): LeaveApplication =
        client.send(
            Endpoint("leave-applications/$id/cancellation/approve", Endpoint.Method.POST, body = approveBody(comments)),
        )

    override suspend fun rejectCancellation(id: Int, comments: String?): LeaveApplication =
        client.send(
            Endpoint("leave-applications/$id/cancellation/reject", Endpoint.Method.POST, body = rejectBody(comments)),
        )

    private fun applyBody(draft: NewLeaveApplication): ByteArray =
        AppJson.encodeToString(
            ApplyBody(draft.leaveTypeId, draft.startDate.toString(), draft.endDate.toString(), draft.reason),
        ).encodeToByteArray()

    /** Optional `{ "comments": ... }` body for approve. Null/empty → no body. */
    private fun approveBody(comments: String?): ByteArray? =
        comments?.takeIf { it.isNotEmpty() }?.let { AppJson.encodeToString(CommentsBody(it)).encodeToByteArray() }

    /** Reject always sends a body; empty comments → `{}`. */
    private fun rejectBody(comments: String?): ByteArray =
        comments?.takeIf { it.isNotEmpty() }?.let { AppJson.encodeToString(CommentsBody(it)).encodeToByteArray() }
            ?: "{}".encodeToByteArray()

    private companion object {
        const val PAGE_SIZE = 100
    }
}
