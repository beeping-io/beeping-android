import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Same `.env.local` parser as :AndroidBeepingCore — kept duplicated (KISS)
// because Gradle composite-builds / convention plugins would be overkill for
// two readers. Forwarded into BuildConfig so the sample's runtime UI can
// switch between DEV and PROD without touching code.
val dotenvLocal: Map<String, String> =
    rootProject
        .file(".env.local")
        .takeIf { it.exists() }
        ?.let { file ->
            file
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
                .associate { line ->
                    val (k, v) = line.split('=', limit = 2)
                    k.trim() to v.trim().trim('"').trim('\'')
                }
        }.orEmpty()

fun envOr(
    name: String,
    fallback: String = "",
): String = dotenvLocal[name] ?: System.getenv(name) ?: fallback

android {
    namespace = "com.beeping.sample"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "com.beeping.sample"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"

        // BEE-64: BuildConfig fields seeded from .env.local. The sample's UI
        // exposes a DEV/PROD switcher that picks the matching pair at runtime.
        buildConfigField("String", "BEEPBOX_DEV_BASE_URL", "\"${envOr("BEEPBOX_DEV_BASE_URL")}\"")
        buildConfigField("String", "BEEPBOX_DEV_API_KEY", "\"${envOr("BEEPBOX_DEV_API_KEY")}\"")
        buildConfigField("String", "BEEPBOX_PROD_BASE_URL", "\"${envOr("BEEPBOX_PROD_BASE_URL")}\"")
        buildConfigField("String", "BEEPBOX_PROD_API_KEY", "\"${envOr("BEEPBOX_PROD_API_KEY")}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
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
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = false
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// BEE-64: ktlint + detekt point at the same root configs as :AndroidBeepingCore
// so the sample app obeys the same code-style + smell rules as the SDK.
ktlint {
    version.set("1.4.1")
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

detekt {
    config.setFrom(rootProject.layout.projectDirectory.file("detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
    parallel = true
    source.setFrom(files("src/main/java"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.compose.bom))

    implementation(project(":AndroidBeepingCore"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.bundles.compose.ui.bundle)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
}
