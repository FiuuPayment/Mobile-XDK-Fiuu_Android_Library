package com.fiuu.xdkandroid.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.fiuu.xdkandroid.utils.FiuuLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Encrypts with an AES/GCM key held in AndroidKeyStore (never leaves hardware-backed storage)
// rather than pulling in androidx.security.crypto — keeps this to stock Android SDK APIs so no
// new Gradle dependency is needed for a single-purpose store.
class AndroidSecureTokenStore(context: Context) : SecureTokenStore {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun save(session: StoredPaymentSession) {
        withContext(Dispatchers.IO) {
            try {
                val plaintext = Json.encodeToString(session)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                val ciphertext = cipher.doFinal(plaintext.encodeToByteArray())
                val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
                val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
                prefs.edit().putString(SESSION_KEY, "$ivB64:$cipherB64").apply()
                if (FiuuLogger.enabled) Log.d(TAG, "save: encrypted session written for orderId=${session.orderId}, expiresAt=${session.expiresAtEpochSeconds}")
            } catch (e: Exception) {
                Log.e(TAG, "save: failed for orderId=${session.orderId}", e)
            }
        }
    }

    override suspend fun get(): StoredPaymentSession? {
        return withContext(Dispatchers.IO) {
            val session = decryptEntry()
            if (session == null) {
                if (FiuuLogger.enabled) Log.d(TAG, "get: no usable session stored")
            } else {
                if (FiuuLogger.enabled) Log.d(TAG, "get: decrypted session found for orderId=${session.orderId}, expiresAt=${session.expiresAtEpochSeconds}")
            }
            session
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            prefs.edit().remove(SESSION_KEY).apply()
            if (FiuuLogger.enabled) Log.d(TAG, "clear: session removed")
        }
    }

    override suspend fun clearExpired(nowEpochSeconds: Long) {
        withContext(Dispatchers.IO) {
            // A null session here means either the TTL passed or the entry is undecryptable
            // (e.g. the AndroidKeyStore key was rotated/wiped) — either way it can never be
            // resumed again, so it's safe to drop opportunistically.
            val session = decryptEntry() ?: return@withContext
            if (session.expiresAtEpochSeconds <= nowEpochSeconds) {
                prefs.edit().remove(SESSION_KEY).apply()
                if (FiuuLogger.enabled) Log.d(TAG, "clearExpired: removed stale session for orderId=${session.orderId}")
            }
        }
    }

    private fun decryptEntry(): StoredPaymentSession? {
        return try {
            val stored = prefs.getString(SESSION_KEY, null) ?: return null
            val (ivB64, cipherB64) = stored.split(":", limit = 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plaintext = cipher.doFinal(Base64.decode(cipherB64, Base64.NO_WRAP))
            decodeStoredPaymentSession(plaintext.decodeToString())
        } catch (e: Exception) {
            Log.e(TAG, "decryptEntry: failed", e)
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val TAG = "XDK-SecureStore"
        const val SESSION_KEY = "latest_session"
        const val PREFS_NAME = "fiuu_xdk_secure_sessions"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "fiuu_xdk_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
