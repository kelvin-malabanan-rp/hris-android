package io.rocketpartners.hris.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.model.AuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores the [AuthTokens] pair as a single JSON blob in [EncryptedSharedPreferences], the Android
 * counterpart to the iOS `KeychainTokenStore`. The prefs file is backed by an AndroidKeyStore
 * master key, so at-rest data is hardware-encrypted where available.
 */
class EncryptedTokenStore(
    context: Context,
    fileName: String = "hris_auth_tokens",
) : TokenStore {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun tokens(): AuthTokens? = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_TOKENS, null) ?: return@withContext null
        runCatching { AppJson.decodeFromString(AuthTokens.serializer(), json) }.getOrNull()
    }

    override suspend fun save(tokens: AuthTokens) = withContext(Dispatchers.IO) {
        val json = AppJson.encodeToString(AuthTokens.serializer(), tokens)
        prefs.edit().putString(KEY_TOKENS, json).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_TOKENS).apply()
    }

    private companion object {
        const val KEY_TOKENS = "auth-tokens"
    }
}
