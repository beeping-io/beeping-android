# 🤖 beeping-android — Documento de producto

> Source of truth del scope y timeline del SDK Android del ecosistema Beeping.
> Vive junto al código (regla taxonomía global: code-adjacent docs → git).
> Cualquier cambio de scope dispara entrada en `docs/ROADMAP_CHANGELOG.md`.

---

## 1 · 📌 Información básica

| Campo | Valor |
|---|---|
| **Nombre** | `beeping-android` |
| **Tag** | 🤖 |
| **Versión actual** | `0.0.0` (regla 0.x del ecosistema Beeping) |
| **Fecha de inicio** | 2026-04-28 |
| **License** | Apache-2.0 |
| **GitHub** | `beeping-io/beeping-android` (público) |
| **Branch base** | `develop` |
| **Linear project** | 🔊 Beeping Platform · id `a83369a5-3cb8-4fca-932d-ee33f6a7a00e` |
| **Linear milestone** | 🤖 Phase 8 — beeping-android (Kotlin 2.0) · id `cf4da38e-c680-40ba-9194-20d0f075ef73` |
| **Distribución** | Maven Central (`io.beeping:beeping-android`) + GitHub Releases (AAR firmado) |

---

## 2 · ❓ Qué es

**`beeping-android` es el SDK oficial Android del ecosistema Beeping**: una librería Kotlin que permite a apps Android **codificar y decodificar datos transmitidos por sonido** (audible + ultrasónico), usando el core C++ `beeping-core` via JNI o el servidor HTTP `beepbox-server` via Ktor.

Expone una API pública Kotlin moderna (instance-based, `Flow`-based, suspend) y se distribuye como AAR firmado en Maven Central.

---

## 3 · 🎯 Objetivo medible

Publicar **`io.beeping:beeping-android` v0.0.0` en Maven Central** completando los 16 tasks de Phase 8 (BEE-51..BEE-66, 99 story points totales) con:

- ✅ Migración Java → Kotlin 2.0 + AndroidX completa
- ✅ Toolchain moderno (AGP 8.5+, Gradle 8.7+, NDK r27, compileSdk 35, 16 KB page size)
- ✅ API pública nueva `BeepingClient` con `Flow<BeepingEvent>` + suspend
- ✅ Strategy pattern dual-mode (Local JNI + Cloud Ktor)
- ✅ Tests: JUnit5 + MockK + Robolectric + Kotest property + Paparazzi snapshots + Pitest mutation
- ✅ Sample app Jetpack Compose con debug console
- ✅ AAR firmado (GPG) publicado coordinadamente Maven Central + GitHub Releases
- ✅ Lint strict (ktlint + detekt + Android Lint) verde en CI

---

## 4 · 🚧 Restricciones clave

- **minSdk 24** (Android 7.0, ~98% del parque actual)
- **targetSdk / compileSdk 35** (Android 15)
- **16 KB page size** support obligatorio (requisito Android 15+)
- **JVM 17+** para el build
- **ABIs soportadas únicamente**: `arm64-v8a`, `armeabi-v7a`, `x86_64` (drop legacy `mips`, `mips64`, `armeabi`, `x86`)
- **Native libs NO vendoreados**: `libbeepingcore.so` se descarga en build-time desde GitHub Releases de `beeping-core` (BEE-65)
- **API pública estable a partir de 1.0.0**: en `0.x` la API puede romper entre minor bumps
- **Apache-2.0 obligatorio**: regla ecosistema, no negociable
- **Conventional Commits + commitlint preset compartido** (`@beeping-io/commitlint-config`)
- **Branch protection** en `develop` y `main`: PR + 1 review + linear history + signed commits + sin force push

---

## 5 · 🌐 Entorno y distribución

### Plataformas soportadas
- Android 7.0+ (API 24) en arm64-v8a, armeabi-v7a, x86_64

### Modos runtime (dual-mode strategy)
- **`BeepingMode.LOCAL`** → encoding/decoding en el dispositivo via JNI a `libbeepingcore.so`. No requiere red. Latencia mínima. Privacy-first.
- **`BeepingMode.CLOUD(apiKey, endpoint)`** → encoding/decoding via HTTP POST a `beepbox-server` (`POST /v1/encode`, `POST /v1/decode`). Requiere red + API key. Útil para devices low-end o feature gating.

### Canales de distribución
- **Primario**: Maven Central → `implementation("io.beeping:beeping-android:<version>")`
- **Secundario**: GitHub Releases con AAR + sources.jar + javadoc.jar firmados (GPG + cosign keyless cuando esté disponible)
- **NO**: JitPack, jcenter (muerto desde 2022), GitHub Packages

---

## 6 · ✅ Alcance — qué incluye

Phase 8 (16 tasks, 99 SP). Orden por dependencia (sortOrder Linear ascendente):

| # | Linear | SP | Título |
|---|---|---|---|
| 1 | BEE-51 | 2 | 🏷️ Rename `sdk-android` → `beeping-android` + Apache-2.0 + Conventional Commits |
| 2 | BEE-52 | 5 | ⬆️ Migración a Gradle 8.7 + Kotlin DSL + version catalogs |
| 3 | BEE-53 | 13 | 🔄 Migración completa Java → Kotlin 2.0 + AndroidX |
| 4 | BEE-54 | 5 | 🎯 AGP 8.5+ + NDK r27 + compileSdk 35 + targetSdk 35 + minSdk 24 + 16 KB pages |
| 5 | BEE-55 | 2 | ✂️ ABIs cleanup: solo arm64-v8a + armeabi-v7a + x86_64 |
| 6 | BEE-56 | 8 | 🌊 API pública nueva: `BeepingClient` instance-based + `Flow<BeepingEvent>` + suspend |
| 7 | BEE-57 | 8 | 🎭 Strategy pattern: `LocalEncoder` (JNI) + `CloudEncoder` (Ktor) |
| 8 | BEE-58 | 3 | 🛠️ Builder con `BeepingMode.LOCAL` / `BeepingMode.CLOUD(apiKey, endpoint)` |
| 9 | BEE-59 | 3 | 🔌 Cliente HTTP generado desde OpenAPI (Ktor + kotlinx.serialization) |
| 10 | BEE-60 | 3 | 🪵 Logging Timber + JSON sink + trace-ID propagation |
| 11 | BEE-61 | 5 | 📡 Telemetry hook con opt-out + tests de privacy |
| 12 | BEE-62 | 13 | 🧪 Tests: JUnit5 + MockK + Robolectric + Kotest property + Paparazzi snapshots + Pitest mutation |
| 13 | BEE-63 | 3 | 🧼 ktlint + detekt + Android Lint strict en CI |
| 14 | BEE-64 | 8 | 📱 Sample app rewrite con Jetpack Compose + debug console |
| 15 | BEE-65 | 5 | 🔗 Consumir `beeping-core` via GitHub Releases (no `.so` vendoreados) |
| 16 | BEE-66 | 13 | 📦 Maven Central publishing (Sonatype OSSRH + GPG signed + sources.jar + javadoc.jar) |
|   | **Total** | **99** | |

---

## 7 · 🚫 Qué NO incluye

- ❌ App de producción end-user (eso es `beeply` Flutter / Phase 18)
- ❌ SDK iOS (`beeping-ios` / Phase 9)
- ❌ SDK Flutter (`beeping_flutter` / Phase 10)
- ❌ SDK React Native (`beeping-react-native` / Phase 12)
- ❌ Server-side SDK Node/Python (Phase 13)
- ❌ Web/WASM SDK (`beeping-web` / Phase 11)
- ❌ Backend HTTP server (`beepbox` / Phase 2 — done)
- ❌ Core C++ (`beeping-core` / Phase 1 — next)
- ❌ Marketing site (`beeping-www` / Phase 20)
- ❌ Docs hub (`beeping-docs` / Phase 19)
- ❌ Soporte Android <7.0 (API <24)
- ❌ Soporte ABIs `mips`, `mips64`, `armeabi`, `x86` (deprecadas desde NDK r17)
- ❌ Distribución via JitPack o jcenter
- ❌ Backwards-compat con la API legacy `BeepingCore(Context)` + `BeepingCoreEvent`

---

## 8 · 🔁 Cambios de alcance

Cualquier alteración (añadir/quitar tarea de Phase 8, ajustar story points, recalibrar velocidad) **DEBE** disparar:

1. Edición de la descripción de Phase 8 en Linear (o issue/task individual).
2. Recálculo de `docs/ROADMAP.md`.
3. Nueva entrada al inicio de la sección History de `docs/ROADMAP_CHANGELOG.md` con trigger `Scope change` o `Velocity recalibration`.
4. Commit conjunto `ROADMAP.md` + `ROADMAP_CHANGELOG.md` en el mismo PR.

Regla canónica del ecosistema: **el ROADMAP y su CHANGELOG nunca van por separado.**

---

## 9 · 🧭 Principios de producto

1. **Public API instance-based, no singletons.** Lifecycle controlado por el consumer; testeable sin shenanigans.
2. **Coroutines + `Flow` first-class.** Callbacks Java legacy quedan fuera; cualquier consumer Java accede via interop bridges.
3. **Privacy-first.** Telemetry opt-out por defecto; cero PII en logs; redaction por contrato (BEE-61).
4. **Dual-mode invisible al consumer.** Mismo `BeepingClient` API independientemente de Local vs Cloud — la decisión es declarativa al construir.
5. **Open source Apache-2.0.** Sin features behind closed source; pricing/auth se hace fuera (en `beepbox-server`).
6. **Distribución por package manager oficial.** Cero "git+url://" en producción.
7. **Zero state global.** Cualquier estado vive en la instancia.
8. **Strict mode por defecto en CI.** ktlint + detekt + Android Lint con cero warnings — no "0 nuevos", sino **0 totales**.

---

## 10 · 🌊 Flujo principal

```kotlin
// 1) Consumer añade dependencia
// implementation("io.beeping:beeping-android:0.0.0")

// 2) Construye BeepingClient declarando el modo
val client = BeepingClient.Builder(context)
    .mode(BeepingMode.Local)  // o BeepingMode.Cloud(apiKey = "...", endpoint = "https://api.beeping.io")
    .build()

// 3) Listen — Flow caliente que emite eventos del decoder
viewModelScope.launch {
    client.listen()
        .collect { event ->
            when (event) {
                is BeepingEvent.Started -> /* ... */
                is BeepingEvent.Decoded -> handle(event.payload)
                is BeepingEvent.Failed  -> handle(event.reason)
                BeepingEvent.Stopped    -> /* ... */
            }
        }
}

// 4) Encode + emit (suspend)
val audioBytes = client.encode("HOLA1")  // returns ByteArray PCM
client.play(audioBytes)

// 5) Stop
client.stop()
```

Internamente:
- **LOCAL**: `LocalEncoder` invoca JNI a `libbeepingcore.so` (descargado de releases de `beeping-core` en build-time).
- **CLOUD**: `CloudEncoder` usa Ktor HTTP a `POST /v1/encode` / `POST /v1/decode` con API key.

---

## 11 · ⚠️ Estados y errores

### Estados públicos (sealed class `BeepingEvent`)

- `BeepingEvent.Started` — la sesión arrancó (mic permission, audio focus OK, JNI/HTTP listo).
- `BeepingEvent.Decoded(payload: BeepingPayload)` — un beep válido se ha decodificado.
- `BeepingEvent.Failed(reason: BeepingError)` — error recuperable o no recuperable.
- `BeepingEvent.Stopped` — sesión cerrada (manualmente o por error fatal).

### Tipos de error (sealed class `BeepingError`)

- `MissingMicPermission` (recuperable: el consumer pide permission y reintenta)
- `AudioFocusLost` (recuperable: el consumer espera y reintenta)
- `NativeLibraryNotLoaded` (no recuperable en LOCAL: faltan libs nativas)
- `NetworkError(cause)` (recuperable en CLOUD: retry con backoff)
- `AuthenticationFailed` (no recuperable en CLOUD: API key inválida)
- `RateLimited(retryAfter)` (recuperable en CLOUD)
- `DecoderInternal(cause)` (no recuperable: bug del SDK)

### Excepciones

`BeepingException` jerarquía sellada — sólo se lanzan desde funciones suspend que retornan `Result<T>` o equivalente. Nunca se filtran a través de `Flow` (van como `Failed` event).

---

## 12 · 📐 Requisitos no funcionales

| Categoría | Requisito |
|---|---|
| **Tests cobertura** | ≥ 80% line coverage en `:AndroidBeepingCore` (target: BEE-62) |
| **Tests calidad** | Pitest mutation score ≥ 70% en módulo SDK |
| **CI tiempo** | < 10 min build + test + lint full pipeline |
| **APK tamaño** | AAR < 500 KB (excluyendo native libs); native libs < 2 MB por ABI |
| **Latencia decode** | < 200 ms desde fin de chirp hasta emisión de `BeepingEvent.Decoded` (LOCAL) |
| **Latencia decode** | < 1500 ms (CLOUD) — depende de RTT |
| **Memory** | Steady-state < 30 MB durante listen |
| **Privacy** | Cero PII en logs, telemetry opt-out por defecto, audit anual de eventos emitidos |
| **A11y** | Sample app pasa Accessibility Scanner sin issues bloqueantes |
| **i18n** | Sample app: EN + ES |
| **Reliability** | Maneja audio focus loss, mic permission revoked at runtime, network drop, JNI lib unload |

---

## 13 · 🛠️ Stack técnico

### Build & toolchain
- **Kotlin 2.0** (target stack)
- **AGP 8.5+** + **Gradle 8.7+** + **Kotlin DSL** + **version catalogs**
- **NDK r27** + **CMake** (para JNI bridge)
- **JVM 17+**
- **AndroidX** (jetpack libs only)

### Runtime
- **kotlinx.coroutines + Flow**
- **kotlinx.serialization** (JSON)
- **Ktor client** (CloudEncoder)
- **Timber** (structured logging) + JSON sink custom
- **JNI** → `libbeepingcore.so` (de `beeping-core` releases)

### Tests
- **JUnit5** (Jupiter) + **MockK** + **Robolectric** (unit/JVM)
- **Kotest** (property-based)
- **Paparazzi** (Compose screenshot snapshots)
- **Pitest** (mutation testing)
- **AndroidX Test** + **Espresso** (instrumented)

### Lint & format
- **ktlint** + **detekt** + **Android Lint** (strict, 0 warnings)

### Publishing & release
- **Vanniktech Maven Publish plugin** → **Sonatype OSSRH** → **Maven Central**
- **GPG signing** (sources.jar + javadoc.jar + AAR + module metadata)
- **release-please** (automated SemVer + changelog + tag)
- **cosign keyless** (Sigstore) cuando esté disponible
- **GitHub Releases** con SBOM (CycloneDX) y SLSA L3 provenance

---

## 14 · 🧩 Componentes principales

### Módulo `:AndroidBeepingCore` (la SDK library)

| Clase / interface | Rol | Tipo |
|---|---|---|
| `BeepingClient` | Punto de entrada público, instance-based | `class` |
| `BeepingClient.Builder` | DSL de construcción + validación | `class` |
| `BeepingMode` | `LOCAL` / `CLOUD(apiKey, endpoint)` | sealed `class` |
| `BeepingEvent` | `Started` / `Decoded` / `Failed` / `Stopped` | sealed `class` |
| `BeepingPayload` | Datos decodificados + metadata (mode, timestamp, confidence) | data `class` |
| `BeepingError` | Jerarquía de errores | sealed `class` |
| `Encoder` | Interface strategy | `interface` |
| `LocalEncoder` | Implementación JNI a `libbeepingcore` | internal `class` |
| `CloudEncoder` | Implementación Ktor a beepbox-server | internal `class` |
| `BeepingApi` (generated) | OpenAPI client para beepbox-server | generated |
| `TelemetryHook` | Hook opt-in con event emit | `interface` |

### Módulo `:app` (sample app)

> ⚠️ **Removido 2026-06-03.** El módulo `:app` se borró; el example app se
> reconstruirá como copia del example de `beeping_flutter` una vez el plugin de
> Flutter esté terminado (BEE-2336, Phase 10). La descripción de abajo es la
> referencia de qué debe cubrir el example reconstruido.

App Compose + Material 3 demostrativa que:
- Lista los modos (LOCAL / CLOUD) seleccionables.
- Muestra el debug console con eventos `BeepingEvent` en tiempo real.
- Permite simular permission denied / audio focus loss / network drop.
- Tiene smoke E2E con AndroidX Test.
- **NO** es una app de producción (eso es `beeply` / Phase 18).

---

## 15 · 🔗 Integraciones externas

| Integración | Para qué | Cómo |
|---|---|---|
| **`beeping-core`** | Native lib `libbeepingcore.so` | Download desde GitHub Releases por ABI en build-time (BEE-65) |
| **`beepbox-server`** | Endpoints `/v1/encode`, `/v1/decode`, `/v1/healthz` | Ktor HTTP client generado de OpenAPI 3.1 (BEE-59) |
| **Sonatype OSSRH** | Staging Maven Central | Vanniktech plugin (BEE-66) |
| **Maven Central** | Distribución pública | Promote desde OSSRH staging |
| **release-please** | Releases automatizados | GitHub Action |
| **GitHub Actions** | CI/CD | `.github/workflows/ci.yml` |
| **Sigstore (cosign)** | Firmado keyless | Step en release workflow (cuando ConanCenter/SLSA L3 estén ready) |

---

## 16 · 🧪 Decisiones técnicas

### DT-01: API instance-based en vez de singleton
**Por qué**: La API legacy usaba `BeepingCore(Context)` con state interno y casts implícitos del Context al callback interface. Frágil para tests, lifecycle confuso, no permite múltiples sesiones. Instance-based + builder permite testing puro, lifecycle controlado, y multi-session si se desea.

### DT-02: `Flow<BeepingEvent>` en vez de listener interface
**Por qué**: Flow integra con coroutines + structured concurrency. Cancelación automática con scope, backpressure declarativo, transformaciones encadenables. Listeners Java se exponen via interop helper opcional.

### DT-03: Strategy pattern Local/Cloud transparente
**Por qué**: Permite switching declarativo via `BeepingMode` sin cambiar el código consumer. Local minimiza red + privacy; Cloud habilita feature gating, low-end devices, y telemetry centralizada. La factoría es interna; el consumer no instancia `Encoder` directamente.

### DT-04: Maven Central como canal primario (no JitPack/jcenter)
**Por qué**: jcenter cerró en 2022. JitPack no firma artifacts ni soporta provenance. Maven Central es el estándar Android, requiere GPG signing, y permite publish coordinado con semver.

### DT-05: Native libs consumidas, no vendoreadas
**Por qué**: Los `.so` actualmente vendoreados en `jniLibs/` son de Jul 2020, sin trazabilidad de versión. Consumirlos de releases firmadas de `beeping-core` da: trazabilidad (versión pin en `gradle.properties`), seguridad (firma + SBOM), y workflow consistente con sdk-iphone (Phase 9).

### DT-06: Kotlin 2.0 sin compatibilidad Java legacy
**Por qué**: La API actual Java usa `com.android.support` (deprecated) y casts a Activity. Refactor preserva el JNI bridge pero todo el código de capa Kotlin se reescribe. Java consumers acceden via interop estándar (PerformanceJUnit del compilador Kotlin).

### DT-07: Telemetry opt-out con auditoría
**Por qué**: Privacy es principio del producto. Opt-out por defecto, tests automáticos verifican que ningún evento sale sin opt-in. Hook `TelemetryHook` permite al consumer inyectar su propio sink (Sentry, Firebase, Datadog, etc.).

### DT-08: 16 KB page size obligatorio
**Por qué**: Android 15+ requiere apps + libs nativas con 16 KB page support. Si `beeping-core` no lo soporta, **bloqueamos** Phase 8 hasta que `beeping-core` (Phase 1) lo libere. Riesgo trackeado en sección 19.

---

## 17 · 👥 Usuarios target

### Primario
**Desarrolladores Android Kotlin** integrando data-over-sound en sus apps. Casos de uso conocidos:
- Intercambio de tarjetas de contacto en eventos / conferencias / networking
- Pairing de devices (POS, kiosk, IoT)
- Watermarking de TV ads / second-screen experiences
- Proximity 2FA / unlock
- Multiplayer local sin red
- Cupones in-store / wallet / loyalty

### Secundario
**Desarrolladores Android Java** legacy — acceso via interop generado del compilador Kotlin. No prioritario.

### Anti-target
- Devs que necesitan transmisión de archivos grandes (Beeping es para payloads pequeños, ~5-9 chars).
- Apps requiriendo Android <7.0.

---

## 18 · 📊 Métricas de éxito

| Métrica | Target | Cuándo medir |
|---|---|---|
| **Phase 8 completada** | 16/16 tasks closed | Fin de Phase 8 |
| **Maven Central live** | `io.beeping:beeping-android:0.0.0` resoluble | Tras BEE-66 |
| **CI green ratio** | ≥ 95% en `develop` | Continuo |
| **Lint warnings** | 0 totales | Continuo (BEE-63) |
| **Coverage** | ≥ 80% line | Tras BEE-62 |
| **Mutation score** | ≥ 70% Pitest | Tras BEE-62 |
| **Sample app E2E** | Verde Local + Cloud | Tras BEE-64 |
| **AAR size** | < 500 KB (excl. native) | Continuo en CI |
| **Build time (cold)** | < 5 min | Tras BEE-52 |
| **Build time (incremental)** | < 30 s | Continuo |

---

## 19 · ⚡ Riesgos y mitigaciones

| # | Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|---|
| R1 | **`beeping-core` releases delay** — BEE-65 depende de que Phase 1 entregue native libs firmadas en GH Releases | Media | Alto (bloquea Phase 8) | Fallback temporal: usar los `.so` vendoreados actuales hasta que Phase 1 entregue. Documentar versión pin con `// TODO BEE-65: replace with release pin` |
| R2 | **16 KB page size en `beeping-core`** | Media | Alto (bloquea release Maven Central) | Coordinar con Phase 1; si no llega, marcar build con `android:largeHeap` workaround documentado y acelerar Phase 1 |
| R3 | **Maven Central onboarding (review humano)** — Sonatype OSSRH puede tardar 2-4 semanas en aprobar coordenadas `io.beeping` | Alta | Medio (retrasa publish, no Phase 8) | Iniciar trámite en BEE-51 (no en BEE-66). GitHub Releases con AAR firmado disponible siempre como fallback (regla global "nunca bloquear ecosistema") |
| R4 | **Ktor binary size impacta AAR** | Baja | Medio | Si AAR > 1 MB, evaluar OkHttp + manual JSON. Decisión en BEE-59 |
| R5 | **Telemetry opt-out edge cases** filtran datos por error | Baja | Alto (privacy breach) | BEE-61 incluye tests automáticos de privacy: assertion de zero events sin opt-in |
| R6 | **API legacy consumers downstream** rompen con el rewrite | Baja | Bajo | No hay consumers downstream activos del SDK legacy (no publicado en Maven). Nuevo SDK = nuevo coordinates `io.beeping:beeping-android` |
| R7 | **JDK 17 mínimo en build excluye dev environments** legacy | Baja | Bajo | Documentar JDK 17 requisito en `README.md`; CI matrix sólo JDK 17 |
| R8 | **Pitest mutation testing demasiado lento en CI** | Media | Bajo | Si pipeline > 15 min, mover Pitest a job nightly con threshold gate más bajo en PR |

---

## 20 · 📅 Timeline

Detalle vivo en `docs/ROADMAP.md` (snapshot fechado por el motor de scheduler) + history en `docs/ROADMAP_CHANGELOG.md`.

**Resumen al 2026-04-28** (init):
- **1 milestone**: Phase 8 — beeping-android (Kotlin 2.0)
- **16 tasks**: BEE-51..BEE-66
- **99 story points** totales
- **Velocidad asumida**: 8 SP/día (default ecosistema)
- **Esfuerzo bruto**: 99 / 8 = ~12.4 días hábiles
- **Con 20% margen de riesgo**: ~15 días hábiles
- **Fecha fin estimada**: 2026-05-19 (lun) si arrancamos hoy y trabajamos lineal

Estado al init: **✅ En tiempo** (sin tareas cerradas todavía).

Cualquier task que se cierre antes/después de su SP estimado **dispara recálculo** del ROADMAP + entrada en CHANGELOG (regla canónica `/worktree-start` Paso 7).

---

## 📎 Referencias

- Global methodology: `~/.claude/CLAUDE.md`
- Beeping Platform Linear project: `https://linear.app/me8/project/03da887d924e`
- Phase 8 milestone: `https://linear.app/me8/project/03da887d924e?selectedProjectMilestone=cf4da38e-c680-40ba-9194-20d0f075ef73`
- Conventions (commit + branch + PR + Renovate): `beeping-io/beeping-meta` → `CONVENTIONS.md`
- Brand kit: `beeping-io/beeping-meta` → `brand/`
- Code of Conduct + Security: `beeping-io/beeping-meta` → `CODE_OF_CONDUCT.md`, `SECURITY.md`
