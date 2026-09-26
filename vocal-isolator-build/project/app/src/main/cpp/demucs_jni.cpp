#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

#include "model.hpp"

namespace {
constexpr const char *TAG = "VocalIsolatorNative";
constexpr int STATUS_OK = 0;
constexpr int STATUS_MODEL_LOAD_FAILED = 1;
constexpr int STATUS_SEPARATION_FAILED = 2;
constexpr int STATUS_CANCELLED = 3;
constexpr int STATUS_INVALID_WAV = 4;
constexpr int SAMPLE_RATE = 44100;
constexpr int CHANNELS = 2;
constexpr int BITS_PER_SAMPLE = 16;
constexpr int64_t WINDOW_FRAMES = SAMPLE_RATE * 11LL;
constexpr int64_t OVERLAP_FRAMES = WINDOW_FRAMES / 4LL;
constexpr int64_t STEP_FRAMES = WINDOW_FRAMES - OVERLAP_FRAMES;

struct CancelledInference final : std::exception {};

struct Handle {
    std::unique_ptr<demucscpp::demucs_model> model;
    std::atomic_bool cancelled{false};
};

uint16_t readU16(std::istream &in) {
    uint8_t b[2]{};
    in.read(reinterpret_cast<char *>(b), 2);
    return static_cast<uint16_t>(b[0] | (static_cast<uint16_t>(b[1]) << 8));
}

uint32_t readU32(std::istream &in) {
    uint8_t b[4]{};
    in.read(reinterpret_cast<char *>(b), 4);
    return static_cast<uint32_t>(b[0]) |
           (static_cast<uint32_t>(b[1]) << 8) |
           (static_cast<uint32_t>(b[2]) << 16) |
           (static_cast<uint32_t>(b[3]) << 24);
}

void writeU16(std::ostream &out, uint16_t value) {
    const uint8_t b[2] = {
        static_cast<uint8_t>(value & 0xff),
        static_cast<uint8_t>((value >> 8) & 0xff),
    };
    out.write(reinterpret_cast<const char *>(b), 2);
}

void writeU32(std::ostream &out, uint32_t value) {
    const uint8_t b[4] = {
        static_cast<uint8_t>(value & 0xff),
        static_cast<uint8_t>((value >> 8) & 0xff),
        static_cast<uint8_t>((value >> 16) & 0xff),
        static_cast<uint8_t>((value >> 24) & 0xff),
    };
    out.write(reinterpret_cast<const char *>(b), 4);
}

struct WavReader {
    std::ifstream stream;
    int64_t dataOffset = 0;
    int64_t totalFrames = 0;

    explicit WavReader(const std::string &path) : stream(path, std::ios::binary) {
        if (!stream) throw std::runtime_error("open input wav failed");
        char riff[4]{}, wave[4]{};
        stream.read(riff, 4);
        (void) readU32(stream);
        stream.read(wave, 4);
        if (std::memcmp(riff, "RIFF", 4) != 0 || std::memcmp(wave, "WAVE", 4) != 0) {
            throw std::runtime_error("not RIFF/WAVE");
        }

        bool fmtFound = false;
        bool dataFound = false;
        uint16_t audioFormat = 0, channels = 0, bits = 0;
        uint32_t sampleRate = 0;
        uint32_t dataSize = 0;
        while (stream && !(fmtFound && dataFound)) {
            char id[4]{};
            stream.read(id, 4);
            if (stream.gcount() != 4) break;
            const uint32_t size = readU32(stream);
            const auto payload = stream.tellg();
            if (std::memcmp(id, "fmt ", 4) == 0) {
                audioFormat = readU16(stream);
                channels = readU16(stream);
                sampleRate = readU32(stream);
                (void) readU32(stream);
                (void) readU16(stream);
                bits = readU16(stream);
                fmtFound = true;
            } else if (std::memcmp(id, "data", 4) == 0) {
                dataOffset = static_cast<int64_t>(payload);
                dataSize = size;
                dataFound = true;
            }
            stream.seekg(payload + static_cast<std::streamoff>(size + (size & 1u)));
        }
        if (!fmtFound || !dataFound || audioFormat != 1 || channels != CHANNELS ||
            sampleRate != SAMPLE_RATE || bits != BITS_PER_SAMPLE) {
            throw std::runtime_error("expected PCM16 stereo 44100 Hz wav");
        }
        totalFrames = dataSize / (CHANNELS * (BITS_PER_SAMPLE / 8));
    }

    Eigen::MatrixXf readFrames(int64_t startFrame, int64_t count) {
        Eigen::MatrixXf audio(CHANNELS, static_cast<Eigen::Index>(count));
        stream.clear();
        stream.seekg(dataOffset + startFrame * CHANNELS * 2LL);
        for (int64_t i = 0; i < count; ++i) {
            const int16_t left = static_cast<int16_t>(readU16(stream));
            const int16_t right = static_cast<int16_t>(readU16(stream));
            if (!stream) throw std::runtime_error("truncated input wav");
            audio(0, static_cast<Eigen::Index>(i)) = static_cast<float>(left) / 32768.0f;
            audio(1, static_cast<Eigen::Index>(i)) = static_cast<float>(right) / 32768.0f;
        }
        return audio;
    }
};

struct WavWriter {
    std::fstream stream;
    uint64_t framesWritten = 0;

    explicit WavWriter(const std::string &path)
        : stream(path, std::ios::binary | std::ios::out | std::ios::trunc) {
        if (!stream) throw std::runtime_error("open output wav failed");
        char zero[44]{};
        stream.write(zero, sizeof(zero));
    }

    static int16_t pcm(float value) {
        const float clamped = std::max(-1.0f, std::min(1.0f, value));
        const int scaled = static_cast<int>(std::lrint(clamped * 32767.0f));
        return static_cast<int16_t>(std::max(-32768, std::min(32767, scaled)));
    }

    void writeFrame(float left, float right) {
        writeU16(stream, static_cast<uint16_t>(pcm(left)));
        writeU16(stream, static_cast<uint16_t>(pcm(right)));
        ++framesWritten;
    }

    void finish() {
        const uint64_t dataBytes64 = framesWritten * CHANNELS * 2ULL;
        if (dataBytes64 > 0xffffffffULL - 36ULL) throw std::runtime_error("WAV exceeds RIFF size limit");
        const uint32_t dataBytes = static_cast<uint32_t>(dataBytes64);
        stream.seekp(0);
        stream.write("RIFF", 4);
        writeU32(stream, 36u + dataBytes);
        stream.write("WAVE", 4);
        stream.write("fmt ", 4);
        writeU32(stream, 16);
        writeU16(stream, 1);
        writeU16(stream, CHANNELS);
        writeU32(stream, SAMPLE_RATE);
        writeU32(stream, SAMPLE_RATE * CHANNELS * 2);
        writeU16(stream, CHANNELS * 2);
        writeU16(stream, BITS_PER_SAMPLE);
        stream.write("data", 4);
        writeU32(stream, dataBytes);
        stream.flush();
    }

    ~WavWriter() {
        if (stream.is_open()) {
            try { finish(); } catch (...) {}
        }
    }
};

struct StereoTail {
    std::vector<float> left;
    std::vector<float> right;
};

void emitRange(
    const Eigen::Tensor3dXf &targets,
    int64_t from,
    int64_t to,
    WavWriter &vocals,
    WavWriter &instrumental) {
    for (int64_t i = from; i < to; ++i) {
        const auto sample = static_cast<Eigen::Index>(i);
        const float vl = targets(3, 0, sample);
        const float vr = targets(3, 1, sample);
        const float il = targets(0, 0, sample) + targets(1, 0, sample) + targets(2, 0, sample);
        const float ir = targets(0, 1, sample) + targets(1, 1, sample) + targets(2, 1, sample);
        vocals.writeFrame(vl, vr);
        instrumental.writeFrame(il, ir);
    }
}

StereoTail makeTail(const Eigen::Tensor3dXf &targets, int target, int64_t start, int64_t count) {
    StereoTail tail;
    tail.left.resize(static_cast<size_t>(count));
    tail.right.resize(static_cast<size_t>(count));
    for (int64_t i = 0; i < count; ++i) {
        const auto sample = static_cast<Eigen::Index>(start + i);
        if (target == 3) {
            tail.left[static_cast<size_t>(i)] = targets(3, 0, sample);
            tail.right[static_cast<size_t>(i)] = targets(3, 1, sample);
        } else {
            tail.left[static_cast<size_t>(i)] = targets(0, 0, sample) + targets(1, 0, sample) + targets(2, 0, sample);
            tail.right[static_cast<size_t>(i)] = targets(0, 1, sample) + targets(1, 1, sample) + targets(2, 1, sample);
        }
    }
    return tail;
}

void emitCrossfade(
    const StereoTail &previousVocals,
    const StereoTail &previousInstrumental,
    const Eigen::Tensor3dXf &current,
    int64_t count,
    WavWriter &vocals,
    WavWriter &instrumental) {
    const int64_t usable = std::min<int64_t>(count, std::min(previousVocals.left.size(), previousInstrumental.left.size()));
    for (int64_t i = 0; i < usable; ++i) {
        const float alpha = static_cast<float>(i + 1) / static_cast<float>(usable + 1);
        const float inv = 1.0f - alpha;
        const auto sample = static_cast<Eigen::Index>(i);
        const float cvl = current(3, 0, sample);
        const float cvr = current(3, 1, sample);
        const float cil = current(0, 0, sample) + current(1, 0, sample) + current(2, 0, sample);
        const float cir = current(0, 1, sample) + current(1, 1, sample) + current(2, 1, sample);
        vocals.writeFrame(previousVocals.left[static_cast<size_t>(i)] * inv + cvl * alpha,
                          previousVocals.right[static_cast<size_t>(i)] * inv + cvr * alpha);
        instrumental.writeFrame(previousInstrumental.left[static_cast<size_t>(i)] * inv + cil * alpha,
                                previousInstrumental.right[static_cast<size_t>(i)] * inv + cir * alpha);
    }
}

std::string fromJString(JNIEnv *env, jstring value) {
    if (!value) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

void report(JNIEnv *env, jobject callback, jmethodID method, float progress, const std::string &message) {
    if (!callback || !method) return;
    jstring text = env->NewStringUTF(message.c_str());
    env->CallVoidMethod(callback, method, progress, text);
    env->DeleteLocalRef(text);
    if (env->ExceptionCheck()) throw std::runtime_error("progress callback failed");
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_vocalisolator_app_separation_DemucsNativeBridge_nativeCreate(
    JNIEnv *env, jobject, jstring modelPath) {
    try {
        auto handle = std::make_unique<Handle>();
        handle->model = std::make_unique<demucscpp::demucs_model>();
        const std::string path = fromJString(env, modelPath);
        if (path.empty() || !demucscpp::load_demucs_model(path, handle->model.get()) || !handle->model->is_4sources) {
            return 0;
        }
        return reinterpret_cast<jlong>(handle.release());
    } catch (const std::exception &e) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "nativeCreate: %s", e.what());
        return 0;
    } catch (...) {
        return 0;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_vocalisolator_app_separation_DemucsNativeBridge_nativeSeparate(
    JNIEnv *env,
    jobject,
    jlong rawHandle,
    jstring inputWavPath,
    jstring vocalsPath,
    jstring instrumentalPath,
    jobject callback) {
    auto *handle = reinterpret_cast<Handle *>(rawHandle);
    if (!handle || !handle->model) return STATUS_MODEL_LOAD_FAILED;
    handle->cancelled.store(false, std::memory_order_relaxed);
    try {
        const std::string inputPath = fromJString(env, inputWavPath);
        const std::string vocalsOut = fromJString(env, vocalsPath);
        const std::string instrumentalOut = fromJString(env, instrumentalPath);
        WavReader reader(inputPath);
        WavWriter vocals(vocalsOut);
        WavWriter instrumental(instrumentalOut);

        jclass callbackClass = callback ? env->GetObjectClass(callback) : nullptr;
        jmethodID progressMethod = callbackClass
            ? env->GetMethodID(callbackClass, "onProgress", "(FLjava/lang/String;)V")
            : nullptr;
        if (callbackClass) env->DeleteLocalRef(callbackClass);

        const int64_t total = reader.totalFrames;
        const int64_t windowCount = total <= WINDOW_FRAMES
            ? 1
            : 1 + (total - WINDOW_FRAMES + STEP_FRAMES - 1) / STEP_FRAMES;
        StereoTail previousVocals;
        StereoTail previousInstrumental;

        for (int64_t window = 0; window < windowCount; ++window) {
            if (handle->cancelled.load(std::memory_order_relaxed)) throw CancelledInference();
            const int64_t start = window * STEP_FRAMES;
            const int64_t count = std::min<int64_t>(WINDOW_FRAMES, total - start);
            if (count <= 0) break;
            auto audio = reader.readFrames(start, count);
            const float baseProgress = static_cast<float>(window) / static_cast<float>(windowCount);
            const float windowScale = 1.0f / static_cast<float>(windowCount);
            demucscpp::ProgressCallback progress = [&](float inner, const std::string &message) {
                if (handle->cancelled.load(std::memory_order_relaxed)) throw CancelledInference();
                report(env, callback, progressMethod,
                       std::min(0.999f, baseProgress + std::max(0.0f, std::min(1.0f, inner)) * windowScale),
                       message);
            };
            Eigen::Tensor3dXf targets = demucscpp::demucs_inference(*handle->model, audio, progress);
            if (targets.dimension(0) < 4 || targets.dimension(1) != 2 || targets.dimension(2) < count) {
                throw std::runtime_error("unexpected demucs output shape");
            }

            const bool first = window == 0;
            const bool last = window == windowCount - 1;
            if (first && last) {
                emitRange(targets, 0, count, vocals, instrumental);
            } else if (first) {
                const int64_t tailCount = std::min<int64_t>(OVERLAP_FRAMES, count);
                emitRange(targets, 0, count - tailCount, vocals, instrumental);
                previousVocals = makeTail(targets, 3, count - tailCount, tailCount);
                previousInstrumental = makeTail(targets, -1, count - tailCount, tailCount);
            } else {
                const int64_t crossCount = std::min<int64_t>(OVERLAP_FRAMES, count);
                emitCrossfade(previousVocals, previousInstrumental, targets, crossCount, vocals, instrumental);
                if (last) {
                    emitRange(targets, crossCount, count, vocals, instrumental);
                } else {
                    const int64_t tailCount = std::min<int64_t>(OVERLAP_FRAMES, count - crossCount);
                    const int64_t middleEnd = count - tailCount;
                    emitRange(targets, crossCount, middleEnd, vocals, instrumental);
                    previousVocals = makeTail(targets, 3, middleEnd, tailCount);
                    previousInstrumental = makeTail(targets, -1, middleEnd, tailCount);
                }
            }
        }
        vocals.finish();
        instrumental.finish();
        report(env, callback, progressMethod, 1.0f, "Finished");
        return STATUS_OK;
    } catch (const CancelledInference &) {
        return STATUS_CANCELLED;
    } catch (const std::runtime_error &e) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "nativeSeparate: %s", e.what());
        const std::string message = e.what();
        if (message.find("wav") != std::string::npos || message.find("RIFF") != std::string::npos ||
            message.find("PCM16") != std::string::npos) {
            return STATUS_INVALID_WAV;
        }
        return STATUS_SEPARATION_FAILED;
    } catch (const std::exception &e) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "nativeSeparate: %s", e.what());
        return STATUS_SEPARATION_FAILED;
    } catch (...) {
        return STATUS_SEPARATION_FAILED;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_vocalisolator_app_separation_DemucsNativeBridge_nativeCancel(
    JNIEnv *, jobject, jlong rawHandle) {
    auto *handle = reinterpret_cast<Handle *>(rawHandle);
    if (handle) handle->cancelled.store(true, std::memory_order_relaxed);
}

extern "C" JNIEXPORT void JNICALL
Java_com_vocalisolator_app_separation_DemucsNativeBridge_nativeRelease(
    JNIEnv *, jobject, jlong rawHandle) {
    auto *handle = reinterpret_cast<Handle *>(rawHandle);
    delete handle;
}
