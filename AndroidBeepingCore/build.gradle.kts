import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
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
    // Foojay (declared in settings.gradle.kts) downloads JDK 17 automatically
    // when the local environment doesn't have one — guarantees deterministic
    // toolchain across machines and CI without per-env JDK setup.
    jvmToolchain(17)

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
    implementation(libs.bundles.ktor.client)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
}

// Pass through env vars used for opt-in real E2E tests against beepbox-server.
// Only the (gitignored) `.env.local` value reaches CI/local test runs — never
// committed. CI without these env vars falls back to MockEngine-only tests.
android.testOptions.unitTests.all {
    it.environment("BEEPBOX_API_KEY", System.getenv("BEEPBOX_API_KEY") ?: "")
    it.environment("BEEPBOX_BASE_URL", System.getenv("BEEPBOX_BASE_URL") ?: "")
}

// ── BEE-59: generate the typed beepbox HTTP client from api/openapi.yaml ────
// Uses openapi-generator with `kotlin` + `jvm-ktor` library + kotlinx-serialization.
// The generated sources land in build/generated/openapi/ (gitignored) and are
// added to the main source set so they compile alongside our hand-written code.
//
// Re-vendor flow (when beepbox/docs/openapi.yaml changes upstream):
//   cp ../beepbox/docs/openapi.yaml api/openapi.yaml
//   ./gradlew :AndroidBeepingCore:openApiGenerate
//   git add api/openapi.yaml && commit
val openApiOutputDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-ktor")
    inputSpec.set("$rootDir/api/openapi.yaml")
    outputDir.set(openApiOutputDir.map { it.asFile.path })
    apiPackage.set("com.beeping.AndroidBeepingCore.internal.api.apis")
    modelPackage.set("com.beeping.AndroidBeepingCore.internal.api.models")
    invokerPackage.set("com.beeping.AndroidBeepingCore.internal.api.infrastructure")
    packageName.set("com.beeping.AndroidBeepingCore.internal.api")

    configOptions.set(mapOf(
        "serializationLibrary" to "kotlinx_serialization",
        "useCoroutines" to "true",
        "omitGradleWrapper" to "true",
        "omitGradlePluginVersions" to "true",
    ))

    // Remap binary types from `java.io.File` (default) to `kotlin.ByteArray`.
    // The /v1/encode endpoint returns audio/wav as binary — on Android we want
    // in-memory bytes, not a File reference. This avoids JVM-only File dependency.
    typeMappings.set(mapOf(
        "file" to "kotlin.ByteArray",
        "binary" to "kotlin.ByteArray",
    ))

    globalProperties.set(mapOf(
        "modelDocs" to "false",
        "apiDocs" to "false",
        "modelTests" to "false",
        "apiTests" to "false",
    ))

    // Don't generate Gradle scaffolding (build.gradle.kts, gradle.properties,
    // settings.gradle.kts) inside the output dir — we own the build setup.
    skipOverwrite.set(false)
}

android.libraryVariants.configureEach {
    val openApiGenerate = tasks.named("openApiGenerate")
    javaCompileProvider.configure { dependsOn(openApiGenerate) }
}
tasks.matching { it.name.startsWith("compileDebugKotlin") || it.name.startsWith("compileReleaseKotlin") }
    .configureEach { dependsOn("openApiGenerate") }

android.sourceSets.named("main") {
    java.srcDir(openApiOutputDir.map { it.asFile.resolve("src/main/kotlin") })
}
