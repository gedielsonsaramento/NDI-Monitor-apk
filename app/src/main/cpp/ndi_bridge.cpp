#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#if NDI_ENABLED
#include "Processing.NDI.Lib.h"
#endif

#define LOG_TAG "NDIMonitorNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
JavaVM* g_vm = nullptr;
jobject g_callback = nullptr;
std::mutex g_callback_mutex;

ANativeWindow* g_window = nullptr;
std::mutex g_window_mutex;

std::thread g_capture_thread;
std::atomic<bool> g_running{false};

JNIEnv* get_env(bool& attached) {
    attached = false;
    if (!g_vm) return nullptr;
    JNIEnv* env = nullptr;
    const jint result = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return nullptr;
        attached = true;
    }
    return env;
}

void call_status(const std::string& message) {
    bool attached = false;
    JNIEnv* env = get_env(attached);
    if (!env) return;

    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (g_callback) {
        jclass cls = env->GetObjectClass(g_callback);
        jmethodID method = env->GetMethodID(cls, "onNativeStatus", "(Ljava/lang/String;)V");
        if (method) {
            jstring text = env->NewStringUTF(message.c_str());
            env->CallVoidMethod(g_callback, method, text);
            env->DeleteLocalRef(text);
        }
        env->DeleteLocalRef(cls);
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    if (attached) g_vm->DetachCurrentThread();
}

void call_video_format(int width, int height, double fps) {
    bool attached = false;
    JNIEnv* env = get_env(attached);
    if (!env) return;

    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (g_callback) {
        jclass cls = env->GetObjectClass(g_callback);
        jmethodID method = env->GetMethodID(cls, "onNativeVideoFormat", "(IID)V");
        if (method) env->CallVoidMethod(g_callback, method, width, height, fps);
        env->DeleteLocalRef(cls);
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    if (attached) g_vm->DetachCurrentThread();
}

void call_audio(const std::vector<int16_t>& pcm, int sample_rate, int channels) {
    if (pcm.empty()) return;
    bool attached = false;
    JNIEnv* env = get_env(attached);
    if (!env) return;

    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (g_callback) {
        jclass cls = env->GetObjectClass(g_callback);
        jmethodID method = env->GetMethodID(cls, "onNativeAudio", "([SII)V");
        if (method) {
            jshortArray data = env->NewShortArray(static_cast<jsize>(pcm.size()));
            env->SetShortArrayRegion(data, 0, static_cast<jsize>(pcm.size()),
                                     reinterpret_cast<const jshort*>(pcm.data()));
            env->CallVoidMethod(g_callback, method, data, sample_rate, channels);
            env->DeleteLocalRef(data);
        }
        env->DeleteLocalRef(cls);
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    if (attached) g_vm->DetachCurrentThread();
}

void stop_capture() {
    g_running.store(false);
    if (g_capture_thread.joinable()) g_capture_thread.join();
}

#if NDI_ENABLED
bool ensure_ndi() {
    static std::once_flag once;
    static bool ready = false;
    std::call_once(once, [] { ready = NDIlib_initialize(); });
    return ready;
}

void render_frame(const NDIlib_video_frame_v2_t& frame) {
    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (!g_window || !frame.p_data || frame.xres <= 0 || frame.yres <= 0) return;

    ANativeWindow_setBuffersGeometry(g_window, frame.xres, frame.yres, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer buffer{};
    if (ANativeWindow_lock(g_window, &buffer, nullptr) != 0) return;

    const int rows = std::min(frame.yres, buffer.height);
    const int bytes_per_row = std::min(frame.xres, buffer.width) * 4;
    auto* destination = static_cast<uint8_t*>(buffer.bits);
    for (int y = 0; y < rows; ++y) {
        std::memcpy(destination + y * buffer.stride * 4,
                    frame.p_data + y * frame.line_stride_in_bytes,
                    bytes_per_row);
    }
    ANativeWindow_unlockAndPost(g_window);
}

void deliver_audio(const NDIlib_audio_frame_v3_t& frame) {
    if (!frame.p_data || frame.no_channels <= 0 || frame.no_samples <= 0) return;
    const int output_channels = std::min(frame.no_channels, 2);
    std::vector<int16_t> pcm(static_cast<size_t>(frame.no_samples) * output_channels);
    const auto* base = reinterpret_cast<const uint8_t*>(frame.p_data);

    for (int sample = 0; sample < frame.no_samples; ++sample) {
        for (int channel = 0; channel < output_channels; ++channel) {
            const auto* channel_data = reinterpret_cast<const float*>(
                base + static_cast<size_t>(channel) * frame.channel_stride_in_bytes);
            const float value = std::max(-1.0f, std::min(1.0f, channel_data[sample]));
            pcm[static_cast<size_t>(sample) * output_channels + channel] =
                static_cast<int16_t>(std::lround(value * 32767.0f));
        }
    }
    call_audio(pcm, frame.sample_rate, output_channels);
}

void capture_loop(std::string source_name) {
    if (!ensure_ndi()) {
        call_status("Falha ao inicializar o NDI SDK");
        return;
    }

    NDIlib_recv_create_v3_t settings{};
    settings.source_to_connect_to.p_ndi_name = source_name.c_str();
    settings.source_to_connect_to.p_url_address = nullptr;
    settings.color_format = NDIlib_recv_color_format_RGBX_RGBA;
    settings.bandwidth = NDIlib_recv_bandwidth_highest;
    settings.allow_video_fields = false;
    settings.p_ndi_recv_name = "NDI Monitor Android";

    NDIlib_recv_instance_t receiver = NDIlib_recv_create_v3(&settings);
    if (!receiver) {
        call_status("Nao foi possivel criar o receptor NDI");
        return;
    }

    call_status("Conectando a " + source_name + "...");
    int last_width = 0;
    int last_height = 0;
    int empty_captures = 0;

    while (g_running.load()) {
        NDIlib_video_frame_v2_t video{};
        NDIlib_audio_frame_v3_t audio{};
        NDIlib_metadata_frame_t metadata{};

        const NDIlib_frame_type_e type =
            NDIlib_recv_capture_v3(receiver, &video, &audio, &metadata, 1000);

        switch (type) {
            case NDIlib_frame_type_video: {
                empty_captures = 0;
                render_frame(video);
                if (video.xres != last_width || video.yres != last_height) {
                    last_width = video.xres;
                    last_height = video.yres;
                    const double fps = video.frame_rate_D
                        ? static_cast<double>(video.frame_rate_N) / video.frame_rate_D : 0.0;
                    call_video_format(video.xres, video.yres, fps);
                    call_status("Recebendo " + source_name);
                }
                NDIlib_recv_free_video_v2(receiver, &video);
                break;
            }
            case NDIlib_frame_type_audio:
                empty_captures = 0;
                deliver_audio(audio);
                NDIlib_recv_free_audio_v3(receiver, &audio);
                break;
            case NDIlib_frame_type_metadata:
                NDIlib_recv_free_metadata(receiver, &metadata);
                break;
            case NDIlib_frame_type_none:
                if (++empty_captures == 5) {
                    call_status("Sem sinal. Aguardando reconexao de " + source_name + "...");
                }
                break;
            case NDIlib_frame_type_error:
                call_status("Erro no sinal NDI. Tentando reconectar...");
                std::this_thread::sleep_for(std::chrono::milliseconds(500));
                break;
            default:
                break;
        }
    }

    NDIlib_recv_destroy(receiver);
}
#endif
} // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_br_com_polegar_ndimonitor_NativeBridge_isNdiAvailable(JNIEnv*, jclass) {
#if NDI_ENABLED
    return ensure_ndi() ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_br_com_polegar_ndimonitor_NativeBridge_findSources(JNIEnv* env, jclass, jint timeout_ms) {
    jclass string_class = env->FindClass("java/lang/String");
#if NDI_ENABLED
    if (!ensure_ndi()) return env->NewObjectArray(0, string_class, nullptr);
    NDIlib_find_create_t config{};
    config.show_local_sources = true;
    config.p_groups = nullptr;
    config.p_extra_ips = nullptr;
    NDIlib_find_instance_t finder = NDIlib_find_create_v2(&config);
    if (!finder) return env->NewObjectArray(0, string_class, nullptr);

    const int wait_time = std::max(500, static_cast<int>(timeout_ms));
    NDIlib_find_wait_for_sources(finder, static_cast<uint32_t>(wait_time));
    uint32_t count = 0;
    const NDIlib_source_t* sources = NDIlib_find_get_current_sources(finder, &count);
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(count), string_class, nullptr);
    for (uint32_t i = 0; i < count; ++i) {
        jstring name = env->NewStringUTF(sources[i].p_ndi_name ? sources[i].p_ndi_name : "");
        env->SetObjectArrayElement(result, static_cast<jsize>(i), name);
        env->DeleteLocalRef(name);
    }
    NDIlib_find_destroy(finder);
    return result;
#else
    return env->NewObjectArray(0, string_class, nullptr);
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_br_com_polegar_ndimonitor_NativeBridge_connect(JNIEnv* env, jclass, jstring source) {
    const char* value = env->GetStringUTFChars(source, nullptr);
    const std::string source_name = value ? value : "";
    env->ReleaseStringUTFChars(source, value);
    stop_capture();
#if NDI_ENABLED
    if (!source_name.empty()) {
        g_running.store(true);
        g_capture_thread = std::thread(capture_loop, source_name);
    }
#else
    call_status("SDK NDI ainda nao foi instalado no projeto");
#endif
}

extern "C" JNIEXPORT void JNICALL
Java_br_com_polegar_ndimonitor_NativeBridge_disconnect(JNIEnv*, jclass) {
    stop_capture();
}

extern "C" JNIEXPORT void JNICALL
Java_br_com_polegar_ndimonitor_NativeBridge_setSurface(JNIEnv* env, jclass, jobject surface) {
    std::lock_guard<std::mutex> lock(g_window_mutex);
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
    if (surface) g_window = ANativeWindow_fromSurface(env, surface);
}

extern "C" JNIEXPORT void JNICALL
Java_br_com_polegar_ndimonitor_NativeBridge_setCallbackTarget(JNIEnv* env, jclass, jobject target) {
    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (g_callback) env->DeleteGlobalRef(g_callback);
    g_callback = target ? env->NewGlobalRef(target) : nullptr;
}
