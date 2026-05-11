// BEE-2226: JNI shim bridging Kotlin externs in BeepingCoreJNI.kt to the
// beeping-core C API. Same pattern as the iOS Obj-C wrapper sitting on top
// of the portable C API.
//
// Threading: each beeping object handle is owned by a single Kotlin caller
// (LocalEncoder per instance). The shim does NOT synchronize per-handle; the
// Kotlin layer must serialize calls on the same handle.

#include <jni.h>
#include <android/log.h>
#include <sys/stat.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <string>
#include <vector>

#include "BeepingCoreLib_api.h"

#define LOG_TAG "beeping_jni"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// Recommended buffer size for BEEPING_GetDecodedData; matches the C header
// docstring ("recommended size 30").
constexpr int kDecodedBufferSize = 30;

inline void* asPtr(jlong handle) {
    return reinterpret_cast<void*>(handle);
}

}  // namespace

extern "C" {

// BEEPING_Create() internally calls initBeepingLogger() which uses spdlog
// rotating_file_sink with a *relative* path "logs/beeping.log". On Android the
// process cwd is "/" (read-only), so the file open fails and spdlog throws an
// uncaught exception -> SIGABRT. As a workaround we chdir to a writeable
// directory provided by the caller (typically Context.filesDir) and create
// the "logs/" subdir before invoking BEEPING_Create. Tracked upstream as a
// separate task to make initBeepingLogger() Android-aware.
JNIEXPORT jlong JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_create(JNIEnv* env, jobject /*thiz*/,
                                                            jstring workDir) {
    if (workDir != nullptr) {
        const char* path = env->GetStringUTFChars(workDir, nullptr);
        if (path != nullptr) {
            mkdir(path, 0700);  // idempotent — filesDir already exists
            std::string logs = std::string(path) + "/logs";
            if (mkdir(logs.c_str(), 0700) != 0 && errno != EEXIST) {
                LOGW("mkdir %s failed (errno=%d) — BEEPING_Create may abort",
                     logs.c_str(), errno);
            }
            if (chdir(path) != 0) {
                LOGE("chdir %s failed (errno=%d) — BEEPING_Create may abort",
                     path, errno);
            }
            env->ReleaseStringUTFChars(workDir, path);
        }
    }
    void* h = BEEPING_Create();
    if (h == nullptr) {
        LOGE("BEEPING_Create returned null");
        return 0;
    }
    return reinterpret_cast<jlong>(h);
}

JNIEXPORT void JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_destroy(JNIEnv* /*env*/, jobject /*thiz*/,
                                                            jlong handle) {
    if (handle == 0) return;
    BEEPING_Destroy(asPtr(handle));
}

JNIEXPORT jint JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_configure(JNIEnv* /*env*/, jobject /*thiz*/,
                                                              jlong handle, jint mode,
                                                              jfloat samplingRate,
                                                              jint bufferSize) {
    if (handle == 0) return -1;
    return BEEPING_Configure(static_cast<int>(mode), static_cast<float>(samplingRate),
                             static_cast<int32_t>(bufferSize), asPtr(handle));
}

// Encode a UTF-8 payload. `type` follows the C API: 0 = pure tones, 1 = tones
// + R2D2, 2 = melody mode (not exposed here — melody string param is unused).
// Returns the total number of float32 samples generated, or -1 on bad handle.
JNIEXPORT jint JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_encode(JNIEnv* env, jobject /*thiz*/,
                                                           jlong handle, jstring payload,
                                                           jint type) {
    if (handle == 0 || payload == nullptr) return -1;
    const char* utf = env->GetStringUTFChars(payload, nullptr);
    if (utf == nullptr) return -1;
    const jsize len = env->GetStringUTFLength(payload);
    const int32_t total = BEEPING_EncodeDataToAudioBuffer(
            utf, static_cast<int32_t>(len), static_cast<int32_t>(type),
            nullptr, 0, asPtr(handle));
    env->ReleaseStringUTFChars(payload, utf);
    return total;
}

// Drain a chunk of encoded samples into `out`. Call repeatedly until the
// return value is less than out.length — that signals end of buffer.
JNIEXPORT jint JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_readEncodedBuffer(JNIEnv* env, jobject /*thiz*/,
                                                                       jlong handle,
                                                                       jfloatArray out) {
    if (handle == 0 || out == nullptr) return -1;
    const jsize len = env->GetArrayLength(out);
    if (len <= 0) return 0;
    jfloat* buf = env->GetFloatArrayElements(out, nullptr);
    if (buf == nullptr) return -1;
    const int32_t written = BEEPING_GetEncodedAudioBuffer(buf, asPtr(handle));
    env->ReleaseFloatArrayElements(out, buf, 0);  // 0 = copy back + free
    return written;
}

// Feed PCM samples to the decoder. Return codes (from the C API):
//   -1 = no data found
//   -2 = start token detected
//   -3 = complete word decoded (caller should call getDecodedData)
//   >=0 = single token decoded (index)
JNIEXPORT jint JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_decodeBuffer(JNIEnv* env, jobject /*thiz*/,
                                                                  jlong handle, jfloatArray pcm,
                                                                  jint size) {
    if (handle == 0 || pcm == nullptr || size <= 0) return -1;
    const jsize len = env->GetArrayLength(pcm);
    if (size > len) {
        LOGW("decodeBuffer size=%d > array length=%d, clamping", size, len);
        size = len;
    }
    jfloat* buf = env->GetFloatArrayElements(pcm, nullptr);
    if (buf == nullptr) return -1;
    const int32_t state = BEEPING_DecodeAudioBuffer(buf, static_cast<int>(size), asPtr(handle));
    env->ReleaseFloatArrayElements(pcm, buf, JNI_ABORT);  // read-only on Java side
    return state;
}

// Pull the last decoded string. Returns null when:
//   - no data available (C API returns 0)
//   - integrity check failed (C API returns negative)
// Returns a Java String for valid payloads (positive return).
JNIEXPORT jstring JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getDecodedData(JNIEnv* env, jobject /*thiz*/,
                                                                    jlong handle) {
    if (handle == 0) return nullptr;
    char buf[kDecodedBufferSize] = {0};
    const int32_t res = BEEPING_GetDecodedData(buf, asPtr(handle));
    if (res <= 0) return nullptr;
    // res holds the length on valid data; NUL-terminate defensively.
    const int32_t safeLen = (res < kDecodedBufferSize) ? res : kDecodedBufferSize - 1;
    buf[safeLen] = '\0';
    return env->NewStringUTF(buf);
}

// Confidence + frequency-range accessors — surfaced as float-returning calls.
// The Kotlin side may or may not expose these to consumers yet.

JNIEXPORT jfloat JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getConfidence(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                   jlong handle) {
    if (handle == 0) return 0.0f;
    return BEEPING_GetConfidence(asPtr(handle));
}

}  // extern "C"
