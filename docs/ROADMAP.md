# 📅 ROADMAP — `beeping-android`

> **Live document.** Recalculado en cada cierre de tarea, cada cambio de scope,
> o cada recalibración de velocidad. Cualquier actualización dispara una
> entrada nueva en `docs/ROADMAP_CHANGELOG.md` (regla canónica del ecosistema:
> ROADMAP y CHANGELOG van **siempre** en el mismo PR).

---

## 🎯 Snapshot global

| | |
|---|---|
| **Fecha de inicio del proyecto** | 2026-04-28 (mar) |
| **Velocidad asumida** | 8 story points / día hábil |
| **Margen de riesgo** | 20% (planificación defensiva) |
| **Fecha fin estimada (con margen)** | 🔄 **WAVE 2 EN CURSO** — ETA 2026-06-09 (wave 1 cerró 2026-05-16) |
| **Fecha fin sin margen** | wave 2: 2026-06-06 (sáb) |
| **Story points totales** | 159 SP (125 wave 1 + 34 wave 2: 9 contract-parity + 19 C-API-coverage + 5 release + 1 security). BEE-2331 cancelada; example rebuild → BEE-2336 (Phase 10). |
| **Esfuerzo bruto** | 13.75 días hábiles (wave 1) + 4.1 días (wave 2) |
| **Esfuerzo con margen** | wave 1: 17 días hábiles (real: 12 sesiones / 18 días cal.) + wave 2: ~5 días |
| **Estado global** | 🔄 **REABIERTO — wave 2 (C API coverage + contract parity)** desde 2026-06-02. Audit reveló 13 funciones de `beeping-core` v0.8.1 sin exponer + `BeepingPayload.confidence` campo muerto → 4 tasks nuevas (BEE-2313..2316). Más 3 followups de contract-parity con `beeping_flutter`/iOS (BEE-2305/06/07). Wave 1 sigue ✅: `io.beeping:beeping-android:0.0.0` en Maven Central + scheduler API (BEE-2240). BEE-67 deferred a Phase 9. |
| **Última actualización** | 2026-06-02 (trigger: `Scope change — reopen Phase 8 wave 2: +4 C API coverage tasks + 3 contract-parity followups`) |
| **Tasks completadas** | 31 / 34 · **151 SP cerrados de 159 (95%)** · 5 SP wave 2 open (solo release) · BEE-2331 cancelada · BEE-67 (3 SP) deferred a Phase 9 |
| **Velocidad observada** | 122 SP en 12 sesiones (wave 1); recalibración deferred — datos contra ejecutor Claude no representan velocidad humana |

---

## 🛣️ Milestones

`beeping-android` tiene **un solo milestone activo** en el Linear project
🔊 Beeping Platform. La phase entera vive en una branch `milestone/phase-8`
(modo milestone) y se cierra con un único PR a `develop`.

| # | Milestone | Linear ID | SP | Inicio est. | Fin est. (con margen) | Estado |
|---|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | `cf4da38e-c680-40ba-9194-20d0f075ef73` | 161 | 2026-04-28 | wave 2 ETA 2026-06-10 | 🔄 In Progress (wave 2) |

> Phases 0–7 ya están cerradas o pertenecen a otros repos del ecosistema.
> Phases 9–21 son sucesoras (sdk iOS, Flutter, RN, web, server-side, refs apps,
> docs, marketing, launch readiness). Ninguna depende exclusivamente del
> cierre de Phase 8 — pero Phase 10 (Flutter plugin) y Phase 12 (RN) sí
> consumen el AAR publicado en Maven Central.

---

## 📋 Phase 8 — desglose de tareas

Orden por dependencia (sortOrder Linear ascendente). Cada `Fin est.` se
calcula como `ceil(cumSP / velocidad × (1 + margen))` working days desde
`2026-04-28`. Reality check: las tareas no se ejecutan necesariamente
en este orden — esto es budget de planificación, no allocation rígida.

| # | Linear | Título | SP | cumSP | Fin est. | Estado |
|---|---|---|---|---|---|---|
| 1 | [BEE-51](https://linear.app/me8/issue/BEE-51) | 🏷️ Rename `sdk-android` → `beeping-android` + Apache-2.0 + Conventional Commits | 2 | 2 | 2026-04-28 (mar) | ✅ Done |
| 2 | [BEE-52](https://linear.app/me8/issue/BEE-52) | ⬆️ Migración a Gradle 8.7 + Kotlin DSL + version catalogs | 5 | 7 | 2026-04-28 (mar) | ✅ Done |
| 3 | [BEE-53](https://linear.app/me8/issue/BEE-53) | 🔄 Migración completa Java → Kotlin 2.0 + AndroidX | 13 | 20 | 2026-04-28 (mar) | ✅ Done |
| 4 | [BEE-54](https://linear.app/me8/issue/BEE-54) | 🎯 AGP 8.5+ + NDK r27 + compileSdk 35 + targetSdk 35 + minSdk 24 + 16 KB pages | 5 | 25 | 2026-04-28 (mar) | ✅ Done |
| 4b | [BEE-1793](https://linear.app/me8/issue/BEE-1793) | 🛠️ Build chain stabilization (Foojay + AGP 8.7 + Gradle 8.10) — **scope addition durante execution** | 2 | 27 | 2026-04-29 (mié) | ✅ Done |
| 5 | [BEE-55](https://linear.app/me8/issue/BEE-55) | ✂️ ABIs cleanup: solo arm64-v8a + armeabi-v7a + x86_64 | 2 | 29 | 2026-04-29 (mié) | ✅ Done |
| 6 | [BEE-56](https://linear.app/me8/issue/BEE-56) | 🌊 API pública nueva: `BeepingClient` instance-based + `Flow<BeepingEvent>` + suspend | 8 | 35 | 2026-04-29 (mié) | ✅ Done |
| 7 | [BEE-57](https://linear.app/me8/issue/BEE-57) | 🎭 Strategy pattern: `LocalEncoder` (JNI) + `CloudEncoder` (Ktor) | 8 | 43 | 2026-04-30 (jue) | ✅ Done |
| 8 | [BEE-58](https://linear.app/me8/issue/BEE-58) | 🛠️ Builder con `BeepingMode.LOCAL` / `BeepingMode.CLOUD(apiKey, endpoint)` | 3 | 46 | 2026-04-30 (jue) | ✅ Done |
| 9 | [BEE-59](https://linear.app/me8/issue/BEE-59) | 🔌 Cliente HTTP generado desde OpenAPI (Ktor + kotlinx.serialization) | 3 | 49 | 2026-05-01 (vie) | ✅ Done |
| 10 | [BEE-60](https://linear.app/me8/issue/BEE-60) | 🪵 Logging Timber + JSON sink + trace-ID propagation | 3 | 52 | 2026-05-01 (vie) | ✅ Done |
| 11 | [BEE-61](https://linear.app/me8/issue/BEE-61) | 📡 Telemetry hook con opt-out + tests de privacy | 5 | 57 | 2026-05-01 (vie) | ✅ Done |
| 12 | [BEE-62](https://linear.app/me8/issue/BEE-62) | 🧪 Tests: JUnit5 + MockK + Robolectric + Kotest property + Paparazzi snapshots + Pitest mutation | 13 | 70 | 2026-05-01 (vie) | ✅ Done |
| 13 | [BEE-63](https://linear.app/me8/issue/BEE-63) | 🧼 ktlint + detekt + Android Lint strict en CI | 3 | 73 | 2026-05-01 (vie) | ✅ Done |
| 13b | [BEE-1815](https://linear.app/me8/issue/BEE-1815) | 🧪 Split CloudEncoder E2E into DEV/PROD opt-in + auto-load `.env.local` — **scope addition durante execution** | 1 | 74 | 2026-05-04 (lun) | ✅ Done |
| 14 | [BEE-64](https://linear.app/me8/issue/BEE-64) | 📱 Sample app rewrite con Jetpack Compose + debug console | 8 | 82 | 2026-05-14 (jue) | ✅ Done |
| 15 | [BEE-65](https://linear.app/me8/issue/BEE-65) | 🔗 Consumir `beeping-core` via GitHub Releases (no `.so` vendoreados) — **scope narrowed durante QA: download + verify + package + load** | 5 | 87 | 2026-05-11 (lun) | ✅ Done |
| 15b | [BEE-2226](https://linear.app/me8/issue/BEE-2226) | 🔧 JNI shim layer + wire `LocalEncoder.encode()` + verify decode end-to-end — **scope addition descubierto durante QA BEE-65** | 5 | 92 | 2026-05-11 (lun) | ✅ Done |
| 15c | BEE-67 *(deferred → Phase 9)* | 📱 Sample pivot listener-only + `scripts/send-beep` Mac-side — **deferred a Phase 9 (sample no requerido para cierre técnico Phase 8)** | 3 | 95 | — | 🔜 Phase 9 |
| 16 | [BEE-66](https://linear.app/me8/issue/BEE-66) | 📦 Maven Central publishing (Sonatype Central Portal + GPG signed + sources.jar + javadoc.jar) | 13 | 108 | 2026-05-12 (lun) | ✅ Done |
| 17 | [BEE-2248](https://linear.app/me8/issue/BEE-2248) | 🤖 Remove JNI mkdir+chdir workaround for beeping-core logs — **followup post-v0.0.0 unblock by `beeping-core` v0.8.1 (BEE-2227)** | 2 | 110 | 2026-05-15 (vie) | ✅ Done |
| 18 | [BEE-2234](https://linear.app/me8/issue/BEE-2234) | 🧪 QA SDK en dispositivo físico (`scripts/send-beep.sh`) — **partial-closed founder fiat: runtime validated, scripts + docs + matrix deferred** | 3 | 113 | 2026-05-15 (vie) | ✅ Done |
| 19 | [BEE-2235](https://linear.app/me8/issue/BEE-2235) | 🧪 QA SDK en emulador (`beeping-www /test` page) — **partial-closed founder fiat: runtime validated, `/test` page + docs deferred** | 5 | 118 | 2026-05-15 (vie) | ✅ Done |
| 20 | [BEE-2240](https://linear.app/me8/issue/BEE-2240) | 🤖 Expose beep scheduler (`computeBeepSchedule` + `sendScheduled`) — **followup post-v0.0.0 unblock by `beeping-core` v0.8.1 (BEE-2238)** | 5 | 123 | 2026-05-16 (sáb) | ✅ Done |
| | **Totales wave 1** | | **125** | **122** (BEE-67 deferred) | **2026-05-16** | |

### Wave 2 — C API coverage + contract parity (reabierto 2026-06-02)

Audit `beeping-android` ↔ `beeping-core` v0.8.1: 13 funciones del C API sin exponer + `BeepingPayload.confidence` campo muerto. Orden de ejecución: BEE-2307 primero (founder fiat), resto del milestone después. La nueva API es **aditiva** (no breaking) — Phase 9+ siguen consumiendo el AAR ya publicado.

| # | Linear | Título | SP | Tipo | Estado |
|---|---|---|---|---|---|
| 21 | [BEE-2307](https://linear.app/me8/issue/BEE-2307) | 🐛 Emitir `BeepingError.AudioFocusLost` en pérdida real de foco de audio | 2 | contract parity | ✅ Done |
| 22 | [BEE-2306](https://linear.app/me8/issue/BEE-2306) | 🔗 Inyección de trace-id externo en `BeepingClient.Builder` (X-Trace-Id e2e) | 2 | contract parity | ✅ Done |
| 23 | [BEE-2305](https://linear.app/me8/issue/BEE-2305) | 🎚️ Exponer encoding mode (3 modos reales: AUDIBLE/NON_AUDIBLE/ALL) en Builder + send() + listen() | 5 | contract parity | ✅ Done |
| 24 | [BEE-2313](https://linear.app/me8/issue/BEE-2313) | 📊 Exponer reception metrics (confidence/error/noise/volume/mode) en `BeepingEvent.Decoded` | 8 | C API coverage | ✅ Done |
| 25 | [BEE-2314](https://linear.app/me8/issue/BEE-2314) | 🗓️ Scheduled-payload decode (`ParseScheduledPayload` + `GetDecodedScheduledPayload`) | 5 | C API coverage | ✅ Done |
| 26 | [BEE-2315](https://linear.app/me8/issue/BEE-2315) | 🔧 Diagnostics: decoding frequency range + core version | 3 | C API coverage | ✅ Done |
| 27 | [BEE-2316](https://linear.app/me8/issue/BEE-2316) | 🎛️ Advanced config: `SetAudioSignature` + `SetLogPath` (paridad iOS) + skip `Reset` | 3 | C API coverage | ✅ Done |
| 28 | [BEE-2320](https://linear.app/me8/issue/BEE-2320) | 🚀 Cut & publish wave-2 release a Maven Central (release-please + GPG) — *blocked by 21–27* | 3 | release ops | ⏳ Pending |
| 29 | [BEE-2321](https://linear.app/me8/issue/BEE-2321) | ✅ Verify wave-2 release end-to-end (Maven Central consumible + smoke) — *blocked by 28* | 2 | release ops | ⏳ Pending |
| 30 | [BEE-2326](https://linear.app/me8/issue/BEE-2326) | 🔒 Bump `fast-uri` ≥3.1.2 — resolver 2 Dependabot high (host confusion + path traversal) | 1 | security | ✅ Done |
| 31 | [BEE-2331](https://linear.app/me8/issue/BEE-2331) | 📱 ~~Sample app: selector de encoding mode~~ | 2 | sample/QA | ❌ Canceled (superseded por BEE-2336, Phase 10) |
| — | [BEE-2336](https://linear.app/me8/issue/BEE-2336) | 📱 Rebuild example app como copia del de beeping_flutter | 8 | example | 🌅 Phase 10 (diferido) |
| | **Totales wave 2** | | **34** | | **8 cerrados (2307, 2306, 2305, 2313, 2314, 2315, 2316, 2326) / 5 open (solo release)** |

> **Release gating**: BEE-2320 (cut+publish) tiene relaciones `blocks` desde las 7 features → no arranca hasta cerrarlas. BEE-2321 (verify) `blocked by` BEE-2320. El topological sort de `/tasks` las mantiene al final automáticamente.

> **Nota** sobre fechas forward: las `Fin est.` de BEE-53, BEE-55..BEE-65 NO se han recalculado individualmente tras el cierre de BEE-52+54 — sólo la fecha fin del milestone (BEE-66) se ajusta con el adelanto de -3 días (combined effect). Recalcular forward dates per-task **no aporta valor** porque ya no se ejecutan estrictamente en orden de identifier (BEE-54 se cerró antes que BEE-53, por ejemplo). El budget de 15 días totales se mantiene como referencia.

### Estados individuales

- ⏳ **Pending** — task aún no empezada (default al init).
- 🚧 **In Progress** — alguna persona o agente trabajando activamente.
- 🔍 **In Review** — PR abierto, esperando review/CI.
- ✅ **Done** — Linear cerrado, comentario de cambios + ciclos QA registrados.
- ⚠️ **At Risk** — task atrasada respecto a su `Fin est.` (>1 día) sin razón documentada.
- ❌ **Blocked** — bloqueada por dependencia externa (anotar cuál).

---

## 🚦 Indicadores de riesgo

| ID | Riesgo | Estado actual | Mitigación activa |
|---|---|---|---|
| R1 | `beeping-core` no libera releases firmadas a tiempo (afecta BEE-65) | ⚠️ medio — Phase 1 está "next" en Linear, sin start date | Fallback temporal: usar los `.so` vendoreados que ya están en el repo hasta que Phase 1 cierre |
| R2 | 16 KB page size no soportado en `beeping-core` (afecta BEE-54 + BEE-65 + BEE-66) | 🔴 **alto** — `beeping-core v0.6.0` (latest) **no publica Android NDK builds** (solo linux/macos/wasm/windows). Verificado 2026-05-07 durante QA BEE-64: emulator API 37 falla con `program alignment (8192) cannot be smaller than system page size (16384)`. BEE-65 bloqueada hasta abrir + cerrar task previa en `beeping-core` repo: "Publish Android NDK `.so` artifacts (arm64-v8a + armeabi-v7a + x86_64) with `-Wl,-z,max-page-size=16384`" | Abrir issue en `beeping-core` antes de empezar BEE-65. Mientras tanto, sample app legacy queda usable solo en CLOUD mode |
| R3 | Sonatype OSSRH staging delay (review humano 2-4 semanas) | 🟡 bajo-medio — proceso conocido | Iniciar trámite OSSRH en BEE-51 (no esperar a BEE-66). GitHub Releases firmadas como fallback siempre disponibles |
| R4 | Ktor binary size impacta tamaño AAR | 🟢 bajo | Si AAR > 1 MB tras BEE-59, evaluar OkHttp + manual JSON |
| R5 | Telemetry opt-out filtra datos sin opt-in | 🟢 bajo (pero high impact si ocurre) | BEE-61 incluye tests automáticos de privacy + audit |
| R8 | Pitest mutation testing demasiado lento en CI | 🟢 bajo | Si pipeline > 15 min, mover Pitest a job nightly |

(IDs alineados con `docs/PRODUCTO.md` §19. R6 y R7 omitidos por ser N/A en este momento.)

---

## 🔁 Cómo se mantiene este ROADMAP

Per `~/.claude/CLAUDE.md` y `/worktree-init` Paso 9:

1. **Cada vez que se cierra una tarea de Phase 8** (`/worktree-start` Paso 6):
   - Recalcular fechas siguientes (cascade adelantos/retrasos).
   - Re-evaluar indicadores de riesgo.
   - Actualizar `docs/ROADMAP_CHANGELOG.md` con entrada nueva al inicio de la sección History.
   - Commit conjunto `ROADMAP.md` + `ROADMAP_CHANGELOG.md` en el mismo PR.

2. **Cualquier cambio de scope** (añadir/quitar BEE task, ajustar SP, cambiar velocidad):
   - Mismo flujo, trigger en CHANGELOG = `Scope change` o `Velocity recalibration`.

3. **Nunca actualizar uno sin el otro.** Si se descubre desfase, crear entrada de "reconciliation" explicando el porqué.

---

## 📎 Referencias

- Spec del producto: `docs/PRODUCTO.md`
- Histórico append-only: `docs/ROADMAP_CHANGELOG.md`
- Linear milestone: <https://linear.app/me8/project/03da887d924e?selectedProjectMilestone=cf4da38e-c680-40ba-9194-20d0f075ef73>
- Global methodology: `~/.claude/CLAUDE.md`
