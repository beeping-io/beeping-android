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

### ⏳ pending-004 — Bump AGP 8.5.2 → 8.7+ + Gradle 8.7 → 8.10+ (silenciar warning compileSdk 35)

- 📅 **Fecha añadida**: 2026-04-28
- 🏷️ **Tipo**: chore
- 🧭 **Trigger**: durante BEE-52 + BEE-54 se eligió AGP 8.5.2 (cumple "AGP 8.5+" del scope) + Gradle 8.7 (lo que pedía BEE-52). Build pasa pero AGP 8.5.2 emite WARNING al usar `compileSdk = 35` (tested up to 34). Bump a AGP 8.7+ requiere Gradle 8.9+. Funciona perfecto pero se queda con un warning ruidoso.
- ⚙️ **Acción requerida**: bumpear `agp = "8.5.2"` → `"8.7.3"` o más reciente en `gradle/libs.versions.toml` + actualizar el wrapper a Gradle 8.10.2 (necesita pasar SHA256 explícito al `./gradlew wrapper --gradle-version 8.10.2 --distribution-type bin --gradle-distribution-sha256-sum <SHA>`). SHA256 de las distros: `https://gradle.org/release-checksums/`. Validar build verde + warning desaparecido.
- 🚧 **Bloqueado por**: nada — trabajo de housekeeping. Bueno empotrarlo durante BEE-53 (Java→Kotlin migration) que va a tocar build.gradle.kts igualmente.
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
