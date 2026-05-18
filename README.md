# 🤖 Beeping Android SDK

![License](https://img.shields.io/badge/License-Apache_2.0-blue)
![status](https://img.shields.io/badge/status-early_development-orange)
![platform](https://img.shields.io/badge/platform-Beeping-7C3AED)
![conventional commits](https://img.shields.io/badge/conventional_commits-1.0.0-yellow)
![Kotlin target](https://img.shields.io/badge/Kotlin_target-2.0+-7F52FF)
![Gradle target](https://img.shields.io/badge/Gradle_target-8.7+-02303A)
![Android SDK](https://img.shields.io/badge/Android_SDK-24--35-3DDC84)
[![Maven Central](https://img.shields.io/maven-central/v/io.beeping/beeping-android.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.beeping/beeping-android)

> 🔊 Kotlin SDK for **data over sound** (audible + ultrasonic) on Android.
> Decode and emit short payloads via the device speaker/microphone, locally
> or through the Beeping Platform cloud.
> Part of the [Beeping Platform](https://github.com/beeping-io) ecosystem.

---

## 🚧 Status

**Early development — repository bootstrapped on 2026-04-28.**

The codebase currently contains **legacy 2020-era Java sources** (AGP 4.0.1,
`jcenter()`, Android support library, vendored `.so` libs across 7 ABIs).
This is the starting point — **the SDK is not yet consumable**.

The full migration to Kotlin 2.0 + AndroidX + AGP 8.5+ + Maven Central
distribution is tracked in **Phase 8** of the Beeping Platform Linear project
(milestone 🤖 Phase 8, 16 tasks BEE-51..BEE-66, 99 story points). See
[`docs/PRODUCTO.md`](docs/PRODUCTO.md) for the full scope and
[`docs/ROADMAP.md`](docs/ROADMAP.md) for the live timeline.

The target public API is documented below for reference, but **not yet
implemented** — track Phase 8 progress for availability.

---

## 📦 Installation

> ⚠️ **Maven Central onboarding pending** (Sonatype Central Portal review, 2-4 weeks
> typical). Until the first release lands, consume the AAR directly from
> [GitHub Releases](https://github.com/beeping-io/beeping-android/releases).
> See [`docs/maven-central-publishing.md`](docs/maven-central-publishing.md).

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.beeping:beeping-android:0.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'io.beeping:beeping-android:0.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.beeping</groupId>
    <artifactId>beeping-android</artifactId>
    <version>0.0.0</version>
    <type>aar</type>
</dependency>
```

---

## 🎯 Target API (post-Phase 8)

```kotlin
// Gradle dependency (target — published as of v0.0.0)
// implementation("io.beeping:beeping-android:0.0.0")

import io.beeping.android.BeepingClient
import io.beeping.android.BeepingEvent
import io.beeping.android.BeepingMode

val client = BeepingClient.Builder(context)
    .mode(BeepingMode.Local)            // or BeepingMode.Cloud(apiKey, endpoint)
    .build()

// Listen for incoming beeps
viewModelScope.launch {
    client.listen().collect { event ->
        when (event) {
            is BeepingEvent.Started -> /* session up */
            is BeepingEvent.Decoded -> handle(event.payload)
            is BeepingEvent.Failed  -> handle(event.reason)
            BeepingEvent.Stopped    -> /* session closed */
        }
    }
}

// Encode + emit (suspend)
val pcm = client.encode("HOLA1")
client.play(pcm)

client.stop()
```

### Scheduled transmissions (BEE-2240, beeping-core ≥ 0.8.1)

`BeepingClient` exposes the scheduler from the core C API for the typical
"emit code X every I seconds across D seconds" use case:

```kotlin
import com.beeping.AndroidBeepingCore.BeepingPayload

// 1. Preview the schedule (pure utility, no audio rendered).
val timestamps: List<Double> = client.computeBeepSchedule(
    duration = 10f,    // seconds, >= 2.3
    startTime = 0f,
    interval = 2.3f,
)
// → [0.0, 2.3, 4.6, 6.9, 9.2]  (5 beeps)

// 2. Encode + play the whole schedule in one suspend call (LOCAL mode only).
//    Each beep carries `code + 4-char base-32 timestamp` so receivers can
//    recover position within the schedule via the scheduler-aware decode helpers.
client.sendScheduled(
    payload = BeepingPayload(payload = "abc12"),
    duration = 10f,
    startTime = 0f,
    interval = 2.3f,
    beepGainDb = 0f,  // [-60, +12] dB; 0 = identity
    audible = false,  // true → 3.3-10 kHz (QA / demos); false → 17.8-21 kHz (prod)
)
```

Cloud mode (`BeepingMode.Cloud`) currently fails with
`BeepingError.SchedulingNotSupported` — no beepbox endpoint yet.

See [`docs/PRODUCTO.md`](docs/PRODUCTO.md) section 10 for the full flow and
section 11 for state/error semantics.

---

## 📁 Repository structure

| Path | Purpose | Phase 8 owner |
|---|---|---|
| `AndroidBeepingCore/` | Library module (legacy Java) | BEE-53 (Kotlin migration), BEE-65 (drop vendored `.so`) |
| `AndroidBeepingCore/src/main/jniLibs/` | Vendored native libs (7 ABIs, **temporary**) | BEE-55 (cleanup), BEE-65 (consume from beeping-core releases) |
| `app/` | Sample app stub (currently no Activity) | BEE-64 (Compose rewrite + debug console) |
| `docs/PRODUCTO.md` | Product spec — source of truth for Phase 8 | — |
| `docs/ROADMAP.md` | Live timeline (recalculated on every closed task) | — |
| `docs/ROADMAP_CHANGELOG.md` | Append-only history of timeline changes | — |
| `docs/IDEAS.md` | Cross-project ideas capture | — |
| `docs/PENDING.md` | Cross-project pending capture | — |
| `build.sh` | Convenience build script (legacy) | superseded by BEE-52 |
| `.github/workflows/` | CI/CD (added in Paso 5) | BEE-63 (lint), BEE-62 (tests) |

---

## 💻 Local development setup

To work on this repo you need:

- **Node 20+** for dev tooling (commitlint, lefthook hooks). Install via `nvm`, `fnm` or `brew install node`.
- **JDK 17+** for the Android build (Gradle 8.7 requirement).

```bash
# install dev tooling + activate git hooks
npm install
```

Hooks active after `npm install`:

- 🪝 **`commit-msg`** — every commit message must follow Conventional Commits
  (validated by [commitlint](https://commitlint.js.org/) with the shared preset
  [`@beeping.io/commitlint-config`](https://github.com/beeping-io/commitlint-config))
- 🛡️ **`pre-push`** — direct pushes to `develop` / `main` are blocked
  (defense in depth — GitHub branch protection is authoritative)

To bypass hooks (never without explicit authorization): `git commit --no-verify`.

### OpenAPI client sync (BEE-59)

The Ktor HTTP client used by `CloudEncoder` is generated from the canonical
`beepbox-server` spec via [openapi-generator]. The spec is vendored at
[`api/openapi.yaml`](api/openapi.yaml) and the source of truth lives upstream
in [`beeping-io/beepbox`](https://github.com/beeping-io/beepbox).

**Re-vendor flow** (when beepbox publishes a spec change):

```bash
# 1. Copy the latest spec from the sibling repo (or download from GitHub)
cp ../beepbox/docs/openapi.yaml api/openapi.yaml

# 2. Re-generate the client and verify everything compiles
./gradlew :AndroidBeepingCore:openApiGenerate
./gradlew :AndroidBeepingCore:test

# 3. Commit the spec change (generated code lives in build/, gitignored)
git add api/openapi.yaml && git commit -m "chore(api): bump openapi.yaml to <sha>"
```

The generated sources land in `AndroidBeepingCore/build/generated/openapi/`
and are added to the main source set automatically. They live under the
`com.beeping.AndroidBeepingCore.internal.api` package and are not part of
the public SDK API surface.

[openapi-generator]: https://github.com/OpenAPITools/openapi-generator

---

## 🔧 Building

```bash
# macOS — point JAVA_HOME at JDK 17+
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# build the SDK + sample APK and run unit tests
./build.sh

# skip tests
./build.sh --no-tests

# clean before build
./build.sh --clean
```

Outputs:

- AAR: `AndroidBeepingCore/build/outputs/aar/`
- Sample APK: `app/build/outputs/apk/debug/app-debug.apk`
- Test report: `AndroidBeepingCore/build/reports/tests/test/index.html`

---

## 🧪 Development gates (target)

Real config lands in Phase 8. Targets per `docs/PRODUCTO.md` section 12:

- **Lint**: ktlint + detekt + Android Lint, **0 warnings** (BEE-63)
- **Tests**: JUnit5 + MockK + Robolectric + Kotest property + Paparazzi snapshots + Pitest mutation (BEE-62)
- **Coverage**: ≥80% line on `:AndroidBeepingCore`
- **Mutation score**: ≥70% Pitest
- **CI**: GitHub Actions, full pipeline < 10 min

---

## 🌐 Ecosystem

`beeping-android` is one of ~18 components of the Beeping Platform.

| Component | Repo | Role |
|---|---|---|
| Core C++ library | [`beeping-core`](https://github.com/beeping-io/beeping-core) | Encoding/decoding C++ engine; emits native `.so` for android |
| Cloud HTTP server | [`beepbox`](https://github.com/beeping-io/beepbox) | Beeping Platform cloud API (consumed by Cloud mode) |
| iOS SDK | (sister, Phase 9) | iOS counterpart |
| Flutter plugin | (Phase 10) | Wraps both Android + iOS |
| React Native | (Phase 12) | Wraps both Android + iOS |
| Reference apps | [`beeply`](https://github.com/beeping-io/beeply) | Flutter reference app |
| Governance | [`beeping-meta`](https://github.com/beeping-io/beeping-meta) | Conventions, ADRs, brand, terraform |

---

## 💬 Community

### Chat & support

- 💬 **Discord** — [chat with the team and other developers](https://discord.gg/beeping)
- 💼 **Slack** — coming soon (will be cross-bridged with Discord)
- 🐛 **Issues** — see the [issue tracker](issues) for bugs and feature requests

### Follow us

Social handles coming soon — see the issue tracker for status.

- 🐦 X (Twitter) · 💼 LinkedIn · 📸 Instagram · 🎵 TikTok
- 📺 YouTube · 🐘 Mastodon · 🦋 Bluesky

## 🤝 Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Conventions, branch model and PR
rules are shared ecosystem-wide via
[`beeping-io/beeping-meta`](https://github.com/beeping-io/beeping-meta).

## 🔒 Security

See [`SECURITY.md`](SECURITY.md). **Do not open public issues for vulnerabilities.**

## 📜 License

Apache License 2.0 — see [`LICENSE`](LICENSE).
