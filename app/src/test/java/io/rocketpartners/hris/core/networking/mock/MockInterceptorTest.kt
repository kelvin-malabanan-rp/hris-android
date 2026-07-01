package io.rocketpartners.hris.core.networking.mock

import io.rocketpartners.hris.core.auth.TokenProviding
import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.LiveApiClient
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.User
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MockInterceptorTest {

    private val baseUrl = "https://mock.local/api/v1"

    private class FixedToken(private val token: String?) : TokenProviding {
        override suspend fun accessToken(): String? = token
        override suspend fun refresh() = Unit
    }

    private fun client(autoLogin: Boolean = false, token: String? = "t") = LiveApiClient(
        baseUrl,
        OkHttpClient.Builder().addInterceptor(MockInterceptor(autoLogin)).build(),
        FixedToken(token),
    )

    @Test
    fun authMe_withTokenReturnsMockUser() = runTest {
        val user: User = client().send(Endpoint("auth/me"))
        assertEquals("Angelo Soliveres", user.name)
        assertTrue(user.canApproveWfh)
    }

    @Test
    fun authMe_withoutTokenAndNoAutoLogin401s() = runTest {
        try {
            client(token = null).send<User>(Endpoint("auth/me"))
            fail("expected Unauthorized")
        } catch (e: ApiError) {
            assertTrue(e is ApiError.Unauthorized)
        }
    }

    @Test
    fun authMe_withoutTokenButAutoLoginResolves() = runTest {
        val user: User = client(autoLogin = true, token = null).send(Endpoint("auth/me"))
        assertEquals(42, user.id)
    }

    @Test
    fun leaveBalances_decodeFromFixture() = runTest {
        val balances: List<LeaveBalance> =
            client().send(Endpoint("leave-applications/balances/my"), ListSerializer(LeaveBalance.serializer()))
        assertEquals(4, balances.size)
        assertEquals("Annual Leave", balances.first().leaveTypeName)
    }

    @Test
    fun payslipDownload_returnsPdfBytes() = runTest {
        val bytes = client().sendData(Endpoint("payslips/9001/download"))
        assertTrue(bytes.decodeToString().startsWith("%PDF"))
    }
}
