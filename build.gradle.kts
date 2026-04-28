// Top-level build file — declare plugins for the whole tree without applying them.
// Each module's build.gradle.kts opts in via `alias(libs.plugins.<id>)`.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}
