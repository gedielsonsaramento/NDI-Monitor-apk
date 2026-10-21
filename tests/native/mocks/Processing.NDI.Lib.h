#pragma once
#include <cstdint>

// Minimal fake of the public discovery interface; no NDI SDK implementation.
using NDIlib_find_instance_t = void*;
struct NDIlib_find_create_t { bool show_local_sources; const char* p_groups; const char* p_extra_ips; };
struct NDIlib_source_t { const char* p_ndi_name; const char* p_url_address; };
NDIlib_find_instance_t NDIlib_find_create_v2(const NDIlib_find_create_t*);
bool NDIlib_find_wait_for_sources(NDIlib_find_instance_t, uint32_t);
const NDIlib_source_t* NDIlib_find_get_current_sources(NDIlib_find_instance_t, uint32_t*);
void NDIlib_find_destroy(NDIlib_find_instance_t);
