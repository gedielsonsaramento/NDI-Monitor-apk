#include "ndi_source_finder.h"
#include <cassert>
#include <iostream>

static int creates = 0, destroys = 0;
static bool fail_create = false;
static std::vector<uint32_t> waits;
static std::vector<NDIlib_source_t> snapshot;

NDIlib_find_instance_t NDIlib_find_create_v2(const NDIlib_find_create_t* config) {
    assert(config->show_local_sources);
    ++creates;
    return fail_create ? nullptr : reinterpret_cast<void*>(1);
}
bool NDIlib_find_wait_for_sources(NDIlib_find_instance_t instance, uint32_t timeout) {
    assert(instance);
    waits.push_back(timeout);
    return false;
}
const NDIlib_source_t* NDIlib_find_get_current_sources(NDIlib_find_instance_t instance, uint32_t* count) {
    assert(instance);
    *count = static_cast<uint32_t>(snapshot.size());
    return snapshot.data();
}
void NDIlib_find_destroy(NDIlib_find_instance_t instance) {
    assert(instance);
    ++destroys;
}

int main() {
    {
        NdiSourceFinder finder;
        snapshot = {{"Palco", nullptr}};
        assert(finder.find(0) == std::vector<std::string>{"Palco"});
        assert(waits.empty()); // cached results don't pay a new discovery timeout
        snapshot = {{"Camera", nullptr}, {"Palco", nullptr}, {"Palco", nullptr},
                    {"", nullptr}, {nullptr, nullptr}};
        assert(finder.find(500) == (std::vector<std::string>{"Camera", "Palco"}));
        finder.find(3500);
        assert(creates == 1); // finding the first source must not end discovery
        assert(waits == (std::vector<uint32_t>{500, 500}));
        finder.close();
        finder.close();
        assert(destroys == 1);
        assert(finder.find(-1) == (std::vector<std::string>{"Camera", "Palco"}));
        assert(creates == 2);
        assert(waits.size() == 2);
    }
    assert(destroys == 2);
    {
        NdiSourceFinder finder;
        fail_create = true;
        assert(finder.find(500).empty());
        fail_create = false;
        snapshot.clear();
        assert(finder.find(0).empty()); // a later poll may recover a failed creation
    }
    assert(creates == 4 && destroys == 3);
    std::cout << "Native discovery regression checks passed\n";
}
