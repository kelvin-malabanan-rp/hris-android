package io.rocketpartners.hris.core.auth

import io.rocketpartners.hris.model.AuthTokens
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Persists the [AuthTokens] pair. Mirrors iOS `TokenStoring`. */
interface TokenStore {
    suspend fun tokens(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}

/** Test/preview implementation; not persisted. Mirrors iOS `InMemoryTokenStore`. */
class InMemoryTokenStore(initial: AuthTokens? = null) : TokenStore {
    private val mutex = Mutex()
    private var stored: AuthTokens? = initial

    override suspend fun tokens(): AuthTokens? = mutex.withLock { stored }
    override suspend fun save(tokens: AuthTokens) = mutex.withLock { stored = tokens }
    override suspend fun clear() = mutex.withLock { stored = null }
}
