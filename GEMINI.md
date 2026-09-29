# 🤖 GEMINI.md — `beeping-android`

> **Component-Level Instructions for Antigravity CLI (`agy`).**  
> Location: `/Users/fredi/Workspace/Beeping/beeping-android/GEMINI.md`.  
> Hierarchy: Inherits Global Methodology (`~/.gemini/config/rules/methodology.md`) and Workspace Rules (`../GEMINI.md`).

---

## 🎯 Role & Technology Stack

- **Purpose**: Android SDK component of the Beeping Platform ecosystem. Exposes a modern instance-based API to encode and decode short payloads transmitted over sound (audible + ultrasonic), running locally via JNI to `beeping-core` or remotely via the `beepbox` HTTP server.
- **Tech Stack**: `Kotlin 2.0 · AGP 8.5+ · JNI / NDK r28 · CMake 3.22.1`
- **Direct Consumers**: `beeping_flutter_android`, Native Android Apps
- **Distribution**: Maven Central (`io.beeping:beeping-android`)

**Source of Truth for Scope and Architecture**:
- 📄 `docs/PRODUCTO.md` — Product specification
- 📅 `docs/ROADMAP.md` — Live timeline
- 📜 `docs/ROADMAP_CHANGELOG.md` — Append-only history of timeline changes

---

## 🚧 Current State vs. Target State (Phase 8 Modernization)

| Aspect | Current (legacy) | Target (post-Phase 8) | Owner |
|---|---|---|---|
| Language | Java | Kotlin 2.0 | BEE-53 |
| AGP | 4.0.1 | 8.5+ | BEE-54 |
| Gradle | 6.1.1 | 8.7+ | BEE-52 |
| Kotlin DSL | No | Yes + version catalogs | BEE-52 |
| compileSdk / targetSdk | 26 / 26 | 35 / 35 | BEE-54 |
| minSdk lib | 26 | 24 | BEE-54 |
| ABIs | 7 (incl. `mips`, `armeabi`) | 3 (`arm64-v8a`, `armeabi-v7a`, `x86_64`) | BEE-55 |
| UI lib | `com.android.support` | AndroidX | BEE-53 |
| Repos | `jcenter` + google | `mavenCentral` + google | BEE-52 |
| Public API | Singleton-ish + listener | `BeepingClient` instance + `Flow<BeepingEvent>` | BEE-56 |
| Native libs | Vendored `.so` (Jul 2020) | Consumed from `beeping-core` GH releases | BEE-65 |
| Distribution | None | Maven Central + GH Releases (signed) | BEE-66 |
| Sample app | Empty manifest, no Activity | Rebuilt aligned with `beeping_flutter` example | BEE-2336 (Phase 10) |
| CI | None | GitHub Actions (lint + test + assemble) | BEE-63 / BEE-62 |
| Lint | None | ktlint + detekt + Android Lint strict | BEE-63 |
| Tests | 1 trivial JVM test | JUnit5 + MockK + Robolectric + Kotest + Paparazzi + Pitest + instrumented | BEE-62 |

---

## 🛠️ Canonical Developer Commands

### 1. Build

```bash
# Build library debug AAR
./gradlew :AndroidBeepingCore:assembleDebug

# Build library release AAR
./gradlew :AndroidBeepingCore:assembleRelease

# Publish to local Maven cache for consumer verification
./gradlew :AndroidBeepingCore:publishToMavenLocal
```

### 2. Run Tests

```bash
# Run unit tests on JVM
./gradlew :AndroidBeepingCore:test

# Mutation testing gate (Pitest)
./gradlew pitestRelease
```

### 3. Lint & Format

```bash
# Full check (Android Lint + Detekt + ktlint)
./gradlew check

# Code formatting
./gradlew ktlintFormat
```

---

## 🧪 Testing Strategy

- **Unit/JVM**: JUnit5 + MockK + Robolectric + Kotest (property-based)
- **Snapshot**: Paparazzi for Compose UI states
- **Mutation**: Pitest with $\ge 70\%$ mutation score gate
- **Instrumented**: AndroidX Test + Espresso for integration on emulator
- **Coverage**: $\ge 80\%$ line coverage on `:AndroidBeepingCore` (JaCoCo)

---

## 🚫 Things to Never Do

- ❌ **Reintroduce `jcenter()` or `com.android.support:*` dependencies** (AndroidX only).
- ❌ **Commit `.so` binaries from anywhere except a pinned `beeping-core` release tag**.
- ❌ **Hardcode API keys or secrets** — always use `.env.local` or build arguments.
- ❌ **Skip `🧑‍🔬 Human QA Checkpoint`** for any task with observable effects on device or emulator.
- ❌ **Force-push to `develop` or `main`**.
- ❌ **Commit directly to `develop` or `main`** without PR and automated CI gate validation.

---

## 🚦 Quality Gates & Conventions

1. **Zero Tolerance Quality Gate**:
   - **0 test failures**
   - **0 lint warnings / detekt violations**
   - Code formatting verified before reporting any task done.
2. **Lefthook Enforced**:
   - Pre-commit checks (whitespace, json/yaml lint, gitleaks).
   - Commit-msg Conventional Commits: `feat(scope): BEE-XXXX subject`.
   - Pre-push branch protection (blocks direct push to `develop` or `main`).
3. **Branching Model**:
   - Base branch: `develop`
   - Feature branch: `feat/<task-id>-<slug>`
   - Fix branch: `fix/<task-id>-<slug>`
   - Spike / Discovery branch: `spike/<task-id>-<slug>`
4. **Ecosystem Impact**:
   - If modifying APIs consumed by downstream packages (`beeping_flutter_android`), the `## 🔗 Ecosystem Impact` table must be declared and reviewed.
