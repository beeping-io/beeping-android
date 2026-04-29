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
- **Última actualización**: 2026-04-29 (trigger: `Closed BEE-1793` + Scope change: +2 SP)
- **Story points totales**: 101 SP (Phase 8 — 99 originales + 2 BEE-1793)
- **Story points cerrados**: 27 SP (BEE-51, BEE-52, BEE-53, BEE-54, BEE-1793)
- **Story points remaining**: 74 SP (26.7% completado)
- **Days esfuerzo (con margen)**: 15 días hábiles (sin cambio — BEE-1793 ejecutada en paralelo)
- **Fecha fin estimada actualizada**: 2026-05-15 (vie) — sin cambio

| # | Milestone | SP | Inicio est. | Fin est. | Estado |
|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | 99 | 2026-04-28 | 2026-05-18 | ⚠️ Riesgo medio |

---

## 📜 History

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
