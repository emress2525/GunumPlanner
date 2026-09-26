cmake_minimum_required(VERSION 3.22.1)
project(vocal_isolator_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(DEMUCS_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/demucs_cpp")
if(NOT EXISTS "${DEMUCS_ROOT}/src/model.hpp")
    message(FATAL_ERROR "demucs.cpp sources are missing. Run scripts/vendor_demucs_cpp.sh first.")
endif()

file(GLOB DEMUCS_SOURCES CONFIGURE_DEPENDS "${DEMUCS_ROOT}/src/*.cpp")
add_library(demucs_jni SHARED demucs_jni.cpp ${DEMUCS_SOURCES})

target_include_directories(demucs_jni PRIVATE
    "${DEMUCS_ROOT}/src"
    "${DEMUCS_ROOT}/vendor/eigen"
)

target_compile_definitions(demucs_jni PRIVATE
    EIGEN_DONT_PARALLELIZE
    EIGEN_MPL2_ONLY
)
target_compile_options(demucs_jni PRIVATE -O3 -DNDEBUG -Wall -Wextra -fexceptions)

find_library(LOG_LIB log)
find_library(ANDROID_LIB android)
target_link_libraries(demucs_jni PRIVATE ${LOG_LIB} ${ANDROID_LIB})
