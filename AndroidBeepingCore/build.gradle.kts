import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.beeping.AndroidBeepingCore"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // No targetSdk on library modules — only applications carry that.

        ndk {
            // Drop legacy ABIs (mips, mips64, armeabi, x86 deprecated since NDK r17).
            // The vendored .so files for those will be removed in BEE-55.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Required for 16 KB page size compliance (Android 15+ Play Store policy).
        // Vendored .so files do NOT comply yet — that's deferred to BEE-65 when
        // we consume signed releases from beeping-core. The Gradle config is
        // however ready.
        jniLibs {
            useLegacyPackaging = false
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // Align transitive kotlin-stdlib variants (avoids duplicate-class errors
    // between kotlin-stdlib 1.8.x and stdlib-jdk7/jdk8 1.6.x).
    implementation(platform(libs.kotlin.bom))

    implementation(libs.bundles.androidx.base)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
