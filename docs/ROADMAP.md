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
| **Fecha fin estimada (con margen)** | 2026-05-15 (vie) — adelantada -3 días respecto al snapshot inicial |
| **Fecha fin sin margen** | 2026-05-13 (mié) |
| **Story points totales** | 99 SP (Phase 8) |
| **Esfuerzo bruto** | 12.4 días hábiles |
| **Esfuerzo con margen** | 14.85 → **15 días hábiles** |
| **Estado global** | ⚠️ **Riesgo medio** — depende de Phase 1 (`beeping-core`) para releases firmadas (R1) y soporte 16 KB pages (R2). Ver `docs/PRODUCTO.md` §19. |
| **Última actualización** | 2026-05-01 (trigger: `Closed BEE-62`) |
| **Tasks completadas** | 13 / 17 (BEE-51..62 + BEE-1793) · 72 SP cerrados de 101 (71.3%) |
| **Velocidad observada** | 72 SP en 4 sesiones (13 closures); recalibración deferred — datos contra ejecutor Claude no representan velocidad humana |

---

## 🛣️ Milestones

`beeping-android` tiene **un solo milestone activo** en el Linear project
🔊 Beeping Platform. La phase entera vive en una branch `milestone/phase-8`
(modo milestone) y se cierra con un único PR a `develop`.

| # | Milestone | Linear ID | SP | Inicio est. | Fin est. (con margen) | Estado |
|---|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | `cf4da38e-c680-40ba-9194-20d0f075ef73` | 99 | 2026-04-28 | 2026-05-18 | ⚠️ Riesgo medio |

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
| 13 | [BEE-63](https://linear.app/me8/issue/BEE-63) | 🧼 ktlint + detekt + Android Lint strict en CI | 3 | 73 | 2026-05-12 (mar) | ⏳ Pending |
| 14 | [BEE-64](https://linear.app/me8/issue/BEE-64) | 📱 Sample app rewrite con Jetpack Compose + debug console | 8 | 81 | 2026-05-14 (jue) | ⏳ Pending |
| 15 | [BEE-65](https://linear.app/me8/issue/BEE-65) | 🔗 Consumir `beeping-core` via GitHub Releases (no `.so` vendoreados) | 5 | 86 | 2026-05-14 (jue) | ⏳ Pending |
| 16 | [BEE-66](https://linear.app/me8/issue/BEE-66) | 📦 Maven Central publishing (Sonatype OSSRH + GPG signed + sources.jar + javadoc.jar) | 13 | 101 | 2026-05-15 (vie) | ⏳ Pending |
| | **Totales** | | **101** | **101** | **2026-05-15** | |

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
| R2 | 16 KB page size no soportado en `beeping-core` (afecta BEE-54 + BEE-66) | ⚠️ medio — depende de Phase 1 | Coordinar con Phase 1; si bloquea, acelerar Phase 1 antes de Phase 8 BEE-66 |
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
