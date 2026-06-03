// Settings — top-level project structure + repositories.
// https://docs.gradle.org/current/userguide/declaring_repositories.html

@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Foojay Disco API toolchain resolver — enables `kotlin { jvmToolchain(17) }`
// to download a JDK 17 automatically on any machine/CI that doesn't have one.
// Eliminates the "Cannot find a Java installation matching {languageVersion=17}"
// failure mode without per-env JDK provisioning.
//
// Note: Foojay is a third-party service. Transient outages have been observed.
// If Foojay is unreachable AND the local environment has no JDK 17 installed,
// the build fails. CI is unaffected (uses actions/setup-java). For local
// resilience, install JDK 17 via brew/sdkman/etc. as a fallback.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    // Reject any module-level repositories — all repositories must be declared here.
    // This prevents accidental drift across modules.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "beeping-android"

// :app (example) removed 2026-06-03 — rebuilt as a copy of the beeping_flutter
// example after the Flutter plugin lands (BEE-2336, Phase 10).
include(":AndroidBeepingCore")
