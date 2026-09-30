/*
 * Copyright 2024 Fiuu.
 */

#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <cstdio>
#include <cstdint>

namespace {

// ============================================================================
// Standard RFC 1321 MD5 Implementation (Self-Contained, No External OpenSSL Dep)
// ============================================================================

struct MD5Context {
    uint32_t state[4];
    uint32_t count[2];
    uint8_t buffer[64];
};

#define F(x, y, z) (((x) & (y)) | ((~x) & (z)))
#define G(x, y, z) (((x) & (z)) | ((y) & (~z)))
#define H(x, y, z) ((x) ^ (y) ^ (z))
#define I(x, y, z) ((y) ^ ((x) | (~z)))

#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32-(n))))

#define FF(a, b, c, d, x, s, ac) { \
    (a) += F ((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT ((a), (s)); \
    (a) += (b); \
}
#define GG(a, b, c, d, x, s, ac) { \
    (a) += G ((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT ((a), (s)); \
    (a) += (b); \
}
#define HH(a, b, c, d, x, s, ac) { \
    (a) += H ((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT ((a), (s)); \
    (a) += (b); \
}
#define II(a, b, c, d, x, s, ac) { \
    (a) += I ((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT ((a), (s)); \
    (a) += (b); \
}

void MD5Transform(uint32_t state[4], const uint8_t block[64]) {
    uint32_t a = state[0], b = state[1], c = state[2], d = state[3], x[16];

    for (int i = 0, j = 0; j < 64; i++, j += 4) {
        x[i] = ((uint32_t)block[j]) | (((uint32_t)block[j+1]) << 8) |
               (((uint32_t)block[j+2]) << 16) | (((uint32_t)block[j+3]) << 24);
    }

    // Round 1
    FF(a, b, c, d, x[ 0],  7, 0xd76aa478);
    FF(d, a, b, c, x[ 1], 12, 0xe8c7b756);
    FF(c, d, a, b, x[ 2], 17, 0x242070db);
    FF(b, c, d, a, x[ 3], 22, 0xc1bdceee);
    FF(a, b, c, d, x[ 4],  7, 0xf57c0faf);
    FF(d, a, b, c, x[ 5], 12, 0x4787c62a);
    FF(c, d, a, b, x[ 6], 17, 0xa8304613);
    FF(b, c, d, a, x[ 7], 22, 0xfd469501);
    FF(a, b, c, d, x[ 8],  7, 0x698098d8);
    FF(d, a, b, c, x[ 9], 12, 0x8b44f7af);
    FF(c, d, a, b, x[10], 17, 0xffff5bb1);
    FF(b, c, d, a, x[11], 22, 0x895cd7be);
    FF(a, b, c, d, x[12],  7, 0x6b901122);
    FF(d, a, b, c, x[13], 12, 0xfd987193);
    FF(c, d, a, b, x[14], 17, 0xa679438e);
    FF(b, c, d, a, x[15], 22, 0x49b40821);

    // Round 2
    GG(a, b, c, d, x[ 1],  5, 0xf61e2562);
    GG(d, a, b, c, x[ 6],  9, 0xc040b340);
    GG(c, d, a, b, x[11], 14, 0x265e5a51);
    GG(b, c, d, a, x[ 0], 20, 0xe9b6c7aa);
    GG(a, b, c, d, x[ 5],  5, 0xd62f105d);
    GG(d, a, b, c, x[10],  9, 0x02441453);
    GG(c, d, a, b, x[15], 14, 0xd8a1e681);
    GG(b, c, d, a, x[ 4], 20, 0xe7d3fbc8);
    GG(a, b, c, d, x[ 9],  5, 0x21e1cde6);
    GG(d, a, b, c, x[14],  9, 0xc33707d6);
    GG(c, d, a, b, x[ 3], 14, 0xf4d50d87);
    GG(b, c, d, a, x[ 8], 20, 0x455a14ed);
    GG(a, b, c, d, x[13],  5, 0xa9e3e905);
    GG(d, a, b, c, x[ 2],  9, 0xfcefa3f8);
    GG(c, d, a, b, x[ 7], 14, 0x676f02d9);
    GG(b, c, d, a, x[12], 20, 0x8d2a4c8a);

    // Round 3
    HH(a, b, c, d, x[ 5],  4, 0xfffa3942);
    HH(d, a, b, c, x[ 8], 11, 0x8771f681);
    HH(c, d, a, b, x[11], 16, 0x6d9d6122);
    HH(b, c, d, a, x[14], 23, 0xfde5380c);
    HH(a, b, c, d, x[ 1],  4, 0xa4beea44);
    HH(d, a, b, c, x[ 4], 11, 0x4bdecfa9);
    HH(c, d, a, b, x[ 7], 16, 0xf6bb4b60);
    HH(b, c, d, a, x[10], 23, 0xbebfbc70);
    HH(a, b, c, d, x[13],  4, 0x289b7ec6);
    HH(d, a, b, c, x[ 0], 11, 0xeaa127fa);
    HH(c, d, a, b, x[ 3], 16, 0xd4ef3085);
    HH(b, c, d, a, x[ 6], 23, 0x04881d05);
    HH(a, b, c, d, x[ 9],  4, 0xd9d4d039);
    HH(d, a, b, c, x[12], 11, 0xe6db99e5);
    HH(c, d, a, b, x[15], 16, 0x1fa27cf8);
    HH(b, c, d, a, x[ 2], 23, 0xc4ac5665);

    // Round 4
    II(a, b, c, d, x[ 0],  6, 0xf4292244);
    II(d, a, b, c, x[ 7], 10, 0x432aff97);
    II(c, d, a, b, x[14], 15, 0xab9423a7);
    II(b, c, d, a, x[ 5], 21, 0xfc93a039);
    II(a, b, c, d, x[12],  6, 0x655b59c3);
    II(d, a, b, c, x[ 3], 10, 0x8f0ccc92);
    II(c, d, a, b, x[10], 15, 0xffeff47d);
    II(b, c, d, a, x[ 1], 21, 0x85845dd1);
    II(a, b, c, d, x[ 8],  6, 0x6fa87e4f);
    II(d, a, b, c, x[15], 10, 0xfe2ce6e0);
    II(c, d, a, b, x[ 6], 15, 0xa3014314);
    II(b, c, d, a, x[13], 21, 0x4e0811a1);
    II(a, b, c, d, x[ 4],  6, 0xf7537e82);
    II(d, a, b, c, x[11], 10, 0xbd3af235);
    II(c, d, a, b, x[ 2], 15, 0x2ad7d2bb);
    II(b, c, d, a, x[ 9], 21, 0xeb86d391);

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;

    // Secure zeroing
    memset(x, 0, sizeof(x));
}

void MD5Init(MD5Context* context) {
    context->count[0] = context->count[1] = 0;
    context->state[0] = 0x67452301;
    context->state[1] = 0xefcdab89;
    context->state[2] = 0x98badcfe;
    context->state[3] = 0x10325476;
}

void MD5Update(MD5Context* context, const uint8_t* input, size_t inputLen) {
    size_t i, index, partLen;

    index = (size_t)((context->count[0] >> 3) & 0x3F);
    if ((context->count[0] += ((uint32_t)inputLen << 3)) < ((uint32_t)inputLen << 3)) {
        context->count[1]++;
    }
    context->count[1] += ((uint32_t)inputLen >> 29);

    partLen = 64 - index;
    if (inputLen >= partLen) {
        memcpy(&context->buffer[index], input, partLen);
        MD5Transform(context->state, context->buffer);
        for (i = partLen; i + 63 < inputLen; i += 64) {
            MD5Transform(context->state, &input[i]);
        }
        index = 0;
    } else {
        i = 0;
    }
    memcpy(&context->buffer[index], &input[i], inputLen - i);
}

void MD5Final(uint8_t digest[16], MD5Context* context) {
    static const uint8_t PADDING[64] = {
        0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0,    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0,    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0,    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    uint8_t bits[8];
    for (int i = 0; i < 4; i++) {
        bits[i] = (uint8_t)((context->count[0] >> (i * 8)) & 0xFF);
        bits[i + 4] = (uint8_t)((context->count[1] >> (i * 8)) & 0xFF);
    }

    size_t index = (size_t)((context->count[0] >> 3) & 0x3F);
    size_t padLen = (index < 56) ? (56 - index) : (120 - index);
    MD5Update(context, PADDING, padLen);
    MD5Update(context, bits, 8);

    for (int i = 0; i < 4; i++) {
        digest[i]      = (uint8_t)((context->state[0] >> (i * 8)) & 0xFF);
        digest[i + 4]  = (uint8_t)((context->state[1] >> (i * 8)) & 0xFF);
        digest[i + 8]  = (uint8_t)((context->state[2] >> (i * 8)) & 0xFF);
        digest[i + 12] = (uint8_t)((context->state[3] >> (i * 8)) & 0xFF);
    }

    // Secure wipe context
    memset(context, 0, sizeof(MD5Context));
}

std::string ComputeMD5Hex(const std::string& input) {
    MD5Context context;
    MD5Init(&context);
    MD5Update(&context, reinterpret_cast<const uint8_t*>(input.data()), input.size());
    uint8_t digest[16];
    MD5Final(digest, &context);

    char hex[33];
    for (int i = 0; i < 16; i++) {
        snprintf(&(hex[i * 2]), 3, "%02x", digest[i]);
    }
    hex[32] = '\0';
    return std::string(hex);
}

// Helper: Convert jstring safely to std::string
std::string JStringToString(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    if (!chars) return "";
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

// XOR mask for shielding sensitive endpoint paths in binary
static const uint8_t XOR_KEY = 0x5D;

// De-obfuscate XOR byte arrays at runtime
std::string UnmaskString(const uint8_t* masked, size_t len) {
    std::string result;
    result.reserve(len);
    for (size_t i = 0; i < len; ++i) {
        result.push_back(static_cast<char>(masked[i] ^ XOR_KEY));
    }
    return result;
}

} // namespace

extern "C" {

/**
 * Native VCode Calculation.
 * Computes MD5(amount + merchantId + orderId + vkey [+ currency])
 * with memory zeroing of temporary buffers.
 */
JNIEXPORT jstring JNICALL
Java_com_fiuu_xdk_security_NativeSecurity_calculateVCodeNative(
        JNIEnv* env,
        jclass /* clazz */,
        jstring jAmount,
        jstring jMerchantId,
        jstring jOrderId,
        jstring jVKey,
        jstring jCurrency,
        jboolean jExtendedVCode) {

    std::string amount = JStringToString(env, jAmount);
    std::string merchantId = JStringToString(env, jMerchantId);
    std::string orderId = JStringToString(env, jOrderId);
    std::string vkey = JStringToString(env, jVKey);
    std::string currency = JStringToString(env, jCurrency);

    std::string buffer;
    if (jExtendedVCode) {
        buffer = amount + merchantId + orderId + vkey + currency;
    } else {
        buffer = amount + merchantId + orderId + vkey;
    }

    std::string hashHex = ComputeMD5Hex(buffer);

    // Explicitly wipe the buffer holding the raw vkey
    if (!buffer.empty()) {
        memset(&(buffer[0]), 0, buffer.size());
    }
    if (!vkey.empty()) {
        memset(&(vkey[0]), 0, vkey.size());
    }

    return env->NewStringUTF(hashHex.c_str());
}

/**
 * Native SKey Calculation.
 * Computes MD5(txnID + merchantId + vkey + amount)
 */
JNIEXPORT jstring JNICALL
Java_com_fiuu_xdk_security_NativeSecurity_calculateSKeyNative(
        JNIEnv* env,
        jclass /* clazz */,
        jstring jTxnID,
        jstring jMerchantId,
        jstring jVKey,
        jstring jAmount) {

    std::string txnID = JStringToString(env, jTxnID);
    std::string merchantId = JStringToString(env, jMerchantId);
    std::string vkey = JStringToString(env, jVKey);
    std::string amount = JStringToString(env, jAmount);

    std::string buffer = txnID + merchantId + vkey + amount;
    std::string hashHex = ComputeMD5Hex(buffer);

    if (!buffer.empty()) {
        memset(&(buffer[0]), 0, buffer.size());
    }
    if (!vkey.empty()) {
        memset(&(vkey[0]), 0, vkey.size());
    }

    return env->NewStringUTF(hashHex.c_str());
}

/**
 * Retrieves shielded endpoint path strings without storing them
 * as plain text in the DEX string pool.
 */
JNIEXPORT jstring JNICALL
Java_com_fiuu_xdk_security_NativeSecurity_getShieldedPathNative(
        JNIEnv* env,
        jclass /* clazz */,
        jint pathId) {

    // Masked with XOR_KEY 0x5D
    // "RMS/GooglePay/cancel.php"
    static const uint8_t MASKED_CANCEL[] = {
        0x0F, 0x10, 0x0E, 0x72, 0x1A, 0x32, 0x32, 0x3A, 0x31, 0x38, 0x0D, 0x3C, 0x24, 0x72, 0x3E, 0x3C, 0x33, 0x3E, 0x38, 0x31, 0x73, 0x2D, 0x35, 0x2D
    };
    // "RMS/GooglePay/createTxn.php"
    static const uint8_t MASKED_CREATE_TXN[] = {
        0x0F, 0x10, 0x0E, 0x72, 0x1A, 0x32, 0x32, 0x3A, 0x31, 0x38, 0x0D, 0x3C, 0x24, 0x72, 0x3E, 0x2F, 0x38, 0x3C, 0x29, 0x38, 0x09, 0x25, 0x33, 0x73, 0x2D, 0x35, 0x2D
    };
    // "RMS/GooglePay/payment_v2.php"
    static const uint8_t MASKED_PAYMENT_V2[] = {
        0x0F, 0x10, 0x0E, 0x72, 0x1A, 0x32, 0x32, 0x3A, 0x31, 0x38, 0x0D, 0x3C, 0x24, 0x72, 0x2D, 0x3C, 0x24, 0x30, 0x38, 0x33, 0x29, 0x02, 0x2B, 0x6F, 0x73, 0x2D, 0x35, 0x2D
    };
    // "RMS/q_by_tid.php"
    static const uint8_t MASKED_QUERY_TID[] = {
        0x0F, 0x10, 0x0E, 0x72, 0x2C, 0x02, 0x3F, 0x24, 0x02, 0x29, 0x34, 0x39, 0x73, 0x2D, 0x35, 0x2D
    };
    // "RMS/intermediate_app/loading.php"
    static const uint8_t MASKED_LOADING[] = {
        0x0F, 0x10, 0x0E, 0x72, 0x34, 0x33, 0x29, 0x38, 0x2F, 0x30, 0x38, 0x39, 0x34, 0x3C, 0x29, 0x38, 0x02, 0x3C, 0x2D, 0x2D, 0x72, 0x31, 0x32, 0x3C, 0x39, 0x34, 0x33, 0x3A, 0x73, 0x2D, 0x35, 0x2D
    };

    std::string path;
    switch (pathId) {
        case 0: path = UnmaskString(MASKED_CANCEL, sizeof(MASKED_CANCEL)); break;
        case 1: path = UnmaskString(MASKED_CREATE_TXN, sizeof(MASKED_CREATE_TXN)); break;
        case 2: path = UnmaskString(MASKED_PAYMENT_V2, sizeof(MASKED_PAYMENT_V2)); break;
        case 3: path = UnmaskString(MASKED_QUERY_TID, sizeof(MASKED_QUERY_TID)); break;
        case 4: path = UnmaskString(MASKED_LOADING, sizeof(MASKED_LOADING)); break;
        default: path = ""; break;
    }

    return env->NewStringUTF(path.c_str());
}

} // extern "C"
