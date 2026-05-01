import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.android.junit5)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.beeping.AndroidBeepingCore"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
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
        unitTests.isIncludeAndroidResources = true
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
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotest.property)
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

    configOptions.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization",
            "useCoroutines" to "true",
            "omitGradleWrapper" to "true",
            "omitGradlePluginVersions" to "true",
        ),
    )

    // Remap binary types from `java.io.File` (default) to `kotlin.ByteArray`.
    // The /v1/encode endpoint returns audio/wav as binary — on Android we want
    // in-memory bytes, not a File reference. This avoids JVM-only File dependency.
    typeMappings.set(
        mapOf(
            "file" to "kotlin.ByteArray",
            "binary" to "kotlin.ByteArray",
        ),
    )

    globalProperties.set(
        mapOf(
            "modelDocs" to "false",
            "apiDocs" to "false",
            "modelTests" to "false",
            "apiTests" to "false",
        ),
    )

    // Don't generate Gradle scaffolding (build.gradle.kts, gradle.properties,
    // settings.gradle.kts) inside the output dir — we own the build setup.
    skipOverwrite.set(false)
}

android.libraryVariants.configureEach {
    val openApiGenerate = tasks.named("openApiGenerate")
    javaCompileProvider.configure { dependsOn(openApiGenerate) }
}
tasks
    .matching { it.name.startsWith("compileDebugKotlin") || it.name.startsWith("compileReleaseKotlin") }
    .configureEach { dependsOn("openApiGenerate") }

android.sourceSets.named("main") {
    java.srcDir(openApiOutputDir.map { it.asFile.resolve("src/main/kotlin") })
}

// ── BEE-63: Android Lint strict ──────────────────────────────────────────────
// `warningsAsErrors` + `abortOnError` make ANY new lint warning fail the build.
// We exclude the OpenAPI generated client (third-party code) and target the
// release variant for the strictest checks.
android {
    lint {
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = false
        lintConfig = file("lint.xml")
        // Lint already skips `build/` paths; the OpenAPI sources land there.
    }
}

// ── BEE-62: Kover (coverage) ─────────────────────────────────────────────────
// Kover replaces JaCoCo for Kotlin projects: native support for inline funcs,
// coroutines, sealed classes. Threshold starts at 70% lines (target 90%).
// Generated `internal/api/**` (OpenAPI client) is excluded — third-party code.
//
// Pitest (mutation testing) deferred to a follow-up task — info.solidsoft.pitest
// requires a JVM module, doesn't compose with com.android.library directly.
// Tracked in `docs/PENDING.md` (pending-008).
kover {
    reports {
        filters {
            excludes {
                packages(
                    "com.beeping.AndroidBeepingCore.internal.api",
                    "com.beeping.AndroidBeepingCore.internal.api.apis",
                    "com.beeping.AndroidBeepingCore.internal.api.models",
                    "com.beeping.AndroidBeepingCore.internal.api.infrastructure",
                    "com.beeping.AndroidBeepingCore.internal.api.auth",
                )
            }
        }
        verify {
            rule("Line coverage ≥ 70%") {
                minBound(70)
            }
        }
    }
}

// ── BEE-63: ktlint (formatting) + detekt (code smells) ───────────────────────
// ktlint enforces the Kotlin coding style. Generated OpenAPI code lives in
// `build/generated/**` which is already filtered by the plugin.
ktlint {
    version.set("1.4.1")
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
    filter {
        exclude("**/generated/**")
        exclude { it.file.path.contains("/build/generated/") }
    }
}

// detekt analyses Kotlin sources for code smells, complexity, naming, etc.
// Excludes the generated OpenAPI client (third-party code).
detekt {
    config.setFrom(rootProject.layout.projectDirectory.file("detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
    parallel = true
    source.setFrom(files("src/main/java", "src/test/java"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    dependsOn("openApiGenerate")
    exclude("**/generated/**")
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

// ktlint scans the main source set, which includes the generated OpenAPI dir.
// Declare the implicit dependency so Gradle's task graph stays correct when
// ktlint runs in parallel with `openApiGenerate`. The actual generated files
// are excluded from the lint via the ktlint `filter` block above.
tasks
    .matching {
        it.name.startsWith("runKtlintCheckOver") ||
            it.name.startsWith("runKtlintFormatOver")
    }.configureEach {
        dependsOn("openApiGenerate")
    }
