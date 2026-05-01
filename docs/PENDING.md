# ⏳ Pending

Captura **trabajo conocido pero aún sin fecha** — el "lo haremos algún día pero
no ahora".

🎯 **Aquí entra**: deuda detectada, follow-ups de incidentes, feedback accionable,
"esto hay que hacerlo pero no hemos decidido cuándo".
🚫 **Aquí NO entra**: trabajo ya agendado a un milestone (eso va a Linear).

🪄 **Promoción**: ponerle un milestone a un pending lo convierte en task Linear
`BEE-XXXX` y se elimina automáticamente de este fichero.

---

## 📋 Cómo añadir un pending

Usa el skill `/pending` (recomendado). O copia este bloque al final del fichero:

```markdown
### ⏳ pending-NNN — [Título corto]

- 📅 **Fecha añadida**: YYYY-MM-DD
- 🏷️ **Tipo**: feat | fix | docs | refactor | chore | infra | security | test
- 🧭 **Trigger**: por qué se añadió (incidente, feedback, deuda)
- ⚙️ **Acción requerida**: qué hay que hacer concretamente
- 🚧 **Bloqueado por**: (si aplica) algo o alguien que retrasa
- 🚦 **Estado**: 🆕 Nuevo
```

### 🏷️ Tipos disponibles

| Tipo | Cuándo usarlo |
|------|---------------|
| `feat` | Funcionalidad nueva |
| `fix` | Bug fix |
| `docs` | Solo documentación |
| `refactor` | Refactor sin cambio de comportamiento |
| `chore` | Mantenimiento, deps, config |
| `infra` | Infraestructura, CI/CD |
| `security` | Cuestiones de seguridad |
| `test` | Solo tests |

### 🚦 Estados posibles

| Iconito | Estado | Significado |
|---------|--------|-------------|
| 🆕 | Nuevo | Recién capturado, sin triage |
| 🔍 | En triage | Decidiendo prioridad / scope |
| 📋 | Promovido | Ya es task Linear (`BEE-XXXX`) — debería haberse eliminado de aquí |
| 🚧 | Bloqueado | Esperando algo externo (especificar) |
| ❌ | No procede | Decidido no avanzar (apuntar el porqué) |

---

## 🗂️ Pendientes registrados

### ⏳ pending-001 — Habilitar lefthook pre-commit hooks (gitleaks + markdownlint + yaml-lint + json-lint + trailing-whitespace + eof-newline + large-files)

- 📅 **Fecha añadida**: 2026-04-28
- 🏷️ **Tipo**: infra
- 🧭 **Trigger**: BEE-51 integró lefthook con sólo dos hooks (`commit-msg` para
  commitlint y `pre-push` para protect-base-branches). El config canónico de
  `beeping-meta/lefthook.yml` también tiene 7 hooks pre-commit (gitleaks
  secret scan, markdownlint, yaml/json validation, trailing-whitespace cleanup,
  eof-newline, large-files block) que aportan valor pero estaban fuera de
  scope de BEE-51 (no en sus entregables).
- ⚙️ **Acción requerida**: portar los 7 pre-commit hooks de
  `beeping-meta/lefthook.yml` a `lefthook.yml` de este repo, instalar las
  herramientas necesarias (`brew install gitleaks markdownlint-cli`) en el
  setup local + en CI (los hooks se degradan a warning si no están). Documentar
  en `README.md` los requisitos de tooling extra.
- 🚧 **Bloqueado por**: nada bloquea — el momento natural es cuando se añadan
  ficheros que activen alguno de esos hooks (e.g. al añadir más `.yml` configs
  con BEE-52, o más markdown con BEE-66 para Maven Central docs).
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-002 — Migrar GitHub Actions a Node 24 (deadline: 2026-09-16)

- 📅 **Fecha añadida**: 2026-04-28
- 🏷️ **Tipo**: infra
- 🧭 **Trigger**: durante `/worktree-init` Paso 5, el primer run de CI
  (`25036153289`) emitió warning: las versiones `@v4` de `actions/checkout`,
  `setup-java`, `cache`, `upload-artifact` y `setup-node` corren sobre Node 20,
  que GitHub deprecó en Sep 2025 y forzará a Node 24 por defecto el 2026-06-02
  (con removal del Node 20 runner el 2026-09-16). Hoy las acciones siguen
  funcionando, pero el clock está corriendo.
- ⚙️ **Acción requerida**: bumpear cada acción usada a la versión que soporta
  Node 24 oficialmente (probablemente `@v5` o posterior cuando lleguen) o,
  como puente temporal, añadir `env: FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`
  al workflow. Validar CI verde tras el cambio. Aplicable a `.github/workflows/ci.yml`
  y a futuros workflows.
- 🚧 **Bloqueado por**: que las acciones publiquen versiones Node-24-ready. La
  mayoría ya lo soportan a través del flag de override; las versiones `@v5` con
  Node 24 nativo van llegando.
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-007 — Cerrar HttpClient interno del ApiClient generado (resource cleanup)

- 📅 **Fecha añadida**: 2026-05-01
- 🏷️ **Tipo**: chore
- 🧭 **Trigger**: durante BEE-59 se integró el cliente Ktor generado por openapi-generator. El template del generator declara `private val client: HttpClient by lazy { ... }` en el `ApiClient` parent — accesso privado, sin método público para cerrarlo. Como resultado `CloudEncoder.close()` queda como no-op respecto al HttpClient. En Android la GC + lifecycle del proceso reclama el cliente eventualmente, pero técnicamente es un leak menor.
- ⚙️ **Acción requerida**: dos opciones:
  - (a) Esperar a que openapi-generator exponga `client` como protected/public (tracker upstream) y entonces sobrescribir close() para llamarlo.
  - (b) Usar un custom Mustache template para el `ApiClient.kt` que exponga el client. Más invasivo pero independiente del upstream.
- 🚧 **Bloqueado por**: prioridad — leak es bounded por proceso lifecycle. Aplica si añadimos un usecase que cree/destruya muchas instancias de `BeepingClient` (e.g., test runner, multi-tenant).
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-006 — Cloud-mode live decoding (CloudEncoder.decoded() via AudioRecord chunking)

- 📅 **Fecha añadida**: 2026-04-30
- 🏷️ **Tipo**: feat
- 🧭 **Trigger**: durante BEE-57 se implementó `CloudEncoder.encode()` (POST `/v1/encode` real, verificado contra dev Cloud Run URL) pero `CloudEncoder.decoded()` quedó como `emptyFlow()` stub. La razón: live decoding cloud requiere capturar mic vía `AudioRecord`, chunkear en ventanas de ~500ms, y cyclic-POST a `/v1/decode` con cada chunk hasta detectar un beep. Es un protocol no-trivial que justifica una task separada.
- ⚙️ **Acción requerida**: diseñar el chunking strategy (window size, overlap, energy-based VAD para no enviar silencio), implementar `CloudEncoder.decoded()` con `AudioRecord` + `Flow` que postea chunks al endpoint, manejar errores (no audio, mala calidad, timeout). Tests con MockEngine que simula respuestas 200 con/sin decoded result. Considerar streaming bidirectional si beepbox-server lo soporta en el futuro (WebSocket para evitar cyclic POST overhead).
- 🚧 **Bloqueado por**: nada — diseño interno. Considerar si los casos de uso reales (alta latencia, alto bandwidth) justifican el coste; muchos productos usan Cloud solo para encode y Local para decode (hybrid mode). Si decidimos que cloud-decode no se necesita, esta entry se descarta.
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-008 — Pitest mutation testing en módulo JVM separado

- 📅 **Fecha añadida**: 2026-05-01
- 🏷️ **Tipo**: test
- 🧭 **Trigger**: durante BEE-62 se intentó aplicar `info.solidsoft.pitest` plugin al módulo `:AndroidBeepingCore`. El plugin requiere el `java`/`java-library` Gradle plugin y no compone con `com.android.library` — la extension `pitest {}` no se registra y el build falla. Pitest sigue siendo valor alto (mutation score guard) pero requiere arquitectura diferente.
- ⚙️ **Acción requerida**: crear un módulo JVM-only `:tests-mutation` con `java-library` + `info.solidsoft.pitest`, que dependa del AAR (vía `implementation(project(":AndroidBeepingCore"))`) y corra mutation contra los `.kt` de `main/`. Targets: ≥60% inicial, ≥70% en hardening. Job manual `./gradlew :tests-mutation:pitest` (no en CI critical path para no inflar tiempos). Alternativa: investigar `pitest-android` community plugin si llega a soportar AGP 8.7.
- 🚧 **Bloqueado por**: prioridad — Kover ya da line/branch coverage. Mutation es valor incremental, no crítico de Phase 8.
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-009 — Paparazzi snapshots tests sobre Composables del sample app

- 📅 **Fecha añadida**: 2026-05-01
- 🏷️ **Tipo**: test
- 🧭 **Trigger**: BEE-62 deferred Paparazzi porque `:AndroidBeepingCore` no expone Composables (es un library SDK puro, sin UI). El sample app (`:app`) tiene un manifest vacío hoy y se reescribe en BEE-64 con Compose + debug console.
- ⚙️ **Acción requerida**: durante BEE-64, añadir Paparazzi (`app.cash.paparazzi:paparazzi:1.3.5`) al sample `:app` y escribir snapshot tests para las pantallas principales (encode form, decode listener, debug console). Threshold inicial: 1 snapshot per main composable. Con Paparazzi NO necesitamos emulator — corre sobre LayoutLib.
- 🚧 **Bloqueado por**: BEE-64 (sample app rewrite con Compose).
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-010 — Espresso/Compose UI instrumented tests + Codecov integration

- 📅 **Fecha añadida**: 2026-05-01
- 🏷️ **Tipo**: test
- 🧭 **Trigger**: BEE-62 deferred (a) Espresso/Compose-UI instrumented tests porque requieren un emulador o device farm — Robolectric cubre el 99% de casos sin esa complejidad — y (b) Codecov upload porque requiere `CODECOV_TOKEN` secret en GitHub Actions todavía no configurado. Coverage XML ya se genera y sube como artifact.
- ⚙️ **Acción requerida**: cuando BEE-64 entregue el sample app, añadir:
  - 1-2 instrumented tests críticos en `:app` con Compose UI Test + Espresso, corridos en GitHub Actions Android emulator (`reactivecircus/android-emulator-runner`).
  - Configurar `CODECOV_TOKEN` repo secret + upload `kover/report.xml` con `codecov/codecov-action@v4` en CI workflow. Threshold gradual 70% → 80% → 90% líneas conforme suite crece.
- 🚧 **Bloqueado por**: BEE-64 (sample app necesario para Espresso) + decisión sobre Codecov vs alternativas (sonarcloud, github-native).
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-003 — Añadir `feat/**`, `fix/**`, etc. a triggers de CI (modo individual)

- 📅 **Fecha añadida**: 2026-04-28
- 🏷️ **Tipo**: infra
- 🧭 **Trigger**: BEE-51 modificó `.github/workflows/ci.yml` añadiendo
  `milestone/**` a los push triggers para que CI corra durante el modo
  milestone activo (Phase 8). Si en el futuro trabajamos en modo individual
  (rama por task: `feat/bee-N-...`), esos triggers no cubren las branches de
  feature individuales y CI no correrá hasta abrir el PR.
- ⚙️ **Acción requerida**: añadir `feat/**`, `fix/**`, `chore/**`, `docs/**`,
  `refactor/**`, `test/**`, `ci/**`, `perf/**`, `build/**`, `style/**` a la
  lista de push triggers en `ci.yml`. Alternativa más simple: cambiar a
  `push:` sin `branches:` para que dispare en cualquier rama (más barato
  cognitivamente, ligeramente más caro en compute si se hacen pushes muy
  frecuentes en ramas locales experimentales).
- 🚧 **Bloqueado por**: que decidamos cambiar a modo individual para alguna task.
- 🚦 **Estado**: 🆕 Nuevo
