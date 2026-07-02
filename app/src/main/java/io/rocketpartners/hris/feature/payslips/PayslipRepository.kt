package io.rocketpartners.hris.feature.payslips

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.Paged
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.Payslip

interface PayslipRepository {
    /** The signed-in employee's payslips (newest-first), optionally filtered by pay period. */
    suspend fun myPayslips(payPeriodId: Int? = null): List<Payslip>

    /** The raw PDF bytes for a payslip. Binary (no envelope); 404 ⇒ not available. */
    suspend fun downloadData(id: Int): ByteArray
}

class LivePayslipRepository(private val client: ApiClient) : PayslipRepository {

    override suspend fun myPayslips(payPeriodId: Int?): List<Payslip> {
        // `employeeId` is forced server-side. Accumulate all pages so history isn't truncated.
        val all = mutableListOf<Payslip>()
        var page = 0
        while (true) {
            val query = buildList {
                add("page" to "$page")
                add("size" to "100")
                if (payPeriodId != null) add("payPeriodId" to "$payPeriodId")
            }
            val result: Paged<Payslip> =
                client.send(Endpoint("payslips/me", query = query), Paged.serializer(Payslip.serializer()))
            all.addAll(result.content)
            if (result.last != false || result.content.isEmpty()) break
            page++
        }
        return all
    }

    /** Raw binary; the URL is built from [id] (the wire `downloadUrl` omits `/api/v1`). */
    override suspend fun downloadData(id: Int): ByteArray =
        client.sendData(Endpoint("payslips/$id/download"))
}
