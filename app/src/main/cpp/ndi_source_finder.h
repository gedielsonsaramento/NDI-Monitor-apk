#pragma once

#include "Processing.NDI.Lib.h"

#include <algorithm>
#include <mutex>
#include <string>
#include <vector>

// Keep discovery alive between UI updates: mDNS sources arrive incrementally.
class NdiSourceFinder {
public:
    ~NdiSourceFinder() { close(); }

    std::vector<std::string> find(int timeout_ms) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (!finder_) {
            NDIlib_find_create_t config{};
            config.show_local_sources = true;
            finder_ = NDIlib_find_create_v2(&config);
        }
        if (!finder_) return {};

        // A zero timeout returns cached sources immediately. Bound waiting so
        // activity teardown and manual refresh cannot be delayed by a long scan.
        const int wait_ms = std::clamp(timeout_ms, 0, 500);
        if (wait_ms > 0) {
            NDIlib_find_wait_for_sources(finder_, static_cast<uint32_t>(wait_ms));
        }
        uint32_t count = 0;
        const NDIlib_source_t* sources = NDIlib_find_get_current_sources(finder_, &count);
        std::vector<std::string> names;
        if (sources) {
            names.reserve(count);
            for (uint32_t i = 0; i < count; ++i) {
                if (sources[i].p_ndi_name && sources[i].p_ndi_name[0]) {
                    names.emplace_back(sources[i].p_ndi_name);
                }
            }
        }
        std::sort(names.begin(), names.end());
        names.erase(std::unique(names.begin(), names.end()), names.end());
        return names;
    }

    void close() {
        std::lock_guard<std::mutex> lock(mutex_);
        if (finder_) {
            NDIlib_find_destroy(finder_);
            finder_ = nullptr;
        }
    }

private:
    std::mutex mutex_;
    NDIlib_find_instance_t finder_ = nullptr;
};
