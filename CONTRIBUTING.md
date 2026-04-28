# 🤝 Contributing to `beeping-android`

Thanks for your interest! This repo is part of the **Beeping Platform** ecosystem.
The contributing guidelines, commit conventions, branch model, code of conduct,
and PR rules are **shared across the ecosystem** and live in
[`beeping-io/beeping-meta`][meta]. Read those first.

- 📘 [CONVENTIONS.md][conventions] — commits, branches, PRs, Renovate, lint
- 🤝 [CONTRIBUTING.md][contributing] — process, signing, PR template
- 📜 [CODE_OF_CONDUCT.md][coc] — community standards
- 🔒 [SECURITY.md][security] — vulnerability disclosure (also linked from this repo)

---

## ⚡ Repo-specific quick notes

- **Active scope**: Phase 8 of the Beeping Platform Linear project.
  See [`docs/PRODUCTO.md`](docs/PRODUCTO.md) section 6 for the 16 tasks
  (BEE-51..BEE-66, 99 SP).
- **Branch base**: `develop`. `main` is reserved for releases (release-please managed once BEE-66 lands).
- **Stack target**: Kotlin 2.0, AndroidX, AGP 8.5+, Gradle 8.7. Currently legacy Java.
- **Commit format** (Conventional Commits + Linear ID):

  ```
  feat(jni): BEE-57 strategy pattern for LocalEncoder/CloudEncoder
  fix(http): BEE-59 retry on 429 with exponential backoff
  chore(deps): BEE-52 bump Gradle 6.1.1 → 8.7
  ```

- **PR template**: required sections include 🧪 Automated tests + 🧑‍🔬 Human QA evidence — see [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md).
- **Definition of done**: lint clean (ktlint + detekt + Android Lint, **0 warnings**), tests passing, coverage gate, CHANGELOG updated, Linear task closed with comment summarizing changes + QA cycles.

---

## 🔧 Local development

See [`README.md`](README.md) — currently building requires JDK 11 (legacy).
Post-BEE-52, JDK 17+ is required.

[meta]: https://github.com/beeping-io/beeping-meta
[conventions]: https://github.com/beeping-io/beeping-meta/blob/develop/CONVENTIONS.md
[contributing]: https://github.com/beeping-io/beeping-meta/blob/develop/CONTRIBUTING.md
[coc]: https://github.com/beeping-io/beeping-meta/blob/develop/CODE_OF_CONDUCT.md
[security]: https://github.com/beeping-io/beeping-meta/blob/develop/SECURITY.md
