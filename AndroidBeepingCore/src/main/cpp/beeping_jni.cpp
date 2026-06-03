// BEE-2226: JNI shim bridging Kotlin externs in BeepingCoreJNI.kt to the
// beeping-core C API. Same pattern as the iOS Obj-C wrapper sitting on top
// of the portable C API.
//
// Threading: each beeping object handle is owned by a single Kotlin caller
// (LocalEncoder per instance). The shim does NOT synchronize per-handle; the
// Kotlin layer must serialize calls on the same handle.

#include <jni.h>
#include <android/log.h>
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

JNIEXPORT jlong JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_create(JNIEnv* /*env*/, jobject /*thiz*/) {
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
// Production callers should never see a corrupted payload — silently dropping
// failed-integrity decodes is the safest UX (users would otherwise take
// actions on the wrong code). Diagnostics that need to inspect the raw bytes
// even on integrity failure should call this in a context where they cross-
// check via getConfidence / getConfidenceError.
JNIEXPORT jstring JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getDecodedData(JNIEnv* env, jobject /*thiz*/,
                                                                    jlong handle) {
    if (handle == 0) return nullptr;
    char buf[kDecodedBufferSize] = {0};
    const int32_t res = BEEPING_GetDecodedData(buf, asPtr(handle));
    if (res <= 0) return nullptr;
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

// BEE-2313: reception-quality metrics. Read alongside getConfidence at
// DECODE_COMPLETE to populate ReceptionMetrics on the Kotlin side.

JNIEXPORT jfloat JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getConfidenceError(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                        jlong handle) {
    if (handle == 0) return 0.0f;
    return BEEPING_GetConfidenceError(asPtr(handle));
}

JNIEXPORT jfloat JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getConfidenceNoise(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                        jlong handle) {
    if (handle == 0) return 0.0f;
    return BEEPING_GetConfidenceNoise(asPtr(handle));
}

JNIEXPORT jfloat JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getReceivedBeepsVolume(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                            jlong handle) {
    if (handle == 0) return 0.0f;
    return BEEPING_GetReceivedBeepsVolume(asPtr(handle));
}

JNIEXPORT jint JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getDecodedMode(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                    jlong handle) {
    if (handle == 0) return -1;
    return BEEPING_GetDecodedMode(asPtr(handle));
}

// BEE-2240: scheduler bridges. Mirror the BEE-2238 public C API:
//   computeBeepSchedule — pure utility, no handle, returns timestamps[]
//   encodeWithSchedule  — handle-bound, returns one continuous PCM buffer

// Returns a fresh double[] of timestamps, or null on invalid params.
// (size-query call to BEEPING_ComputeBeepSchedule decides the array length;
// a second call fills it. Both calls cheap — no allocation upstream.)
JNIEXPORT jdoubleArray JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_computeBeepSchedule(
        JNIEnv* env, jobject /*thiz*/,
        jfloat duration, jfloat startTime, jfloat interval) {
    int32_t count = 0;
    int32_t rc = BEEPING_ComputeBeepSchedule(
            duration, startTime, interval,
            nullptr, 0, &count);
    if (rc != 0 || count < 0) {
        LOGW("computeBeepSchedule size-query rc=%d count=%d", rc, count);
        return nullptr;
    }
    jdoubleArray out = env->NewDoubleArray(count);
    if (out == nullptr) return nullptr;
    if (count == 0) return out;
    std::vector<double> tmp(static_cast<size_t>(count));
    rc = BEEPING_ComputeBeepSchedule(
            duration, startTime, interval,
            tmp.data(), count, nullptr);
    if (rc != 0) {
        LOGW("computeBeepSchedule fill rc=%d", rc);
        return nullptr;
    }
    env->SetDoubleArrayRegion(out, 0, count, tmp.data());
    return out;
}

// Returns a fresh float[] of `floor(duration * sampleRate)` samples, or null
// on error. Allocates via BEEPING_GetScheduleBufferSize against the active
// handle (must already be Configure()d).
JNIEXPORT jfloatArray JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_encodeWithSchedule(
        JNIEnv* env, jobject /*thiz*/,
        jlong handle, jstring code, jint type,
        jfloat duration, jfloat startTime, jfloat interval,
        jfloat beepGainDb) {
    if (handle == 0 || code == nullptr) return nullptr;
    const int32_t bufSize = BEEPING_GetScheduleBufferSize(duration, asPtr(handle));
    if (bufSize <= 0) {
        LOGE("encodeWithSchedule: GetScheduleBufferSize returned %d", bufSize);
        return nullptr;
    }
    const char* utf = env->GetStringUTFChars(code, nullptr);
    if (utf == nullptr) return nullptr;
    const jsize codeLen = env->GetStringUTFLength(code);
    std::vector<float> buf(static_cast<size_t>(bufSize), 0.0f);
    int32_t written = 0;
    const int32_t rc = BEEPING_EncodeWithSchedule(
            utf, static_cast<int32_t>(codeLen),
            static_cast<int32_t>(type), nullptr, 0,
            duration, startTime, interval, beepGainDb,
            buf.data(), bufSize, &written, asPtr(handle));
    env->ReleaseStringUTFChars(code, utf);
    if (rc != 0 || written <= 0) {
        LOGE("encodeWithSchedule rc=%d written=%d", rc, written);
        return nullptr;
    }
    jfloatArray out = env->NewFloatArray(written);
    if (out == nullptr) return nullptr;
    env->SetFloatArrayRegion(out, 0, written, buf.data());
    return out;
}

// BEE-2314: scheduled-payload decode. Mirror the BEE-2240 encode side.
//   parseScheduledTimestamp — pure utility (no handle): split a payload string
//     `code + 4-char base-32 timestamp`, returning the timestamp in seconds.
//   getDecodedScheduledPayload — handle-bound: GetDecodedData + split in one
//     step, returning a ScheduledPayload object (or null on no-data/failure).

JNIEXPORT jint JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_parseScheduledTimestamp(
        JNIEnv* env, jobject /*thiz*/, jstring payload) {
    if (payload == nullptr) return -2;
    const char* utf = env->GetStringUTFChars(payload, nullptr);
    if (utf == nullptr) return -2;
    const jsize len = env->GetStringUTFLength(payload);
    int32_t timestampSec = -1;
    // outCode = nullptr → only the timestamp is decoded; rc is 0 or -2.
    const int32_t rc = BEEPING_ParseScheduledPayload(
            utf, static_cast<int32_t>(len), nullptr, 0, nullptr, &timestampSec);
    env->ReleaseStringUTFChars(payload, utf);
    return (rc != 0) ? rc : timestampSec;
}

JNIEXPORT jobject JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getDecodedScheduledPayload(
        JNIEnv* env, jobject /*thiz*/, jlong handle) {
    if (handle == 0) return nullptr;
    char code[kDecodedBufferSize] = {0};
    int32_t codeSize = 0;
    int32_t timestampSec = -1;
    const int32_t rc = BEEPING_GetDecodedScheduledPayload(
            code, kDecodedBufferSize, &codeSize, &timestampSec, asPtr(handle));
    // rc: >0 ok; 0 no data; negative integrity/-10 split failure.
    if (rc <= 0) return nullptr;
    jclass cls = env->FindClass("com/beeping/AndroidBeepingCore/ScheduledPayload");
    if (cls == nullptr) return nullptr;
    jmethodID ctor = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;I)V");
    if (ctor == nullptr) return nullptr;
    jstring jcode = env->NewStringUTF(code);
    return env->NewObject(cls, ctor, jcode, timestampSec);
}

// BEE-2315: diagnostics — core version (no handle) + active decode frequency
// range (handle-bound).

JNIEXPORT jstring JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getVersion(JNIEnv* env, jobject /*thiz*/) {
    const char* version = BEEPING_GetVersion();
    return env->NewStringUTF(version != nullptr ? version : "");
}

JNIEXPORT jstring JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getVersionInfo(JNIEnv* env, jobject /*thiz*/) {
    char info[128] = {0};
    const int32_t n = BEEPING_GetVersionInfo(info);
    if (n <= 0) return env->NewStringUTF("");
    const int32_t safeLen = (n < static_cast<int32_t>(sizeof(info))) ? n : static_cast<int32_t>(sizeof(info)) - 1;
    info[safeLen] = '\0';
    return env->NewStringUTF(info);
}

JNIEXPORT jfloat JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getDecodingBeginFreq(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                          jlong handle) {
    if (handle == 0) return 0.0f;
    return BEEPING_GetDecodingBeginFreq(asPtr(handle));
}

JNIEXPORT jfloat JNICALL
Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_getDecodingEndFreq(JNIEnv* /*env*/, jobject /*thiz*/,
                                                                        jlong handle) {
    if (handle == 0) return 0.0f;
    return BEEPING_GetDecodingEndFreq(asPtr(handle));
}

}  // extern "C"
