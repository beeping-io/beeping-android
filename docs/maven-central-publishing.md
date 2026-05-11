# 📦 Maven Central publishing

> How `beeping-android` is published as `io.beeping:beeping-android:X.Y.Z` to
> Maven Central via the Sonatype Central Portal.
>
> Owner: BEE-66. Touch this file when bumping the publishing infrastructure
> or rotating credentials.

---

## TL;DR

- **Coordinates**: `io.beeping:beeping-android:X.Y.Z` (semver 2.0.0, starts at `0.0.0` per the ecosystem 0.x rule).
- **Plugin**: [`com.vanniktech.maven.publish`](https://vanniktech.github.io/gradle-maven-publish-plugin/) — handles maven-publish + signing + sources/javadoc.jar + Central Portal upload in one block.
- **Signing**: GPG-armored ASCII key, in-memory (no keyring file). Conditional locally (skipped if no key); always required in CI.
- **Release trigger**: push tag `v*.*.*` → GitHub Action runs `publishAndReleaseToMavenCentral`.
- **Fallback while OSSRH onboarding is pending**: GitHub Releases attach the signed `.aar` + `SHA256SUMS.txt` so downstream consumers can `implementation files("...")` from a release URL.

## Onboarding flow

Phase 8 cierre (software-side) does not require any of the OSSRH-side steps;
the local `publishToMavenLocal` smoke gate already covers our CI confidence.
The OSSRH-side onboarding (steps 1-5 below) takes 2-4 weeks of human review
and runs in parallel.

### 1. Claim the `io.beeping` namespace on Sonatype Central Portal

1. Go to <https://central.sonatype.com/> and sign up as `alfred@beeping.io` (use GitHub SSO if available).
2. In the dashboard → **Namespaces** → **Add namespace** → enter `io.beeping`.
3. The Portal shows a **verification token** (e.g. `1a2b3c4d-5e6f-...`). Copy it.
4. Add the DNS TXT record on `beeping.io`:
   ```
   beeping.io.   IN   TXT   "1a2b3c4d-5e6f-..."
   ```
   (Cloudflare / Route 53 / Namecheap — wherever DNS is hosted.)
5. Wait for Sonatype's verifier (usually < 1 hour, occasionally 24h). Status changes to **Verified** in the Portal.
6. Generate a **User Token** (Portal → Account → Generate User Token). It returns:
   - `username` → `MAVEN_CENTRAL_USERNAME` GitHub secret
   - `password` → `MAVEN_CENTRAL_PASSWORD` GitHub secret

### 2. Generate the GPG signing key

Run locally (Mac):

```bash
# 4096-bit RSA key, no expiration. Use a strong passphrase you store in 1Password.
gpg --full-generate-key
# > (1) RSA and RSA (default)
# > 4096
# > 0 = key does not expire
# > Real name: Alfred Rivas
# > Email: alfred@beeping.io
# > Comment: Beeping Maven Central signing
# > <passphrase>

# List + verify
gpg --list-secret-keys --keyid-format=long alfred@beeping.io
# > sec   rsa4096/AAAA1111BBBB2222 2026-05-11 [SC]
# >        XXXXXXXXXXXXXXXX  (the full fingerprint)
```

Take note of the **long key id** (the 16 hex chars after `rsa4096/`).

Export and publish:

```bash
# Export to ASCII-armored format for in-memory signing (vanniktech expects this)
gpg --armor --export-secret-keys AAAA1111BBBB2222 > /tmp/beeping-signing.asc

# Publish the PUBLIC key to multiple keyservers (Sonatype checks at least one)
gpg --keyserver keys.openpgp.org    --send-keys AAAA1111BBBB2222
gpg --keyserver keyserver.ubuntu.com --send-keys AAAA1111BBBB2222
gpg --keyserver pgp.mit.edu          --send-keys AAAA1111BBBB2222
```

Wait ~10 min for keyserver propagation. Verify externally:

```bash
gpg --keyserver keys.openpgp.org --recv-keys AAAA1111BBBB2222
```

### 3. Store credentials

**Local** (`~/.gradle/gradle.properties`, **NOT** in the repo):

```properties
mavenCentralUsername=<MAVEN_CENTRAL_USERNAME from step 1>
mavenCentralPassword=<MAVEN_CENTRAL_PASSWORD from step 1>
signingInMemoryKey=<contents of /tmp/beeping-signing.asc as a single line — replace newlines with \\n>
signingInMemoryKeyId=AAAA1111BBBB2222
signingInMemoryKeyPassword=<gpg passphrase from step 2>
```

To transform the ASCII-armored key to a one-liner:

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0}' /tmp/beeping-signing.asc > /tmp/signing-oneline.txt
# Paste the content of /tmp/signing-oneline.txt as the value of signingInMemoryKey.
```

After setting, delete `/tmp/beeping-signing.asc` and `/tmp/signing-oneline.txt` from disk.

**GitHub Secrets** (Repository Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | from step 1 |
| `MAVEN_CENTRAL_PASSWORD` | from step 1 |
| `SIGNING_KEY` | the in-line ASCII-armored key (`\n` escaped) from step 2 |
| `SIGNING_KEY_ID` | the long key id (`AAAA1111BBBB2222`) |
| `SIGNING_KEY_PASSWORD` | the gpg passphrase from step 2 |

CI maps these to `ORG_GRADLE_PROJECT_*` env vars in the workflow.

### 4. Local verification

```bash
./gradlew :AndroidBeepingCore:publishToMavenLocal
ls -la ~/.m2/repository/io/beeping/beeping-android/0.0.0/
```

You should see (after credentials are set in `~/.gradle/gradle.properties`):

```
beeping-android-0.0.0.aar
beeping-android-0.0.0.aar.asc
beeping-android-0.0.0-sources.jar
beeping-android-0.0.0-sources.jar.asc
beeping-android-0.0.0-javadoc.jar
beeping-android-0.0.0-javadoc.jar.asc
beeping-android-0.0.0.pom
beeping-android-0.0.0.pom.asc
beeping-android-0.0.0.module
beeping-android-0.0.0.module.asc
```

Without credentials configured the signing step is skipped (no `.asc` files
produced) but the artifacts themselves are still generated — useful for
local dev / CI smoke gates that don't have the signing key.

### 5. First real release

Once the namespace is verified and credentials are in GitHub Secrets:

```bash
# In the repo, on the merge commit you want to release:
git tag v0.0.0
git push origin v0.0.0
```

The `.github/workflows/release.yml` workflow runs:
1. `./gradlew :AndroidBeepingCore:publishAndReleaseToMavenCentral --no-configuration-cache`
2. Sonatype Central Portal receives the bundle, stages it.
3. Because `automaticRelease = false` in `mavenPublishing { ... }`, the
   bundle waits for a manual "Publish" click in the Portal UI. This is a
   safety net for the first release; later releases can flip to
   `automaticRelease = true` once we're confident.
4. After the Portal publishes, the artifact is **searchable at**
   <https://central.sonatype.com/artifact/io.beeping/beeping-android> within
   ~15 minutes and **resolvable via**
   <https://repo1.maven.org/maven2/io/beeping/beeping-android/0.0.0/> within
   ~2 hours.

## Bumping the version

Edit `AndroidBeepingCore/build.gradle.kts`:

```kotlin
mavenPublishing {
    coordinates(
        groupId = "io.beeping",
        artifactId = "beeping-android",
        version = "0.0.1", // bumped
    )
    // ...
}
```

Commit + push the bump + tag:

```bash
git commit -am "release(beeping-android): BEE-XXXX bump 0.0.0 -> 0.0.1"
git tag v0.0.1
git push origin main v0.0.1
```

The release workflow does the rest.

> **Semver bumping cheatsheet** (Beeping ecosystem 0.x rule until full
> validation):
> - `fix:` → `0.X.Y` (PATCH)
> - `feat:` → `0.X.Y` (MINOR)
> - Never bump to `1.0.0` automatically — coordinated ecosystem move only.

## Consuming from Maven Central

Once published:

```kotlin
// build.gradle.kts (Gradle Kotlin DSL)
dependencies {
    implementation("io.beeping:beeping-android:0.0.0")
}
```

```groovy
// build.gradle (Groovy DSL)
dependencies {
    implementation 'io.beeping:beeping-android:0.0.0'
}
```

```xml
<!-- pom.xml (Maven) -->
<dependency>
    <groupId>io.beeping</groupId>
    <artifactId>beeping-android</artifactId>
    <version>0.0.0</version>
    <type>aar</type>
</dependency>
```

## Consuming from GitHub Releases (fallback)

While the Maven Central artifact is still pending Sonatype review, downstream
consumers can pull the AAR directly from a GitHub Release tag:

```kotlin
// build.gradle.kts
dependencies {
    implementation(files("libs/beeping-android-0.0.0.aar"))
}
```

Each GitHub Release attaches the same signed AAR + `SHA256SUMS.txt`.
Consumers verify with:

```bash
shasum -a 256 -c SHA256SUMS.txt
gpg --verify beeping-android-0.0.0.aar.asc beeping-android-0.0.0.aar
```

## Troubleshooting

### `Cannot perform signing task: no configured signatory`

The signing step ran but couldn't find a key. Either:
- You're on local dev without `signingInMemoryKey` in `~/.gradle/gradle.properties` → expected, the build script gates signing by property existence (see `AndroidBeepingCore/build.gradle.kts` `mavenPublishing { ... }` block). Run `publishToMavenLocal` without the signing block as a smoke check.
- In CI: a secret is missing. Verify the 5 GitHub Secrets exist and the workflow maps them correctly.

### `403 Forbidden` on publish to Central Portal

- The User Token may have rotated. Generate a new one at
  <https://central.sonatype.com/account> and update GitHub Secrets.
- The namespace might not be Verified yet. Check the Portal dashboard.

### `Invalid signature` on Portal

- The public key wasn't propagated to the keyserver Sonatype checks. Re-run
  the `gpg --keyserver ... --send-keys` commands and wait 10 min.

### POM validation fails

The Portal lists required fields in its error message. Common omissions:
- `<licenses>` / `<developers>` / `<scm>` block missing → fix in `pom { ... }`
  block in `build.gradle.kts`.
- Empty `<description>` → must be a non-trivial sentence.

## References

- Sonatype Central Portal: <https://central.sonatype.com/>
- Vanniktech plugin docs: <https://vanniktech.github.io/gradle-maven-publish-plugin/central/>
- Maven Central requirements: <https://central.sonatype.org/publish/requirements/>
- POM reference: <https://maven.apache.org/pom.html>
