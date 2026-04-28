# 🧠 CLAUDE.md — `beeping-android`

> This file is loaded into every Claude Code session in this repository.
> Global methodology lives in `~/.claude/CLAUDE.md` and **takes precedence**
> over anything below — read it first.

---

## 🎯 What this repo is

The Android SDK component of the **Beeping Platform** ecosystem. A Kotlin
library (target) that exposes a modern instance-based API to encode and
decode short payloads transmitted over sound (audible + ultrasonic), running
locally via JNI to `beeping-core` or remotely via the `beepbox` HTTP server.

**Source of truth for scope and timeline**:

- 📄 `docs/PRODUCTO.md` — product spec (read this before any non-trivial change)
- 📅 `docs/ROADMAP.md` — live timeline
- 📜 `docs/ROADMAP_CHANGELOG.md` — append-only history of timeline changes

---

## 🚧 Current state vs. target state

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
| Sample app | Empty manifest, no Activity | Jetpack Compose + debug console | BEE-64 |
| CI | None | GitHub Actions (lint + test + assemble) | Paso 5 / BEE-63 / BEE-62 |
| Lint | None | ktlint + detekt + Android Lint strict | BEE-63 |
| Tests | 1 trivial JVM test | JUnit5 + MockK + Robolectric + Kotest + Paparazzi + Pitest + instrumented | BEE-62 |

Detailed source baseline lives in `MEMORY.md` → `memory/source_baseline.md`.

---

## 📋 Linear context

- **Project**: 🔊 Beeping Platform · id `a83369a5-3cb8-4fca-932d-ee33f6a7a00e`
  · slugId `03da887d924e`
- **Active milestone**: 🤖 Phase 8 — beeping-android (Kotlin 2.0)
  · id `cf4da38e-c680-40ba-9194-20d0f075ef73`
- **Tasks**: BEE-51 through BEE-66 — 16 tasks, 99 story points total
- **Default assignee**: Alfred Rivas · `alfred@beeping.io`
  · id `706c7757-beef-49ec-be7e-6b63f3ab2de0`
- **API key**: from `.env.local` (gitignored), or session-provided. Never paste in chat.
- **Linear queries**: always via `curl` to GraphQL — never via MCPs (per global rule).

---

## 🌳 Branch model

Per global methodology:

- `develop` — base branch. **Never commit directly** (except during `/worktree-init` bootstrap, which is the one documented exception).
- `main` — release branch. Auto-managed by release-please once activated (Phase 8 BEE-66).
- **Milestone mode** (default for Phase 8): single shared branch `milestone/phase-8`,
  N commits (one per BEE-task), one PR at the end → `develop`.
- **Individual task mode**: branch per task, e.g. `feat/bee-57-strategy-pattern`,
  one PR per task → `develop`.

Branch protection (PR + 1 review + linear history + signed commits + no force push)
becomes active after BEE-51 closes.

---

## ✏️ Commit conventions

Conventional Commits with **Linear task ID in the subject**:

```
feat(jni): BEE-57 strategy pattern for LocalEncoder/CloudEncoder
fix(http): BEE-59 retry on 429 with exponential backoff
chore(deps): BEE-52 bump Gradle 6.1.1 → 8.7
docs: BEE-66 document Maven Central onboarding flow
test(unit): BEE-62 add Kotest property tests for BeepingPayload
```

Co-authoring with Claude is fine and follows global rule.

---

## 🧪 Testing strategy (target)

- **Unit/JVM**: JUnit5 + MockK + Robolectric + Kotest (property-based)
- **Snapshot**: Paparazzi for Compose UI states (sample app)
- **Mutation**: Pitest with ≥70% mutation score gate
- **Instrumented**: AndroidX Test + Espresso for integration on emulator
- **Coverage**: ≥80% line coverage on `:AndroidBeepingCore` (jacoco)

CI gates (BEE-62 + BEE-63):

1. `./gradlew check` → 0 lint warnings, 0 detekt issues, all tests pass
2. `./gradlew pitestRelease` → mutation score gate
3. Coverage report uploaded as artifact

---

## 🔨 Build commands

Currently (legacy stack, JDK 11):

- `./build.sh` — clean+test+assemble shortcut
- `./gradlew :AndroidBeepingCore:test` — unit tests
- `./gradlew :app:assembleDebug` — sample APK

After BEE-52 modernization:

- `./gradlew check` — full quality gate
- `./gradlew :AndroidBeepingCore:assembleRelease`
- `./gradlew :AndroidBeepingCore:publishToMavenLocal` (after BEE-66)

---

## 🚫 Things to never do

- ❌ Reintroduce `jcenter()` or `com.android.support:*` deps.
- ❌ Commit `.so` binaries from anywhere except a pinned `beeping-core` release tag (post-BEE-65).
- ❌ Hardcode API keys or secrets — always `.env.local` or build args.
- ❌ Skip `🧑‍🔬 Human QA Checkpoint` for any task with observable effect.
- ❌ Close a Linear task without commenting the diff summary + QA cycles.
- ❌ Update `docs/ROADMAP.md` without a new entry in `docs/ROADMAP_CHANGELOG.md`.
- ❌ Force-push to `develop` or `main`.
- ❌ Commit directly to `develop` after `/worktree-init` is done.
- ❌ Merge a PR without all CI gates green.

---

## 📎 See also

- `~/.claude/CLAUDE.md` — global methodology (mandatory read)
- `docs/PRODUCTO.md` — full product spec
- `docs/ROADMAP.md` — live timeline
- `docs/ROADMAP_CHANGELOG.md` — timeline change history
- `MEMORY.md` — Claude session memory index
- [`beeping-meta`](https://github.com/beeping-io/beeping-meta) — ecosystem governance, conventions, brand, terraform
