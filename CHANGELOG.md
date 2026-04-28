# 📝 Changelog

All notable changes to **`beeping-android`** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **0.x rule** — While in `0.x` the public API may break between minor bumps.
> Coordinated bump to `1.0.0` happens once the Beeping Platform reaches
> launch readiness (Phase 21). See `docs/PRODUCTO.md`.

---

## [Unreleased]

### Added

- Bootstrap of the repository under `beeping-io/beeping-android` (2026-04-28).
- `docs/PRODUCTO.md` — product spec, scope of Phase 8 (16 tasks, 99 SP).
- `docs/ROADMAP.md` + `docs/ROADMAP_CHANGELOG.md` — live timeline.
- `docs/IDEAS.md` + `docs/PENDING.md` — cross-project capture (canonical template).
- Apache-2.0 LICENSE.
- Conventional Commits + Keep a Changelog conventions adopted.

### Notes

- Repository starts at version `0.0.0` per the Beeping ecosystem `0.x` rule.
- Existing source is **legacy 2020-era Java** (AGP 4.0.1, `jcenter()`, Android
  support library, vendored `.so` libs) preserved as starting point.
- Modernization to Kotlin 2.0 + AndroidX + AGP 8.5+ + Maven Central distribution
  happens task-by-task in Phase 8 (BEE-51..BEE-66).
