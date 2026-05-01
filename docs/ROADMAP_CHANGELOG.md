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
- **Estado global**: ⚠️ Riesgo medio — depende de Phase 1 (`beeping-core`) para releases firmadas (R1) y soporte 16 KB pages (R2)
- **Última actualización**: 2026-05-01 (trigger: `Closed BEE-60`)
- **Story points totales**: 101 SP (Phase 8 — 99 originales + 2 BEE-1793)
- **Story points cerrados**: 54 SP (BEE-51..60 + BEE-1793)
- **Story points remaining**: 47 SP (53.5% completado)
- **Days esfuerzo (con margen)**: 15 días hábiles
- **Fecha fin estimada actualizada**: 2026-05-15 (vie) — sin cambio

| # | Milestone | SP | Inicio est. | Fin est. | Estado |
|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | 99 | 2026-04-28 | 2026-05-18 | ⚠️ Riesgo medio |

---

## 📜 History

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
