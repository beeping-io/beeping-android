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
- **Última actualización**: 2026-04-28 (trigger: `Closed BEE-51`)
- **Story points totales**: 99 SP (Phase 8)
- **Story points cerrados**: 2 SP (BEE-51)
- **Story points remaining**: 97 SP
- **Days esfuerzo (con margen)**: 15 días hábiles

| # | Milestone | SP | Inicio est. | Fin est. | Estado |
|---|---|---|---|---|---|
| 1 | 🤖 Phase 8 — beeping-android (Kotlin 2.0) | 99 | 2026-04-28 | 2026-05-18 | ⚠️ Riesgo medio |

---

## 📜 History

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
