// Commitlint config — extends shared Beeping ecosystem preset.
// Preset rules documented at https://github.com/beeping-io/commitlint-config
export default {
  extends: ['@beeping.io/commitlint-config'],
  rules: {
    // Allow `release(...)` commits — used by the ecosystem's milestone-close /
    // release-promotion squash commits (e.g. `release(phase-8): … v0.2.0`). The
    // shared preset omits it; add it here until it's upstreamed (BEE-2320).
    'type-enum': [
      2,
      'always',
      [
        'feat',
        'fix',
        'docs',
        'style',
        'refactor',
        'perf',
        'test',
        'build',
        'ci',
        'chore',
        'revert',
        'release',
      ],
    ],
  },
};
