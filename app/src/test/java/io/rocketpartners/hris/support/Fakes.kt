package io.rocketpartners.hris.support

import io.rocketpartners.hris.feature.auth.AuthRepository
import io.rocketpartners.hris.model.User

/**
 * Hand-written fakes for store/service tests, mirroring the iOS `Tests/Support/Fakes.swift`
 * convention: `Result`-typed stub properties + recorded calls.
 */
class FakeAuthRepository(
    var loginResult: Result<User> = Result.success(User(1, "Ada", "ada@rp.io")),
    var currentUserResult: Result<User> = Result.success(User(1, "Ada", "ada@rp.io")),
    var logoutResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    val loginCalls = mutableListOf<Pair<String, String>>()
    var logoutCallCount = 0

    override suspend fun login(email: String, password: String): User {
        loginCalls.add(email to password)
        return loginResult.getOrThrow()
    }

    override suspend fun currentUser(): User = currentUserResult.getOrThrow()

    override suspend fun logout() {
        logoutCallCount++
        logoutResult.getOrThrow()
    }
}
