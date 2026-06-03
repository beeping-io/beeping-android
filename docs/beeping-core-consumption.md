# 🔗 beeping-core consumption

> Cómo `beeping-android` consume las bibliotecas nativas de
> [`beeping-core`](https://github.com/beeping-io/beeping-core).
> Owner: BEE-65. Touch this file when bumping the upstream version or
> changing the verification strategy.

---

## TL;DR

- **No hay `.so` checked-in** en este repo. Los 3 `libbeepingcore.so`
  (`arm64-v8a`, `armeabi-v7a`, `x86_64`) se descargan desde
  GitHub Releases de `beeping-core` durante el build.
- **Versión** declarada en `gradle/libs.versions.toml` →
  `beepingCore = "0.8.0"`.
- **Verificación**: SHA256 contra el `SHA256SUMS.txt` publicado en la
  misma release. Cosign signature verify está deferred — ver
  [pending-011](PENDING.md) y [BEE-2225](https://linear.app/me8/issue/BEE-2225) upstream.
- **Cache**: el download es up-to-date si la versión y los archivos
  extraídos no cambian. Builds incrementales no re-descargan.

## Cómo funciona

El task `:AndroidBeepingCore:downloadBeepingCore` corre como
dependencia de `preBuild`, así que cualquier `assembleDebug`,
`test`, `lint`, etc., lo dispara automáticamente. Pasos:

1. Descarga `https://github.com/beeping-io/beeping-core/releases/download/v<version>/SHA256SUMS.txt`.
2. Por cada ABI en `["arm64-v8a", "armeabi-v7a", "x86_64"]`:
   - Descarga `beeping-core-android-<abi>.tar.zst` al cache local.
   - Verifica SHA256 contra el `SHA256SUMS.txt` ya descargado.
     **Falla el build si no coincide.**
   - Extrae con `tar -xf` (auto-detect zstd via libarchive ≥3.5 / GNU tar ≥1.31).
   - Mueve `./lib/libbeepingcore.so` →
     `build/intermediates/beeping-core/<abi>/libbeepingcore.so`.
   - Descarta `lib/cmake/` e `include/` (no usados por Android runtime).
3. `android.sourceSets["main"].jniLibs.srcDirs` apunta a
   `build/intermediates/beeping-core/`. AGP empaqueta los `.so`
   en el AAR como si fueran `src/main/jniLibs/`.

## Bumpear la versión

1. Editar `gradle/libs.versions.toml`:
   ```toml
   beepingCore = "0.9.0"  # nueva versión
   ```
2. `./gradlew :AndroidBeepingCore:downloadBeepingCore` (limpia cache si
   versión cambió, descarga nueva).
3. Verificar que `build/intermediates/beeping-core/<abi>/libbeepingcore.so`
   existe para los 3 ABIs.
4. `./gradlew :AndroidBeepingCore:check :AndroidBeepingCore:assembleDebug` verde.
5. QA en emulator + device físico (al menos un device con Android 14+
   para 16 KB pages).
6. Commit con `chore(deps): BEE-XXXX bump beeping-core 0.8.0 → 0.9.0`.

## Requisitos del entorno

| Tool | Versión mínima | Por qué |
|---|---|---|
| `tar` | bsdtar 3.5+ o GNU tar 1.31+ | auto-detect de zstd |
| `zstd` | cualquiera (libarchive lo invoca) | descomprimir `.tar.zst` |
| Internet | durante primer build / version bump | descargar release artifacts |

CI (GitHub Actions ubuntu-latest) trae GNU tar 1.34+ y zstd preinstalado.
Macs con Sonoma+ vienen con bsdtar 3.5+. Si tu entorno no los tiene,
`brew install zstd` / `apt-get install zstd` resuelve.

## Por qué no hay cosign verify aún

El release workflow de `beeping-core` (commit `9ea0546`) firma con
`cosign sign-blob --yes --output-signature` que solo emite el
archivo `.sig`, sin certificado adjunto. La verificación cosign
keyless local requiere `--certificate <file>` o `--bundle <file>`
además de la signature.

Tracked:
- [BEE-2225](https://linear.app/me8/issue/BEE-2225) upstream — cambiar a `--bundle`.
- [pending-011](PENDING.md) en este repo — añadir verify cuando upstream cierre.

Mientras tanto, SHA256SUMS.txt provee garantía de integridad: el
release está firmado con cosign (aunque no podamos verificarlo
localmente sin el cert), y `SHA256SUMS.txt` también está firmado
(`SHA256SUMS.txt.sig` en la release). Si alguien manipulara los
artifacts tendría que también modificar el SHA256SUMS.txt y
regenerar la firma cosign — la cadena de confianza está intacta
end-to-end aunque la verify-side localmente solo cubra integridad.

## Tamaño de los `.so`

Heads-up para BEE-66 (Maven Central):

| ABI | Tamaño | Vs legacy 2020 |
|---|---|---|
| `arm64-v8a` | 23 MB | 250 KB |
| `armeabi-v7a` | 17 MB | 170 KB |
| `x86_64` | 22 MB | 282 KB |

Crecimiento ~100×. El AAR resultante pesa ~62 MB sin minify. Para
`beeping-android` publicado en Maven Central, evaluar:

- ABI splits (un AAR por ABI).
- App Bundle (Play Store split) — cada user descarga solo su ABI.
- Strip symbols release (`-Wl,--strip-all` en beeping-core release build).
- Profile-guided optimization para reducir code size.

## Troubleshooting

### `tar: Unrecognized archive format`

Tu `tar` no soporta zstd. Bumpear a bsdtar 3.5+ o GNU tar 1.31+, o
instalar `zstd` y exportar `TAR_OPTIONS='--use-compress-program=zstd'`.

### `SHA256 mismatch for beeping-core-android-X.tar.zst`

El download se corrompió. Borra el cache y reintenta:
```bash
rm -rf AndroidBeepingCore/build/intermediates/beeping-core/.cache
./gradlew :AndroidBeepingCore:downloadBeepingCore
```

Si persiste, la release upstream pudo haber sido reemplazada (raro,
re-cuts sí pasan). Verifica manualmente contra
`https://github.com/beeping-io/beeping-core/releases/tag/v<version>`.

### `UnsatisfiedLinkError: program alignment ... cannot be smaller than system page size`

Significa que el `.so` no se compiló con `-Wl,-z,max-page-size=16384`.
Es un bug en el release upstream; abrir issue en
[`beeping-core`](https://github.com/beeping-io/beeping-core/issues).
El último checked working: v0.8.0 cierra
[BEE-2221](https://linear.app/me8/issue/BEE-2221) que añadió el flag.

### Build offline post-cache

Una vez la cache está poblada (`build/intermediates/beeping-core/.cache/`),
el task se queda como UP-TO-DATE en builds subsiguientes —
no requiere internet. Puedes confirmar con `--dry-run`:

```bash
./gradlew :AndroidBeepingCore:downloadBeepingCore --dry-run
```

Si dice `:downloadBeepingCore SKIPPED`, todo bien. Si dice
`EXECUTED`, algún input cambió (típicamente la versión).
