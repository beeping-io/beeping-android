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

include(":AndroidBeepingCore", ":app")
