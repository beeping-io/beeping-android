import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.security.MessageDigest

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

// BEE-65 + BEE-2226: beeping-core consumption + JNI shim. Declared at the top
// because the android {} block below references these paths in CMake arguments.
val beepingCoreVersion = libs.versions.beepingCore.get()
val beepingCoreAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
val beepingCoreOutDir = layout.buildDirectory.dir("intermediates/beeping-core")
val beepingCoreHeadersDir = layout.buildDirectory.dir("intermediates/beeping-core-headers")

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

        // BEE-2226: JNI shim CMake build args. The paths point to the outputs
        // of `downloadBeepingCore`; CMake reads them via -D variables.
        externalNativeBuild {
            cmake {
                cppFlags(
                    "-std=c++17",
                    "-Wall",
                    "-Wextra",
                    "-Werror",
                    "-fvisibility=hidden",
                )
                arguments(
                    "-DBEEPING_CORE_INCLUDE_DIR=" +
                        beepingCoreHeadersDir
                            .get()
                            .asFile.absolutePath +
                        "/include",
                    "-DBEEPING_CORE_LIB_DIR=" +
                        beepingCoreOutDir.get().asFile.absolutePath,
                    "-DANDROID_STL=c++_static",
                )
            }
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
        // BEE-65: the .so files now come from `beeping-core` GH Releases v0.8.0+,
        // built with `-Wl,-z,max-page-size=16384` (per BEE-2221). Verified via
        // `readelf -l libbeepingcore.so | grep LOAD` → align 0x4000.
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // BEE-65: native libraries come from the downloadBeepingCore task output,
    // not from a `src/main/jniLibs/` checked-in directory. The download task
    // populates `build/intermediates/beeping-core/<abi>/libbeepingcore.so`
    // and Android picks them up at packaging time.
    sourceSets.named("main") {
        jniLibs.srcDirs(layout.buildDirectory.dir("intermediates/beeping-core"))
    }

    // BEE-2226: JNI shim built from src/main/cpp/. The CMake build links the
    // shim against the prebuilt libbeepingcore.so populated by downloadBeepingCore.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
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
// Reads from the gitignored `.env.local` at the repo root if present, falls
// back to the process env (CI / shells that already exported them).
// CI without any of these falls back to MockEngine-only tests.
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

fun beepboxEnv(name: String): String = dotenvLocal[name] ?: System.getenv(name) ?: ""

android.testOptions.unitTests.all { test ->
    listOf(
        "BEEPBOX_DEV_BASE_URL",
        "BEEPBOX_DEV_API_KEY",
        "BEEPBOX_PROD_BASE_URL",
        "BEEPBOX_PROD_API_KEY",
        "BEEPBOX_BASE_URL",
        "BEEPBOX_API_KEY",
    ).forEach { test.environment(it, beepboxEnv(it)) }
}

// ── BEE-65: download beeping-core .so artifacts from GitHub Releases ────────
// The legacy 2020 .so files were vendored under src/main/jniLibs/ and did not
// satisfy the 16 KB page size requirement for Android 15+. Now we fetch the
// freshly built artifacts from beeping-core GH Releases (BEE-2221 emits them
// per-ABI with -Wl,-z,max-page-size=16384) and verify SHA256 against the
// release-published SHA256SUMS.txt.
//
// Cosign verify is intentionally skipped here. The upstream release workflow
// emits only `.sig` (no `.bundle` / no `--output-certificate`) — keyless
// verification needs the certificate too. Tracked in pending-011 and upstream
// BEE-2225 (Phase 1, beeping-core).

val downloadBeepingCore =
    tasks.register("downloadBeepingCore") {
        group = "beeping"
        description = "Download + SHA256-verify + extract beeping-core .so artifacts."

        inputs.property("version", beepingCoreVersion)
        inputs.property("abis", beepingCoreAbis)
        outputs.dir(beepingCoreOutDir)
        outputs.dir(beepingCoreHeadersDir)

        doLast {
            val out = beepingCoreOutDir.get().asFile
            val headersOut = beepingCoreHeadersDir.get().asFile
            val cache = out.resolve(".cache").apply { mkdirs() }
            // Reset headers dir on each run to track upstream API changes (a header
            // removed upstream must disappear here too, not linger from a prior version).
            headersOut.deleteRecursively()
            headersOut.mkdirs()
            val baseUrl = "https://github.com/beeping-io/beeping-core/releases/download/v$beepingCoreVersion"

            // 1. SHA256SUMS.txt
            val sumsFile = cache.resolve("SHA256SUMS.txt")
            URI("$baseUrl/SHA256SUMS.txt").toURL().openStream().use { input ->
                sumsFile.outputStream().use { input.copyTo(it) }
            }
            val sums =
                sumsFile
                    .readLines()
                    .filter { it.isNotBlank() }
                    .associate { line ->
                        val parts = line.trim().split(Regex("\\s+"), limit = 2)
                        require(parts.size == 2) { "Malformed SHA256SUMS entry: '$line'" }
                        parts[1] to parts[0]
                    }

            // 2. per-ABI download + SHA256 verify + extract
            beepingCoreAbis.forEach { abi ->
                val tarball = "beeping-core-android-$abi.tar.zst"
                val expectedSha =
                    sums[tarball]
                        ?: error("$tarball missing from SHA256SUMS.txt for v$beepingCoreVersion")

                val tarballFile = cache.resolve(tarball)
                if (!tarballFile.exists() || sha256(tarballFile) != expectedSha) {
                    URI("$baseUrl/$tarball").toURL().openStream().use { input ->
                        tarballFile.outputStream().use { input.copyTo(it) }
                    }
                }

                val actualSha = sha256(tarballFile)
                require(actualSha == expectedSha) {
                    "SHA256 mismatch for $tarball: expected=$expectedSha actual=$actualSha"
                }

                val abiDir =
                    out.resolve(abi).apply {
                        deleteRecursively()
                        mkdirs()
                    }

                // bsdtar 3.5+ and GNU tar 1.31+ auto-detect zstd compression
                exec {
                    workingDir = abiDir
                    commandLine("tar", "-xf", tarballFile.absolutePath)
                }

                val extractedSo = abiDir.resolve("lib/libbeepingcore.so")
                val targetSo = abiDir.resolve("libbeepingcore.so")
                require(extractedSo.exists()) { "$extractedSo missing after extract of $tarball" }
                extractedSo.renameTo(targetSo)

                // Headers are ABI-independent; copy once from the first ABI
                // into the shared headers dir, then drop the per-ABI copy.
                val extractedInclude = abiDir.resolve("include")
                val sharedInclude = headersOut.resolve("include")
                if (!sharedInclude.exists() && extractedInclude.exists()) {
                    extractedInclude.renameTo(sharedInclude)
                }
                abiDir.resolve("lib").deleteRecursively()
                abiDir.resolve("include").deleteRecursively()
            }
        }
    }

fun sha256(file: java.io.File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { stream ->
        val buf = ByteArray(8192)
        while (true) {
            val n = stream.read(buf)
            if (n < 0) break
            digest.update(buf, 0, n)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

// Wire as preBuild dependency so any downstream task (assemble, test, lint)
// triggers it transitively. The task is up-to-date when the version is
// unchanged and the output dir already has the expected .so files.
tasks.named("preBuild") {
    dependsOn(downloadBeepingCore)
}

// BEE-2226: CMake (externalNativeBuild*) needs the beeping-core headers
// and prebuilt .so to be in place before it configures. preBuild is not
// always a transitive dep of native-build tasks, so wire explicitly.
tasks
    .matching {
        it.name.startsWith("externalNativeBuild") ||
            it.name.startsWith("configureCMake")
    }.configureEach {
        dependsOn(downloadBeepingCore)
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
                // BEE-2226: LocalEncoder talks to the JNI shim + AudioRecord —
                // neither loads under JVM unit tests. The full encode/decode
                // paths are exercised by androidTest/ instrumented tests (added
                // in this same task). The JVM tests still verify the validation
                // + permission + native-not-loaded branches.
                classes("com.beeping.AndroidBeepingCore.LocalEncoder*")
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
