package io.rocketpartners.hris.feature.auth

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.model.User
import io.rocketpartners.hris.support.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceTest {

    @Test
    fun bootstrap_successMovesToAuthenticated() = runTest {
        val user = User(7, "Grace", "grace@rp.io")
        val service = AuthService(FakeAuthRepository(currentUserResult = Result.success(user)))
        service.bootstrap()
        assertEquals(AuthState.Authenticated(user), service.state.value)
    }

    @Test
    fun bootstrap_failureMovesToUnauthenticated() = runTest {
        val service = AuthService(
            FakeAuthRepository(currentUserResult = Result.failure(ApiError.Unauthorized)),
        )
        service.bootstrap()
        assertEquals(AuthState.Unauthenticated, service.state.value)
    }

    @Test
    fun login_successMovesToAuthenticated() = runTest {
        val user = User(1, "Ada", "ada@rp.io")
        val repo = FakeAuthRepository(loginResult = Result.success(user))
        val service = AuthService(repo)
        service.login("ada@rp.io", "pw")
        assertEquals(AuthState.Authenticated(user), service.state.value)
        assertEquals(listOf("ada@rp.io" to "pw"), repo.loginCalls)
    }

    @Test
    fun login_failureSurfacesUserMessageAndStaysUnauthenticated() = runTest {
        val service = AuthService(
            FakeAuthRepository(loginResult = Result.failure(ApiError.Server("Bad creds", 401))),
        )
        service.login("a@b.co", "pw")
        assertEquals(AuthState.Unauthenticated, service.state.value)
        assertEquals("Bad creds", service.errorMessage.value)
    }

    @Test
    fun login_nonApiErrorFallsBackToGenericMessage() = runTest {
        val service = AuthService(
            FakeAuthRepository(loginResult = Result.failure(RuntimeException("boom"))),
        )
        service.login("a@b.co", "pw")
        assertEquals("Sign in failed.", service.errorMessage.value)
    }

    @Test
    fun logout_alwaysMovesToUnauthenticatedEvenIfRepoThrows() = runTest {
        val service = AuthService(
            FakeAuthRepository(logoutResult = Result.failure(ApiError.Network("offline"))),
        )
        service.logout()
        assertEquals(AuthState.Unauthenticated, service.state.value)
    }

    @Test
    fun clearError_resetsMessage() = runTest {
        val service = AuthService(
            FakeAuthRepository(loginResult = Result.failure(ApiError.Server("x", 400))),
        )
        service.login("a@b.co", "pw")
        assertTrue(service.errorMessage.value != null)
        service.clearError()
        assertNull(service.errorMessage.value)
    }
}
