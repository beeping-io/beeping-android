## 📋 Summary

<!-- 1-3 bullets describing what changes and why. The "what" should be
visible in the diff; here describe motivation and any non-obvious choices. -->

-

## 🔗 Linear

<!-- One line per task being closed by this PR. Example: Closes BEE-57 -->

Closes BEE-

## 🧪 Automated tests

<!-- Describe concretely what tests were added / updated, and how they
exercise the change. Either point at the new test files or paste a
short summary of cases covered. -->

- [ ] Unit tests added / updated
- [ ] Mutation score still ≥ 70% (Pitest)
- [ ] All existing tests still pass locally + in CI
- [ ] Coverage gate (≥ 80% line on `:AndroidBeepingCore`) still met

## 🧑‍🔬 Human QA Checkpoint

<!-- For any task with observable effect (UI, copy, audio, layout, theme,
behaviour visible to a human). Pasted from the Linear task QA section.

Skipped only if the task is pure infra / test-only / docs-only / dep
update with no observable effect — declare the reason explicitly. -->

**Steps to validate**:

1.
2.

**Expected outcome**:

-

**QA evidence** (screenshot, video clip, log excerpt):

<!-- attach below -->

## ✅ Definition of Done checklist

- [ ] Lint clean — ktlint + detekt + Android Lint, **0 warnings total**
- [ ] Tests passing locally + in CI (incl. mutation score gate post-BEE-62)
- [ ] `CHANGELOG.md` updated under `## [Unreleased]`
- [ ] If scope/velocity changed: `docs/ROADMAP.md` + `docs/ROADMAP_CHANGELOG.md` updated **in this same PR**
- [ ] Conventional Commits with Linear task ID in commit subject(s)
- [ ] Branch protection requirements met (signed commits, conversations resolved)
- [ ] Linear task moved to "In Review" with comment summarising changes + QA cycles

---

🤖 _Beeping Platform methodology — see `~/.claude/CLAUDE.md` and `CLAUDE.md` for context._
