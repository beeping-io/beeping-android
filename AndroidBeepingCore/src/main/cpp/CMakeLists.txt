cmake_minimum_required(VERSION 3.22)
project(beeping_jni LANGUAGES CXX)

# BEE-2226: JNI shim bridging Java_com_beeping_AndroidBeepingCore_BeepingCoreJNI_*
# externs to the beeping-core C API. The prebuilt libbeepingcore.so is consumed
# from the per-ABI artifacts pulled by the Gradle `downloadBeepingCore` task
# (registered in :AndroidBeepingCore:build.gradle.kts).
#
# Required -D variables (passed by externalNativeBuild.cmake.arguments):
#   BEEPING_CORE_INCLUDE_DIR  — beeping-core headers (BeepingCoreLib_api.h, ...)
#   BEEPING_CORE_LIB_DIR      — root of build/intermediates/beeping-core/
#                                with <abi>/libbeepingcore.so subdirs

if(NOT DEFINED BEEPING_CORE_INCLUDE_DIR)
    message(FATAL_ERROR "BEEPING_CORE_INCLUDE_DIR must be set (see build.gradle.kts)")
endif()
if(NOT DEFINED BEEPING_CORE_LIB_DIR)
    message(FATAL_ERROR "BEEPING_CORE_LIB_DIR must be set (see build.gradle.kts)")
endif()

set(BEEPING_CORE_SO "${BEEPING_CORE_LIB_DIR}/${ANDROID_ABI}/libbeepingcore.so")
if(NOT EXISTS "${BEEPING_CORE_SO}")
    message(FATAL_ERROR
        "libbeepingcore.so not found at ${BEEPING_CORE_SO}. "
        "Did the :AndroidBeepingCore:downloadBeepingCore task run?")
endif()

# Imported prebuilt — AGP will not re-package it from here (the .so is already
# wired via jniLibs.srcDirs in build.gradle.kts); this import is only so we
# can link against it at build time.
add_library(beepingcore SHARED IMPORTED)
set_target_properties(beepingcore PROPERTIES
    IMPORTED_LOCATION "${BEEPING_CORE_SO}"
)

add_library(beeping_jni SHARED beeping_jni.cpp)

target_include_directories(beeping_jni PRIVATE
    "${BEEPING_CORE_INCLUDE_DIR}"
)

target_link_libraries(beeping_jni PRIVATE
    beepingcore
    log
)

# 16 KB page size compliance (Android 15+ Play Store policy). Same flag the
# upstream beeping-core release build uses (BEE-2221).
target_link_options(beeping_jni PRIVATE "-Wl,-z,max-page-size=16384")
