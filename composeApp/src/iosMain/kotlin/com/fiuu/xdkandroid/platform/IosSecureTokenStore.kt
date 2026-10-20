package com.fiuu.xdkandroid.platform

import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

import kotlinx.cinterop.BetaInteropApi

// Keychain is encrypted at rest by the OS by default — no extra dependency needed. Only a single
// generic-password item is ever stored under a fixed account name — the latest session overwrites
// whatever was there before, matching the single-slot SecureTokenStore contract.
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureTokenStore : SecureTokenStore {

    private inline fun <R> withCFDictionary(
        pairs: List<Pair<CFTypeRef?, CFTypeRef?>>,
        block: (CFDictionaryRef) -> R
    ): R {
        return memScoped {
            val count = pairs.size
            val keys = allocArray<COpaquePointerVar>(count)
            val values = allocArray<COpaquePointerVar>(count)
            pairs.forEachIndexed { i, (k, v) ->
                keys[i] = k
                values[i] = v
            }
            val dict = CFDictionaryCreate(
                kCFAllocatorDefault,
                keys,
                values,
                count.toLong(),
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr
            ) ?: error("Failed to create CFDictionary")
            try {
                block(dict)
            } finally {
                CFRelease(dict)
            }
        }
    }

    override suspend fun save(session: StoredPaymentSession) {
        withContext(Dispatchers.Default) {
            deleteItem()
            val json = Json.encodeToString(session)
            val data = (json as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return@withContext
            val serviceRef = CFBridgingRetain(SERVICE as NSString)
            val accountRef = CFBridgingRetain(ACCOUNT as NSString)
            val dataRef = CFBridgingRetain(data)
            try {
                withCFDictionary(
                    listOf(
                        kSecClass to kSecClassGenericPassword,
                        kSecAttrService to serviceRef,
                        kSecAttrAccount to accountRef,
                        // The session holds the accessToken and the merchant vkey — keep it out of
                        // iCloud/iTunes backups and off other devices. Same availability as the
                        // default (WhenUnlocked): resume only ever runs with the app in front.
                        kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                        kSecValueData to dataRef
                    )
                ) { dict ->
                    SecItemAdd(dict, null)
                }
            } finally {
                CFBridgingRelease(serviceRef)
                CFBridgingRelease(accountRef)
                CFBridgingRelease(dataRef)
            }
        }
    }

    override suspend fun get(): StoredPaymentSession? {
        return withContext(Dispatchers.Default) {
            readRawJson(SERVICE)?.let { return@withContext decodeStoredPaymentSession(it) }
            // Fall back to the pre-rename Keychain service — a session saved before the SDK was
            // renamed from xdklibrary to xdkandroid would otherwise be permanently orphaned
            // (still present in Keychain, but never looked up again since SERVICE changed).
            // Migrate it into the current service and remove the old entry so it isn't read twice.
            val legacyJson = readRawJson(LEGACY_SERVICE) ?: return@withContext null
            val session = decodeStoredPaymentSession(legacyJson)
            if (session == null) {
                deleteItem(LEGACY_SERVICE)
                return@withContext null
            }
            save(session)
            deleteItem(LEGACY_SERVICE)
            session
        }
    }

    private fun readRawJson(service: String): String? {
        val serviceRef = CFBridgingRetain(service as NSString)
        val accountRef = CFBridgingRetain(ACCOUNT as NSString)
        try {
            return memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = withCFDictionary(
                    listOf(
                        kSecClass to kSecClassGenericPassword,
                        kSecAttrService to serviceRef,
                        kSecAttrAccount to accountRef,
                        kSecReturnData to kCFBooleanTrue,
                        kSecMatchLimit to kSecMatchLimitOne
                    )
                ) { dict ->
                    SecItemCopyMatching(dict, result.ptr)
                }
                if (status != errSecSuccess) return@memScoped null
                val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
                NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
            }
        } finally {
            CFBridgingRelease(serviceRef)
            CFBridgingRelease(accountRef)
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.Default) {
            deleteItem(SERVICE)
            deleteItem(LEGACY_SERVICE)
        }
    }

    // Only runs opportunistically when a new /initiate or /express call starts (no background
    // job) — drops the single stored session if its TTL has already passed.
    override suspend fun clearExpired(nowEpochSeconds: Long) {
        withContext(Dispatchers.Default) {
            val session = get() ?: return@withContext
            if (session.expiresAtEpochSeconds <= nowEpochSeconds) {
                deleteItem()
            }
        }
    }

    private fun deleteItem(service: String = SERVICE) {
        val serviceRef = CFBridgingRetain(service as NSString)
        val accountRef = CFBridgingRetain(ACCOUNT as NSString)
        try {
            withCFDictionary(
                listOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to serviceRef,
                    kSecAttrAccount to accountRef
                )
            ) { dict ->
                SecItemDelete(dict)
            }
        } finally {
            CFBridgingRelease(serviceRef)
            CFBridgingRelease(accountRef)
        }
    }

    private companion object {
        const val SERVICE = "com.fiuu.xdkandroid.session"
        // Pre-rename Keychain service name (SDK package was com.fiuu.xdklibrary) — see get()'s
        // migration fallback.
        const val LEGACY_SERVICE = "com.fiuu.xdklibrary.session"
        const val ACCOUNT = "latest_session"
    }
}
