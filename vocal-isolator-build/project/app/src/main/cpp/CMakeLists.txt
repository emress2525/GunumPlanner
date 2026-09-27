cmake_minimum_required(VERSION 3.22.1)
project(vocal_isolator_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

include(FetchContent)
FetchContent_Declare(
    demucscpp
    GIT_REPOSITORY https://github.com/sevagh/demucs.cpp.git
    GIT_TAG v0.0.4-alpha
    GIT_SHALLOW TRUE
    GIT_SUBMODULES_RECURSE TRUE
)
FetchContent_GetProperties(demucscpp)
if(NOT demucscpp_POPULATED)
    FetchContent_Populate(demucscpp)
endif()

set(DEMUCS_ROOT "${demucscpp_SOURCE_DIR}")
file(GLOB DEMUCS_SOURCES CONFIGURE_DEPENDS "${DEMUCS_ROOT}/src/*.cpp")

add_library(demucs_jni SHARED
    demucs_jni.cpp
    ${DEMUCS_SOURCES}
)

target_include_directories(demucs_jni PRIVATE
    "${DEMUCS_ROOT}/src"
    "${DEMUCS_ROOT}/vendor/eigen"
)

target_compile_definitions(demucs_jni PRIVATE
    EIGEN_MPL2_ONLY
)

target_compile_options(demucs_jni PRIVATE
    -O3
    -DNDEBUG
    -Wall
    -Wextra
    -fexceptions
    -fopenmp
)

target_link_options(demucs_jni PRIVATE
    -fopenmp
    -static-openmp
)

find_library(LOG_LIB log)
find_library(ANDROID_LIB android)
target_link_libraries(demucs_jni PRIVATE ${LOG_LIB} ${ANDROID_LIB})
