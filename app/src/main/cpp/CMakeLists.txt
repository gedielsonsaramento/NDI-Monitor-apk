cmake_minimum_required(VERSION 3.22.1)
project(ndimonitor)

set(NDI_HEADER "${CMAKE_CURRENT_SOURCE_DIR}/include/Processing.NDI.Lib.h")
set(NDI_LIBRARY "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libndi.so")

add_library(ndimonitor SHARED ndi_bridge.cpp)
target_include_directories(ndimonitor PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/include")

find_library(log-lib log)
find_library(android-lib android)

if(EXISTS "${NDI_HEADER}" AND EXISTS "${NDI_LIBRARY}")
    message(STATUS "NDI SDK encontrado para ${ANDROID_ABI}: recepcao real ativada")
    add_library(ndi SHARED IMPORTED)
    set_target_properties(ndi PROPERTIES IMPORTED_LOCATION "${NDI_LIBRARY}")
    target_compile_definitions(ndimonitor PRIVATE NDI_ENABLED=1)
    target_link_libraries(ndimonitor ndi ${android-lib} ${log-lib})
else()
    message(WARNING "NDI SDK ausente para ${ANDROID_ABI}: compilando modo de preparacao")
    target_compile_definitions(ndimonitor PRIVATE NDI_ENABLED=0)
    target_link_libraries(ndimonitor ${android-lib} ${log-lib})
endif()
