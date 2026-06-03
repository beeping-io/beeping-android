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
- 🧭 **Trigger**: BEE-62 deferred Paparazzi porque `:AndroidBeepingCore` no expone Composables (es un library SDK puro, sin UI). El sample app (`:app`) **se borró 2026-06-03** y se reconstruirá como copia del example de `beeping_flutter` (BEE-2336, Phase 10).
- ⚙️ **Acción requerida**: durante BEE-2336 (rebuild del example), añadir Paparazzi (`app.cash.paparazzi:paparazzi:1.3.5`) al nuevo módulo example y escribir snapshot tests para las pantallas principales. Threshold inicial: 1 snapshot per main composable. Con Paparazzi NO necesitamos emulator — corre sobre LayoutLib.
- 🚧 **Bloqueado por**: BEE-2336 (rebuild del example app).
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-010 — Espresso/Compose UI instrumented tests + Codecov integration

- 📅 **Fecha añadida**: 2026-05-01
- 🏷️ **Tipo**: test
- 🧭 **Trigger**: BEE-62 deferred (a) Espresso/Compose-UI instrumented tests porque requieren un emulador o device farm — Robolectric cubre el 99% de casos sin esa complejidad — y (b) Codecov upload porque requiere `CODECOV_TOKEN` secret en GitHub Actions todavía no configurado. Coverage XML ya se genera y sube como artifact.
- ⚙️ **Acción requerida**: cuando BEE-2336 entregue el example app reconstruido, añadir:
  - 1-2 instrumented tests críticos en el módulo example con Compose UI Test + Espresso, corridos en GitHub Actions Android emulator (`reactivecircus/android-emulator-runner`).
  - Configurar `CODECOV_TOKEN` repo secret + upload `kover/report.xml` con `codecov/codecov-action@v4` en CI workflow. Threshold gradual 70% → 80% → 90% líneas conforme suite crece.
- 🚧 **Bloqueado por**: BEE-2336 (example app necesario para Espresso) + decisión sobre Codecov vs alternativas (sonarcloud, github-native).
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

### ⏳ pending-011 — Añadir cosign verify-blob al downloadBeepingCore task (cuando BEE-2225 cierre upstream)

- 📅 **Fecha añadida**: 2026-05-09
- 🏷️ **Tipo**: security
- 🧭 **Trigger**: BEE-65 implementa SHA256-only verify del download de `beeping-core` releases porque el workflow upstream solo emite `.sig` sin `.bundle` ni cert. La verificación cosign keyless local es imposible sin uno de los dos. Detectado durante BEE-65 al intentar `cosign verify-blob` contra `beeping-core v0.8.0`.
- ⚙️ **Acción requerida**: cuando [BEE-2225](https://linear.app/me8/issue/BEE-2225) (Phase 1, `beeping-core`) cierre y emita `.cosign-bundle` por artifact, abrir task en `beeping-android` para añadir `cosign verify-blob --bundle <file>.cosign-bundle --certificate-identity-regexp '...beeping-core...' --certificate-oidc-issuer 'https://token.actions.githubusercontent.com'` a `DownloadBeepingCoreTask`. Bumpear versión `beepingCore` en `libs.versions.toml` a la primera release con bundles.
- 🚧 **Bloqueado por**: BEE-2225 upstream en `beeping-core`.
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-013 — Instrumented tests para LocalEncoder + JNI shim (encode/decode round-trip)

- 📅 **Fecha añadida**: 2026-05-11
- 🏷️ **Tipo**: test
- 🧭 **Trigger**: BEE-2226 entregó el JNI shim + wire encode/decode; el plan original incluía `androidTest/` instrumented suite (`LocalEncoderInstrumentedTest`, `RoundTripInstrumentedTest`, `JniShimLoadTest`). Software-side el path está validado por emulator QA (Send genera WAV 184 KB, Listen abre AudioRecord, no crashes) + el nuevo `SdkPlumbingTest` instrumented añadido en BEE-2226. Pero CI todavía no corre instrumented tests automáticamente — necesitamos `connectedCheck` en GitHub Actions con emulator headless API 35+ para gatear este nivel de confianza en cada push.
- ⚙️ **Acción requerida**: añadir workflow `.github/workflows/instrumented.yml` usando `reactivecircus/android-emulator-runner@v2` con `api-level: 35`, ejecutando `./gradlew :AndroidBeepingCore:connectedDebugAndroidTest`. Añadir gate en branch protection rules. La infra de tests (deps + `SdkPlumbingTest`) ya quedó en BEE-2226.
- 🚧 **Bloqueado por**: capacity (no urgente; emulator QA manual + ejecución local cubren la validación).
- 🚦 **Estado**: 🆕 Nuevo

### ⏳ pending-014 — Restaurar strict round-trip assertion en SdkPlumbingTest cuando BEE-2228 cierre upstream

- 📅 **Fecha añadida**: 2026-05-11
- 🏷️ **Tipo**: test
- 🧭 **Trigger**: durante BEE-2226 el test `SdkPlumbingTest` se diseñó para asertar "SDK plumbing alive" (start token detected + DECODE_COMPLETE reached + getDecodedData callable sin crash) pero NO el exact char round-trip. Razón: `BEEPING_EncodeDataToAudioBuffer` + `BEEPING_DecodeAudioBuffer` no producen un round-trip limpio en in-process feed (el decoder depende de la "función de transferencia" del micrófono físico — AGC, anti-aliasing, ruido térmico — para alinear correctamente el spectral grid). Encoder produce "abc12" → decoder devuelve `null` (integrity fail) o `"faic00cll"` determinista. Tracked upstream en [BEE-2228](https://linear.app/me8/issue/BEE-2228).
- ⚙️ **Acción requerida**: cuando BEE-2228 cierre y publique nueva release de `beeping-core` con in-process round-trip funcional, (1) bumpear `beepingCore` en `gradle/libs.versions.toml`, (2) reactivar la assertion strict en `SdkPlumbingTest.kt`: `assertEquals("abc12", decoded?.trimEnd(' '))`, (3) confirmar verde en emulator.
- 🚧 **Bloqueado por**: [BEE-2228](https://linear.app/me8/issue/BEE-2228) upstream en `beeping-core`.
- 🚦 **Estado**: 🆕 Nuevo
