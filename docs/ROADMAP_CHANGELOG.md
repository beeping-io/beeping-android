# 📅 ROADMAP Changelog

> Historial completo de cambios en `docs/ROADMAP.md`.
> Mantenido automáticamente: cada vez que el ROADMAP cambia, se añade una
> nueva entrada al inicio de la sección History con el diff versus la versión
> anterior. **Append-only**: nunca borrar ni editar entradas pasadas (excepto
> para corregir typos en el mismo día de creación).

---

## 🎯 Snapshot actual

- **Fecha de inicio del proyecto**: 2026-04-28 (mar)
- **Fecha fin estimada (con 20% margen)**: 2026-05-18 (lun)
- **Velocidad asumida**: 8 story points / día hábil
- **Estado global**: 🔴 **Riesgo alto** — R2 (16 KB pages) escalado a alto el 2026-05-07 tras verificar que `beeping-core v0.6.0` no publica Android NDK builds. BEE-65 bloqueada hasta task previa en `beeping-core` repo
- **Última actualización**: 2026-05-07 (trigger: `Closed BEE-64 + R2 risk escalated`)
- **Story points totales**: 102 SP (Phase 8 — 99 originales + 2 BEE-1793 + 1 BEE-1815)
- **Story points cerrados**: 84 SP (BEE-51..64 + BEE-1793 + BEE-1815)
- **Story points remaining**: 18 SP (82.4% completado)
- **Days esfuerzo (con margen)**: 15 días hábiles
- **Fecha fin estimada actualizada**: 2026-05-15 (vie) — sin cambio

| # | Milestone | SP | Inicio est. | Fin est. | Estado |
|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | 99 | 2026-04-28 | 2026-05-18 | 🔴 Riesgo alto |

---

## 📜 History

### [2026-05-07] — Closed BEE-64 + R2 risk escalated to Alto + Pivot announced for BEE-67

**Trigger detallado**: BEE-64 cerrada (Sample app rewrite con Jetpack Compose + debug console, 8 SP, feat). Compose Material 3, MainScreen single-pane (LogoTapTarget 5-tap → DebugConsole, EnvSelector LOCAL/DEV/PROD, Key field, Send + Listen, StatusPanel con error chip dismissable), DebugConsole overlay (bottom sheet 70%, Share via ACTION_SEND, Close), Theme M3 con dynamic color condicional Android 12+ (`@RequiresApi` helper para evitar lint NewApi), BuildConfig fields seeded desde `.env.local` (parser shared con BEE-1815), ktlint + detekt + lint strict (`warningsAsErrors=true`) verde. WavPlayer (MediaPlayer-backed) integrado en `BeepingClient.send()` para reproducir el WAV devuelto por Cloud mode. BeepingTimberTree público con `MutableSharedFlow<String>` replay 200 para live tail de la console. QA emulator API 37 vía adb tap/screenshot loop: 7 checks (5 done — App abre, Permission flow Listen deny, 5-tap console, Share button, Dark mode toggle; 2 deferred — Send audio assertion + Dynamic color saltados por pivot acordado a BEE-67 listener-only).

**Net delta global**: 0 días en fin date. BEE-64 cerró 7 días antes de su slot planificado (`Fin est.` 2026-05-14, cerrada 2026-05-07) — adelanto absorbido pero no cascadeado a BEE-65/66 porque ambas siguen bloqueadas por dependencias externas.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: 🔴 **Riesgo alto** (escalado desde ⚠️ medio).

#### Cambios de estado

- BEE-64: `🚧 In Progress` → `✅ Done` (8 SP).
- R2 (16 KB pages support): ⚠️ Medio → 🔴 **Alto**. Verificado durante QA BEE-64 que `beeping-core v0.6.0` (latest) no publica Android NDK builds — solo linux/macos/wasm/windows targets. BEE-65 (Consumir `beeping-core` via GH Releases) **bloqueada** hasta abrir + cerrar task previa en `beeping-core` repo: "Publish Android NDK `.so` artifacts (arm64-v8a + armeabi-v7a + x86_64) with `-Wl,-z,max-page-size=16384` linker flag". Mientras tanto, sample app legacy + `.so` vendoreados solo funcionan en API <31 (4KB pages) o emulator opted-in 16KB pages.

#### Pivot acordado para BEE-67 (post-BEE-64)

User confirmó pivot 2026-05-07 durante QA: el sample app pasa a ser **listener-puro** demostrando solo el path de decode del SDK. Cambios para BEE-67:

- Quitar Send button + EnvSelector + cloud config + payload encoding helpers del sample.
- Auto-start del listener en `LaunchedEffect`.
- 3 secciones verticales: Header (logo + 5-tap console preserved) / Listener panel (status circle verde/gris + last decoded payload) / Activity log (últimas N events del SDK + botón "Open console").
- Branding rojo Beeping en lugar de lavender M3 default.
- `BeepingMode.Local` único — sin cloud config en sample.

Beeps audibles los emite un script Mac-side externo `scripts/send-beep` (Python o bash, en este repo) que lee `.env.local`, hace 9 reps con vol 0.1→0.9 step 0.1 gap 1s, POST `/v1/encode` a beepbox-server, save WAV temp, `afplay -v <vol>`. Logs estructurados por iteración. El `--target` (emulator/device) es etiqueta para quien monitoriza, no afecta al script.

BEE-67 **depende de BEE-65** (necesita `.so` listener funcional en API 35+/emulator moderno).

#### Secuencia revisada

1. ✅ BEE-64 cerrada (esta entrada).
2. ⏸️ Abrir task en `beeping-core` repo: "Publish Android NDK builds with 16KB-pages flag" (no en este ROADMAP — es upstream).
3. ⏸️ BEE-65 cuando cierre la task de `beeping-core`.
4. ⏸️ BEE-67 (nueva, sample pivot listener-only + `scripts/send-beep`) cuando BEE-65 cierre.
5. ⏸️ BEE-66 (Maven Central) al final, post-pruebas.

#### Detalle implementation BEE-64

- **Ficheros nuevos** (10): `WavPlayer.kt`, `BeepingSampleApp.kt`, `MainActivity.kt`, `MainScreen.kt`, `DebugConsole.kt`, `SampleAppViewModel.kt`, `SampleUiState.kt`, `ui/theme/Theme.kt`, `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`.
- **Ficheros modificados** (7): `BeepingClient.kt`, `BeepingTimberTree.kt`, `app/build.gradle.kts`, `AndroidManifest.xml`, `strings.xml`, `styles.xml`, `gradle/libs.versions.toml`.
- **Tests** (`./gradlew check :app:assembleDebug`):
  - `:AndroidBeepingCore:check` — 50/50 tests + ktlint + detekt + lint verde tras los cambios SDK.
  - `:app:lintDebug` — 6 issues iniciales fixeados (NewApi en dynamic color via @RequiresApi helper, RedundantLabel, DataExtractionRules + backup_rules.xml, MissingApplicationIcon, UnusedResources). 0 warnings con `warningsAsErrors=true`.
  - `:app:detekt` — 24 issues iniciales fixeados via config + suppressions justificados (LongMethod / MagicNumber para Composables + hex colors).
  - `:app:ktlintMainSourceSetCheck` — verde tras ktlintFormat + manual fix del KDoc-EOL ordering.
  - `:app:assembleDebug` — APK 19 MB.
- **QA emulator API 37** (los 7 checks de Linear):
  1. ✅ App abre · main screen render correcto.
  2. ⏭️ Send audio chirp — saltado por pivot a BEE-67.
  3. ✅ Permission flow Listen deny → chip rojo `RECORD_AUDIO permission not granted...`.
  4. ✅ 5-tap → debug console (bottom sheet 70%, logs JSON con trace-IDs).
  5. ✅ Share button → ACTION_SEND chooser con preview de logs.
  6. ✅ Dark mode toggle → paleta M3 dark instantánea.
  7. ⏭️ Dynamic color — saltado per acuerdo con user.
- **Observaciones documentadas**:
  - DebugConsole UX flaw: clicks fuera de los IconButtons cierran el overlay (Box.clickable parent + Surface sin pointer interceptor) → anotar para BEE-67.
  - API 37 LOCAL mode falla con `UnsatisfiedLinkError` → exactly lo que BEE-65 ataca.
  - Cloud mode listen emite Started→Stopped inmediato porque `CloudEncoder.decoded()` devuelve `emptyFlow()` (pending-006 SDK).

---

### [2026-05-04] — Scope change + Closed BEE-1815

**Trigger detallado**: scope addition descubierto durante validación E2E manual contra los 3 entornos (LOCAL/DEV/PROD). El test opt-in original (introducido en BEE-57, extendido en BEE-59) leía un único par `BEEPBOX_API_KEY`/`BEEPBOX_BASE_URL` y tenía `DEV_BASE_URL` hardcodeado a la Cloud Run dev URL — no permitía ejercitar PROD sin swap manual de env vars. Además, `./gradlew test` no auto-cargaba `.env.local`. Abierto BEE-1815 (1 SP, chore/test) para split + auto-load. Implementado y verificado en la misma sesión.

**Net delta global**: +1 SP totales (+1 al total Phase 8). 0 días en fin date — el SP cierra en la misma sesión.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Scope changes

- **BEE-1815 añadida** (1 SP): `🧪 Split CloudEncoder E2E into DEV/PROD opt-in + auto-load .env.local`. Total Phase 8: 101 → 102 SP.

#### Cambios de estado

- BEE-1815: `⏳ Pending` (creada) → `✅ Done` (cerrada en la misma sesión, 1 SP).

#### Detalle implementation

- `.env.example` — split del par único `BEEPBOX_BASE_URL`/`BEEPBOX_API_KEY` en cuatro vars (`BEEPBOX_DEV_BASE_URL`, `BEEPBOX_DEV_API_KEY`, `BEEPBOX_PROD_BASE_URL`, `BEEPBOX_PROD_API_KEY`). Documenta los dos consumers (library E2E tests + sample app `BuildConfig` de BEE-64). El par legacy se mantiene como fallback declarado en código.
- `AndroidBeepingCore/build.gradle.kts` — parser KISS de `.env.local` en root (10 LOC, ignora comentarios/blanks, trim de quotes), expuesto como `Map<String,String>`. Helper `beepboxEnv(name)` con prioridad `.env.local` → `System.getenv` → `""`. Forwarding de las 6 vars (4 DEV/PROD + 2 legacy) al test JVM via `android.testOptions.unitTests.all { test.environment(...) }`.
- `AndroidBeepingCore/src/test/.../CloudEncoderTest.kt` — eliminada la const `DEV_BASE_URL` hardcodeada y el único E2E. Añadidos dos `@Test` (`e2e DEV` + `e2e PROD`) que delegan a un helper `e2eAgainstEnvironment(label, envPrefix)` con `Assume.assumeFalse` cuando faltan vars + `Assume.assumeNoException` para skipear ante errores transitorios del server o keys rotadas.

#### Validación

- `curl POST /v1/encode` smoke contra DEV (`beepbox-dev.beeping.io`) y PROD (`beepbox.beeping.io`): HTTP 200 + 203 052 bytes + magic `RIFF` en ambos, ~3 s cada uno.
- `./gradlew :AndroidBeepingCore:check` end-to-end verde: 8/8 CloudEncoderTest (6 mock + 2 E2E reales — DEV ~1.0 s, PROD ~0.25 s), ktlint + detekt + Android Lint strict + Kover gate.

#### QA

- 🧑‍🔬 Human QA Checkpoint: **Skipped** — pure test infrastructure, no UI/UX/copy/flow change observable por end user.

#### Métricas tras BEE-1815

- 15/18 tasks cerradas (BEE-51..63 + BEE-1793 + BEE-1815), **76/102 SP (74.5%)**.
- 3 tasks pending: BEE-64 (sample app, 8 SP, In Progress), BEE-65 (5 SP), BEE-66 (13 SP). Restante: 26 SP.

---

### [2026-05-01] — Closed BEE-63

**Trigger detallado**: BEE-63 cerrada — ktlint 12.1.2 + detekt 1.23.7 + Android Lint strict wired como CI gate. Auto-format aplicado a 17 ficheros Kotlin. Cero violations en código propio. CI ahora corre 5 gates secuenciales (ktlint + detekt + lint + tests + Kover).

**Net delta global**: 0 días en fin date. BEE-63 cerró **11 días antes** de su Fin estimado (2026-05-12 → 2026-05-01).

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-63: 2026-05-12 → 2026-05-01 (-11 días)

#### Cambios de estado

- BEE-63: `⏳ Pending` → `✅ Done` (3 SP)

#### Detalle implementation

- 2 ficheros nuevos: `detekt.yml` (root, overrides sobre preset default), `AndroidBeepingCore/lint.xml` (ignora `GradleDependency` + `AndroidGradlePluginVersion` advisory checks).
- `gradle/libs.versions.toml` — plugin entries `org.jlleitschuh.gradle.ktlint:12.1.2` + `io.gitlab.arturbosch.detekt:1.23.7`.
- `AndroidBeepingCore/build.gradle.kts` — aplica plugins ktlint + detekt; configura `android.lint { warningsAsErrors abortOnError checkReleaseBuilds = true; lintConfig = lint.xml }`. Declara `dependsOn(openApiGenerate)` en ktlint runners + Detekt tasks (la generated dir está en main source set).
- `.editorconfig` — `ktlint_standard_package-name = disabled` (legacy mixed-case namespace `com.beeping.AndroidBeepingCore` es API contract público) + `ktlint_standard_function-naming = disabled` (BeepingCoreJNI external functions matchean los entry points de `libbeepingcore.so`).
- `.github/workflows/ci.yml` — step nuevo `🧼 Lint & static analysis` antes de los unit tests.
- `.gitignore` — `.claude/` añadido.
- 17 ficheros `src/main/**` + `src/test/**` auto-formateados (whitespace, trailing commas).

#### Cleanups durante el scan

- Dead `private val logger = BeepingLogger(traceId)` removido de `LocalEncoder` + `CloudEncoder` (UnusedPrivateProperty, unused desde BEE-60).
- `BeepingCoreJNI.kt:90` migrado de `android.util.Log.e` → `Timber.tag(TAG).e` (`LogNotTimber` lint check + consistency con BEE-60).
- 2 narrow `@Suppress("TooGenericExceptionCaught")` en `TelemetryEmitter.emit` y `CloudEncoder.encode` documentando los catches deliberados (telemetry must never break SDK; HTTP errors → typed BeepingException).

#### Métricas tras BEE-63

- ktlint: 8 source sets, 0 violations.
- detekt: 0 issues sobre preset default + overrides.
- Android Lint: 0 errors con `warningsAsErrors = true`.
- Tests: 50/50 verde (49 + 1 skipped E2E).
- Kover: 83.8% líneas (gate ≥ 70%).

#### Notas

- 14/17 tasks cerradas, **75/101 SP (74.3%)**, en 4 sesiones efectivas.
- Próximas 3 tasks: BEE-64 (sample app, 8 SP, **internal testing only** — tutorial-grade examples irán a otro repo dedicado en el futuro) · BEE-65 (consume beeping-core, 5 SP) · BEE-66 (Maven Central, 13 SP). Total restante: 26 SP.
- Recta final del milestone — cruzando 75% del trabajo completado.

---

### [2026-05-01] — Closed BEE-62

**Trigger detallado**: BEE-62 cerrada — test stack moderno wired (JUnit5 Jupiter + Vintage en paralelo, Robolectric 4.14.1 sdk=33, Kotest 5.9 property-based, Kover 0.9 coverage). 4 tests nuevos verdes (2 JUnit5 + 1 Robolectric + 1 property fuzzing 1000 iters). Coverage baseline 83.8% líneas. CI verde con nuevo Kover threshold gate.

**Net delta global**: 0 días en fin date. BEE-62 cerró **11 días antes** de su Fin estimado (2026-05-12 → 2026-05-01) — task más grande del milestone (13 SP) cerrada en una sesión.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-62: 2026-05-12 → 2026-05-01 (-11 días)

#### Cambios de estado

- BEE-62: `⏳ Pending` → `✅ Done` (13 SP)

#### Cambios de scope

- 3 entries nuevas en `docs/PENDING.md`:
  - `pending-008` — Pitest en módulo JVM separado.
  - `pending-009` — Paparazzi snapshots (depende BEE-64).
  - `pending-010` — Espresso/Compose UI + Codecov (depende BEE-64).
- Justificación: `info.solidsoft.pitest` no compone con `com.android.library` (requiere `java`/`java-library`); Paparazzi y Espresso requieren UI sample que llega en BEE-64.

#### Detalle implementation

- 3 ficheros test nuevos:
  - `JUnit5SmokeTest.kt` (2 tests Jupiter — `assertAll`, `assertThrows`).
  - `RobolectricSmokeTest.kt` (1 test, `RuntimeEnvironment.getApplication()` con sdk=33 shadows).
  - `KeyPatternPropertyTest.kt` (1 test, `Arb.string` × 1000 iters verificando `LocalEncoder.encode()` rechaza non-base32-5).
- `gradle/libs.versions.toml` — versions de junitJupiter/Platform 5.11.3, androidJunit5 (Mannodermaus) 1.12.0.0, robolectric 4.14.1, kotest 5.9.1, kover 0.9.0. Plugin entries nuevas.
- `AndroidBeepingCore/build.gradle.kts` — aplica plugins `de.mannodermaus.android-junit5` + `org.jetbrains.kotlinx.kover`. Habilita `unitTests.isIncludeAndroidResources = true` (Robolectric). Configura `kover { reports filters excludes packages internal.api.* + verify rule(\"Line coverage ≥ 70%\") }`.
- `.github/workflows/ci.yml` — añade step `📊 Coverage report + threshold gate (Kover ≥ 70%)` post-tests + sube artifact `kover-report` (HTML + XML).
- Suite total **50 tests verdes** (49 + 1 skipped E2E).

#### Métricas coverage baseline (Kover)

- **Líneas**: 233/278 = **83.8%** ✅ (gate ≥ 70%)
- Instructions: 1327/1702 = 78.0%
- Branches: 60/95 = 63.2%

#### Notas

- 13/17 tasks cerradas, **72/101 SP (71.3%)**, en 4 sesiones efectivas.
- Próximas 4 tasks: BEE-63 (lint, 3 SP) · BEE-64 (sample app, 8 SP) · BEE-65 (consume beeping-core, 5 SP) · BEE-66 (Maven Central, 13 SP). Total restante: 29 SP.
- Cruzado el umbral de 70% del milestone — recta final de Phase 8.

---

### [2026-05-01] — Closed BEE-61

**Trigger detallado**: BEE-61 cerrada — TelemetryHook + TelemetryEvent sealed class + TelemetryEmitter pipeline opt-IN funcional. 7 tests nuevos (4 de privacy con reflexión sobre forbidden substrings, 3 del emitter cubriendo disabled/enabled/exception-swallowed). Suite total 46/46 verde, CI verde 1m27s.

**Net delta global**: 0 días en fin date. BEE-61 cerró **7 días antes** de su Fin estimado (2026-05-08 → 2026-05-01). Adelanto absorbido por velocidad acumulada.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-61: 2026-05-08 → 2026-05-01 (-7 días)

#### Cambios de estado

- BEE-61: `⏳ Pending` → `✅ Done` (5 SP)

#### Detalle implementation

- 3 ficheros main nuevos:
  - `TelemetryEvent.kt` — sealed class con 5 variantes (`SdkInitialized`, `Closed`, `EncodeRequested`, `EncodeSucceeded`, `EncodeFailed`). Todas exponen sólo campos sanitizados (`mode`, `traceId`, `durationMs`, `byteCount`, `errorType` class name, `keyLength`).
  - `TelemetryHook.kt` — `fun interface` consumer-pluggable + `NoOp` companion default sink.
  - `TelemetryEmitter.kt` — internal forwarder que **traga excepciones del hook** (telemetry NUNCA debe romper el SDK).
- 2 ficheros test nuevos:
  - `TelemetryEventTest.kt` — 4 tests reflection-based: forbidden substrings (payload/apikey/endpoint/url/ip/token/auth/deviceid/userid/email/phone/secret/password) + whitelists por variante.
  - `TelemetryEmitterTest.kt` — 3 tests: disabled = no-op, enabled = forwards, exception = swallowed.
- `BeepingClient.kt` extendido:
  - Constructor anade `telemetryHook: TelemetryHook = TelemetryHook.NoOp`.
  - `init {}` → emite `SdkInitialized`. `send()` → `EncodeRequested` + (`EncodeSucceeded` | `EncodeFailed`). `close()` → `Closed(sessionDurationMs)`.
  - Builder gana `.telemetryHook(value)` setter.
- **Default `telemetryEnabled = false`** — opt-IN privacy-first per PRODUCTO.md DT-07.
- AAR release 411 → 419 KB (+8 KB pipeline completo).

#### Privacy guarantees verificadas

- ✅ No payload (raw encoded/decoded bytes).
- ✅ No apiKey, no endpoint, no URL.
- ✅ No IP, no deviceId/userId, no email/phone, no token/auth/secret.
- ✅ `errorType` es class name no message (que podría contener PII).
- ✅ `keyLength` es Int no el valor del key.
- ✅ Reflection guard rompe el build si alguien añade un campo PII-named.

#### Notas

- 12/17 tasks cerradas, 59/101 SP (58.4%), en 4 sesiones efectivas.
- Próximas 5 tasks: BEE-62 (test stack, 13 SP) · BEE-63 (lint, 3 SP) · BEE-64 (sample app, 8 SP) · BEE-65 (consume beeping-core, 5 SP) · BEE-66 (Maven Central, 13 SP). Total restante: 42 SP.
- R5 (telemetry opt-out filtra datos sin opt-in) → mitigación **completada**: opt-IN por default + reflection guard.

---

### [2026-05-01] — Closed BEE-60

**Trigger detallado**: BEE-60 cerrada — Timber JSON tree + trace-ID propagation funcional. 39 tests verdes (+9 nuevos del BeepingTimberTreeTest cubriendo level filtering, PII redaction, idempotent install). E2E real verde con la nueva BEEPBOX_API_KEY tras key swap durante exec.

**Net delta global**: 0 días en fin date. BEE-60 cerró **6 días antes** de su Fin estimado (2026-05-07 → 2026-05-01).

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-60: 2026-05-07 → 2026-05-01 (-6 días)

#### Cambios de estado

- BEE-60: `⏳ Pending` → `✅ Done` (3 SP)

#### Detalle implementation

- 2 ficheros main nuevos: `BeepingTimberTree` (object, JSON output + level filter + PII redaction), `BeepingLogger` (facade per-traceId)
- 1 fichero test nuevo: `BeepingTimberTreeTest` con 9 tests
- BeepingClient ahora expone `val traceId: String` público (UUID 8-chars)
- CloudEncoder añade `X-Trace-Id` header outbound via Ktor `defaultRequest`
- BeepingClient.Builder.build() planta tree + setLogLevel + propaga traceId al factory→encoder
- `+ Timber 5.0.1` dep
- AAR 396 → 404 KB (+8 KB por Timber library)

#### Bugs encontrados + arreglados

- Ktor 3.x `defaultRequest` es función (no plugin) → `config.defaultRequest { ... }`
- `Timber.Tree.isLoggable` protected → public mirror `BeepingTimberTree.shouldLog(priority)` para tests
- BEEPBOX_API_KEY rotada/inválida durante exec → `Assume.assumeNoException` graceful skip en E2E test (resilience cross-build)

#### Notas

- 11/17 tasks cerradas, 54/101 SP (53.5%), en 4 sesiones efectivas.
- Próximas 6 tasks: BEE-61 (telemetry, 5 SP) · BEE-62 (test stack, 13 SP) · BEE-63 (lint, 3 SP) · BEE-64 (sample app, 8 SP) · BEE-65 (consume beeping-core, 5 SP) · BEE-66 (Maven Central, 13 SP). Total restante: 47 SP.

---

### [2026-05-01] — Closed BEE-59

**Trigger detallado**: BEE-59 cerrada — OpenAPI Ktor client generation funcional, CloudEncoder refactorizado para usar EncodingApi generated, 30 tests verdes incluido E2E real. 4 bugs encontrados y arreglados durante exec (globalProperties filter, file→ByteArray mapping, ContentNegotiation install, buildDir deprecation). 1 follow-up nuevo (`pending-007` — close internal HttpClient when exposed).

**Net delta global**: 0 días en fin date. BEE-59 cerró **6 días antes** de su Fin estimado (2026-05-07 → 2026-05-01). Adelanto absorbido por velocidad acumulada.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-59: 2026-05-07 → 2026-05-01 (-6 días)

#### Cambios de scope

- `pending-007` añadida en `docs/PENDING.md` (close internal HttpClient when openapi-generator exposes it).

#### Cambios de estado

- BEE-59: `⏳ Pending` → `✅ Done` (3 SP)

#### Detalle implementation

- `api/openapi.yaml` vendoreado (12 773 bytes) desde `beepbox/docs/openapi.yaml`.
- 20 ficheros Kotlin generados (3 APIs + 7 models + 5 infrastructure + 5 auth) en `build/generated/openapi/` (gitignored).
- Plugin `org.openapi.generator` 7.10.0 + `kotlin-serialization` 2.0.21.
- 4 workarounds documentados: typeMappings binary→ByteArray, ContentNegotiation re-install via httpClientConfig, modern `layout.buildDirectory.dir(...)` API, removed empty globalProperties filter.
- AAR 303 → 396 KB (+93 KB) — generated infrastructure code. Reducirá con R8 minify en BEE-66 release builds.

#### Notas

- 10/17 tasks cerradas, 51/101 SP (50.5%), en 4 sesiones efectivas.
- Cruzamos el ecuador SP también — más de la mitad cerrada.
- Risk R4 (Ktor binary size impacta AAR) re-evaluado: 396 KB sigue muy por debajo del threshold de 1 MB. ✅ verde.

---

### [2026-04-30] — Closed BEE-58

**Trigger detallado**: BEE-58 cerrada — Builder DSL público + LogLevel enum + permission gate en LocalEncoder. 30 tests verdes (8 nuevos del Builder, 1 nuevo del permission). Drop justificado de `.apiKey()`/`.endpoint()` setters en favor de pasar `BeepingMode.Cloud(apiKey, endpoint)` directamente — type-safe, single-concept.

**Net delta global**: 0 días en fin date. BEE-58 cerró **6 días antes** de su Fin estimado (2026-05-06 → 2026-04-30). Adelanto absorbido por velocidad acumulada.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-58: 2026-05-06 → 2026-04-30 (-6 días)

#### Cambios de estado

- BEE-58: `⏳ Pending` → `✅ Done` (3 SP)

#### Detalle implementation

- 2 ficheros main nuevos: `LogLevel.kt` enum public, `BeepingClient.Builder` inner class
- 1 fichero test nuevo: `BeepingClientBuilderTest` (7 tests cubriendo defaults, validation, fluent chaining)
- BeepingClient constructor extendido (logLevel + telemetryEnabled storage)
- LocalEncoder permission gate: `MissingMicPermission` y `NativeLibraryNotLoaded` ahora se surfacean como `BeepingException` que se convierte en `BeepingEvent.Failed` en el listen() flow

#### Notas

- 9/17 tasks cerradas, 48/101 SP (47.5%), en 3 sesiones efectivas.
- Cruzamos el ecuador del milestone — más de la mitad del trabajo cerrado.
- Próximas tasks: BEE-59 (HTTP client OpenAPI generation, 3 SP), BEE-60 (Timber wiring, 3 SP), BEE-61 (Telemetry hook, 5 SP) — bloque cohesivo de "wiring inner concerns" antes de BEE-62 (test stack, 13 SP).

---

### [2026-04-30] — Closed BEE-57

**Trigger detallado**: BEE-57 cerrada — Strategy pattern Local/Cloud + Ktor 3.0.3 + 22 tests verdes incluido E2E real (4.96s) contra dev Cloud Run URL. CloudEncoder `encode()` totalmente funcional contra `https://beepbox-server-ai7n45q5lq-ew.a.run.app/v1/encode`. LocalEncoder con SHELL JNI wiring (encode pendiente BEE-65). Bug fixes en libs.versions.toml duplicate bundles + Mockk added.

**Net delta global**: 0 días en fin date. BEE-57 cerró **6 días antes** de su Fin estimado (2026-05-06 → 2026-04-30). Adelanto absorbido por velocidad acumulada.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-57: 2026-05-06 → 2026-04-30 (-6 días)

#### Cambios de scope

- `pending-006` añadida en `docs/PENDING.md` (Cloud-mode live decoding via AudioRecord chunking) — out of scope BEE-57, deferred a futuro

#### Cambios de estado

- BEE-57: `⏳ Pending` → `✅ Done` (8 SP)

#### Detalle implementation

- 4 ficheros main nuevos: `BeepingEncoder` (rename), `LocalEncoder`, `CloudEncoder`, `BeepingEncoderFactory`
- `BeepingException(error: BeepingError)` adapter en `BeepingError.kt`
- 3 ficheros test nuevos: `LocalEncoderTest`, `CloudEncoderTest` (con MockEngine + opt-in E2E real), `BeepingEncoderFactoryTest`
- BeepingClient refactorizado para wirear encoder (try/finally garantiza Stopped, BeepingException → Failed mapping)
- Build chain: + Ktor 3.0.3 + kotlinx-serialization-json 1.7.3 + Mockk 1.13.13 + kotlin-serialization plugin
- AAR 275 KB → 306 KB (+31 KB por Ktor + serialization)

#### Notas

- 8/17 tasks cerradas, 45/101 SP (44.6%), en 3 sesiones efectivas.
- E2E real funciona contra dev — `api.beeping.io` PROD sigue sin DNS configurado.
- LocalEncoder requiere Activity-aware Context para audio infrastructure → BEE-58 lo cierra.

---

### [2026-04-29] — Closed BEE-56

**Trigger detallado**: BEE-56 cerrada — nueva API SHELL `BeepingClient` + sealed classes (BeepingEvent, BeepingError, BeepingMode) + BeepingPayload + internal Encoder interface. Tests Turbine verdes. La implementación real (encode/decode) llega en BEE-57.

**Net delta global**: 0 días en fin date. BEE-56 cerró **6 días antes** de su Fin estimado (2026-05-05 → 2026-04-29). Adelanto absorbido por velocidad acumulada.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-56: 2026-05-05 → 2026-04-29 (-6 días)

#### Cambios de estado

- BEE-56: `⏳ Pending` → `✅ Done` (8 SP)

#### Detalle implementation

- 7 ficheros `.kt` nuevos: `BeepingClient`, `BeepingEvent`, `BeepingError`, `BeepingMode`, `BeepingPayload`, `Encoder` (internal), `BeepingClientTest`.
- 3 ficheros legacy eliminados: `BeepingCore`, `BeepingCoreEvent`, `BeepHandler` (per PRODUCTO.md §7 sin back-compat).
- `BeepingCoreJNI` refactor: `BeepingCore` ref → callback field. JNI ABI preservada (verified javap-style listing).
- 5 tests Turbine verdes + 1 JNI test existente.
- Bug menor encontrado y arreglado: TurbineAssertionError vs IllegalStateException catch — fix con `awaitError()`.

#### Notas

- 7/17 tasks closed, 37/101 SP (36.6%), en 2 sesiones efectivas.
- AAR 262 → 275 KB (+13 KB por sealed class hierarchies).
- `send()` y full `listen()` Flow se completan en BEE-57.

---

### [2026-04-29] — Closed BEE-55

**Trigger detallado**: BEE-55 cerrada — eliminados 4 directorios `jniLibs/{mips,mips64,armeabi,x86}` + sus `.so` files. Solo quedan las 3 ABIs Google-Play-compatible (arm64-v8a, armeabi-v7a, x86_64).

**Net delta global**: 0 días en fin date. BEE-55 cerró **6 días antes** de su Fin estimado (2026-05-04 → 2026-04-29) pero ese adelanto ya está absorbido por la velocidad acumulada.

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-55: 2026-05-04 → 2026-04-29 (-6 días)

#### Cambios de estado

- BEE-55: `⏳ Pending` → `✅ Done` (2 SP)

#### Hallazgo / lección

- Hipótesis previa (BEE-54 + BEE-1793) era que `defaultConfig.ndk.abiFilters` ya filtraba las 4 ABIs deprecadas del AAR. **Era falso**: `abiFilters` en bloque `ndk{}` solo aplica a código nativo COMPILADO, no a `.so` vendoreados. El delete físico es lo que efectivamente reduce el AAR.
- Resultado: AAR 614 KB → **262 KB** (-57%). Mucho mejor de lo esperado.

#### Notas

- 6/17 tasks cerradas, 29/101 SP, en 2 sesiones efectivas.
- Próxima task: BEE-56 (8 SP) — la primera task de feature real (API pública nueva `BeepingClient`).

---

### [2026-04-29] — Closed BEE-1793 (Scope change: +2 SP)

**Trigger detallado**: BEE-1793 cerrada — task creada durante `/worktree-start` después del cierre de BEE-53 en respuesta al feedback "ok, pero no quiero riesgos". Resuelve los 3 caveats acumulados durante BEE-51..BEE-54: C1 (`kotlinOptions` deprecated → `compilerOptions { jvmTarget = JvmTarget.JVM_17 }`), C2 (sin toolchain resolver → Foojay + `jvmToolchain(17)`), pending-004 (AGP 8.5.2 → 8.7.3 + Gradle 8.7 → 8.10.2).

Ejecución en 2 commits (`da252c3` + `c594b06`) por outage transitorio de Foojay durante el primer intento — ver comentario de la task en Linear.

**Net delta global**: **+2 SP scope** / **0 días** en la fecha fin del milestone (Phase 8 ahora 101 SP / 17 tasks; BEE-1793 ejecutada concurrentemente con BEE-52+54+53 en una ventana de 2 días, no añade calendar time).

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio — R1, R2, R3 vigentes; **R7 eliminado** porque Foojay garantiza JDK 17 toolchain auto-download).

#### Adelantados / Retrasados

- (ninguno) — BEE-1793 fue una task adicional, no movió fechas existentes.

#### Cambios de scope

- **+1 task / +2 SP**: BEE-1793 creada bajo Phase 8 milestone, infra+android labels, priority 3.
- Total Phase 8: 16 tasks / 99 SP → **17 tasks / 101 SP**.

#### Cambios de estado

- BEE-1793: nueva task creada en `In Progress` 2026-04-28T10:50Z → `Done` 2026-04-29T04:03Z.

#### Cambios en pending list

- `pending-004` eliminada (resuelta en BEE-1793).
- `pending-005` creada y eliminada en la misma sesión (Foojay alternative deferred → Foojay re-añadido cuando volvió online).

#### Cambios de estado de riesgo (PRODUCTO.md §19)

- **R7** (JDK 17 mínimo en build excluye dev environments legacy) → **eliminado** ✅. Foojay descarga JDK 17 automáticamente; no requiere instalación manual.

#### Notas

- BEE-1793 fue housekeeping/risk-mitigation, no estaba en el scope original de 16 tasks. Se creó por feedback explícito del usuario tras observar caveats acumulados.
- Foojay outage durante primer intento se resolvió en ~horas. La verificación manual (curl al Disco API) fue clave para no quedar bloqueado.
- 5/17 tasks cerradas, 27/101 SP, en 2 sesiones efectivas.

---

### [2026-04-28] — Closed BEE-53

**Trigger detallado**: BEE-53 cerrada — migración completa Java → Kotlin 2.0 (6 ficheros), 3 ficheros muertos eliminados (WavFile + WavFileException + ApplicationTest), bug AudioFocus duplicado fixed, Timer/TimerTask → coroutines. JNI ABI preservada al 100% (verificado javap). 1 commit (`011f6ec`).

**Net delta global**: **0 días adicionales** (la fecha fin del milestone se mantiene en 2026-05-15 — el adelanto de BEE-53 se computa contra la fecha que ya estaba ajustada para BEE-52+54).

**Nueva fecha fin estimada**: 2026-05-15 (vie) — sin cambio (ya adelantada de 2026-05-18 en el closure anterior).

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio).

#### Adelantados

- BEE-53: 2026-04-30 → 2026-04-28 (-2 días respecto a su Fin estimado original)
- Acumulado del milestone: 25 SP cerrados en 1 sesión calendario vs ~3 días planificados — alta velocidad sostenida

#### Cambios de estado

- BEE-53: `⏳ Pending` → `✅ Done` (13 SP, la task más grande de Phase 8 hasta ahora)

#### Notas

- BEE-53 cerró con 1 ciclo de Human QA (sin rework). El usuario aprobó pero pidió "no riesgos" — los caveats C1 (`kotlinOptions` deprecated), C2 (no toolchain resolver), y `pending-004` (AGP 8.5 → 8.7 + Gradle 8.7 → 8.10) se atajan en una stabilization mini-task antes de BEE-55.
- 4/16 tasks closed, 25 SP, en una sesión efectiva — velocidad observada >>baseline pero no recalibramos hasta 5+ closures.

---

### [2026-04-28] — Closed BEE-52 + BEE-54 (combined execution)

**Trigger detallado**: BEE-52 y BEE-54 cerradas conjuntamente porque Gradle 8.7 (BEE-52 scope) requiere AGP 8.4+ (BEE-54 scope) — no se podían separar técnicamente. Ejecución atómica en un commit (`9104343`).

**Net delta global**: **-3 días** (adelanto). 12 SP cerrados en 1 día calendario vs planificación de ~2 días (BEE-52 estaba para 4-29, BEE-54 para 5-1, ambas cerradas hoy 4-28).

**Nueva fecha fin estimada**: 2026-05-15 (vie) — antes 2026-05-18 (lun).

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio — R1, R2, R3 siguen vigentes; el adelanto no afecta los riesgos externos).

#### Adelantados

- BEE-52: 2026-04-29 → 2026-04-28 (-1 día)
- BEE-54: 2026-05-01 → 2026-04-28 (-3 días, ejecutada fuera de orden secuencial junto con BEE-52)
- Milestone Phase 8 (BEE-66 final): 2026-05-18 → 2026-05-15 (-3 días)

#### Retrasados

- (ninguno)

#### Sin cambio

- BEE-51 ✅ (ya cerrada en sesión anterior)
- BEE-53, BEE-55..BEE-65 ⏳ (forward tasks; sus `Fin est.` individuales NO se recalcularon — ya no aplica orden estricto de identifier ya que BEE-54 se ejecutó antes que BEE-53)

#### Cambios de estado de riesgo

- (ninguno) — R1/R2/R3 siguen idénticos.

#### Notas

- Ejecución conjunta BEE-52+54 fue forzada por dependencia técnica Gradle↔AGP. Documentada en commit message + en ambos comments de Linear.
- 1 ciclo de Human QA Checkpoint (sin rework) para ambas — caso ideal del workflow.
- AndroidX migration mínima realizada (3 imports + dep replacement). La migración full Java→Kotlin sigue siendo BEE-53.
- ABI cleanup parcial via `abiFilters` adelanta parte de BEE-55. El cleanup físico de los `.so` legacy queda pendiente en BEE-55.
- Velocidad observada: **12 SP en 1 sesión de trabajo** (~2 horas calendario) — efectivo ~6× del baseline de 8 SP/día. Esto es atribuible al ejecutor (Claude) y al alcance preciso de las tasks. **Recalibración del baseline NO aplicada** todavía — sample size de 3 cierres es insuficiente y distorsiona estimates downstream del ecosistema (sdk-iphone, beeping-flutter, etc.) que se calibran a velocidad humana.
- 1 follow-up nuevo capturado en `docs/PENDING.md`: `pending-004` (bump AGP 8.5 → 8.7+).

---

### [2026-04-28] — Closed BEE-51

**Trigger detallado**: BEE-51 cerrada el 2026-04-28 — la fecha real coincidió con la estimada (mismo día). Scope refinado durante el work: el "rename" del título original no aplicaba (init creó repo desde cero, no rename); el trabajo real fue commitlint preset + lefthook hooks + CI commitlint job + branch protection en develop+main.

**Net delta global**: **0 días** — milestone Phase 8 sigue terminando 2026-05-18.

**Nueva fecha fin estimada**: 2026-05-18 (lun) — sin cambio.

**Nuevo estado global**: ⚠️ Riesgo medio (sin cambio — R1, R2, R3 siguen vigentes).

#### Adelantados / retrasados

- (ninguno) — BEE-51 cerró exactamente en su `Fin estimado`.

#### Cambios de estado

- BEE-51: `⏳ Pending` → `✅ Done` (2 SP cerrados; 97 SP remaining de 99 totales)

#### Notas

- BEE-51 cerró con **1 ciclo de Human QA Checkpoint** (sin rework) — caso ideal del workflow.
- Branch protection ahora activa en `develop` y `main`: todos los commits futuros deben ir vía PR.
- 3 follow-ups capturados en `docs/PENDING.md` (lefthook pre-commit hooks adicionales, Node 24 migration GH Actions, CI triggers para modo individual).
- Velocidad observada con 1 task: insuficiente para recalibrar — esperar a 3-4 cierres antes de ajustar el `8 SP/día` asumido.

---

### [2026-04-28] — Initial ROADMAP creation

**Trigger**: `/worktree-init` bootstrap — primer ROADMAP del repo.

**Estado global inicial**: ⚠️ Riesgo medio (debido a R1 + R2 documentados en `docs/PRODUCTO.md` §19).

**Fecha fin estimada inicial**: 2026-05-18 (lun) con 20% de margen sobre 99 SP / 8 SP/día.

**Snapshot inicial**:

| # | Milestone | SP | Inicio est. | Fin est. | Estado |
|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | 99 | 2026-04-28 | 2026-05-18 | ⚠️ Riesgo medio |

#### Notas del bootstrap

- El milestone Phase 8 ya existía en Linear (id `cf4da38e-c680-40ba-9194-20d0f075ef73`)
  con 16 tasks (BEE-51..BEE-66) y story points pre-asignados antes de este
  `/worktree-init`. La planificación se basa íntegramente en esos datos
  preexistentes — no se crearon ni eliminaron tasks.
- Velocidad de **8 SP/día** es el default del ecosistema Beeping. No hay
  histórico todavía en este repo para recalibrar; se ajustará tras los
  primeros cierres en `/worktree-start`.
- Margen del **20%** absorbe principalmente R1 (delay de Phase 1) y R3
  (review Sonatype OSSRH). Si materializa R2 (16 KB pages no soportado)
  el margen no será suficiente — se replanteará scope.
- 16 de 16 tasks de Phase 8 carecen de la sección obligatoria `🧑‍🔬 Human QA Checkpoint`,
  y 8 de 16 carecen de `🧪 Automated tests`. Esto se resolverá task-por-task
  durante `/worktree-start` Paso 4 (decisión del usuario en init: opción 5a).
- 1 milestone único (Phase 8) en este repo. Phase 0–7 cerradas o de otros repos.
  Phases 9–21 son sucesoras del ecosistema y no se planifican aquí.

#### Adelantados / retrasados

No aplica — primera entrada del CHANGELOG, no hay versión previa con la que comparar.

#### Cambios de estado de riesgo

No aplica — primera entrada.
