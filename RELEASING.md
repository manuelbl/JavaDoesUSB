# Releasing Java Does USB

This document describes how a new version of the library is released to Maven Central, and how
version numbers flow through the repository afterwards.

## Versioning scheme

- The library follows [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`).
- On the `main` branch, `java-does-usb/pom.xml` always carries the *next*, unreleased version
  with a `-SNAPSHOT` suffix (e.g. `1.2.2-SNAPSHOT`). This makes it obvious that a build from `main`
  is a development build, not something published to Central.
- `README.md` and the example projects under `examples/` intentionally do **not** track this
  SNAPSHOT. They pin the latest *released* version, because they double as user-facing
  documentation: a user reading the README or copying an example's `pom.xml`/`build.gradle.kts`
  should see the version they can actually `mvn install` from Central today.
- Continuous integration overrides the pinned example version at build time so it still compiles
  the examples against `main`'s HEAD (see "How CI stays honest" below). No file needs to be edited
  for this to work.

## Release checklist

1. **Drop the `-SNAPSHOT` suffix.**
   In `java-does-usb/pom.xml`, set `<version>` to the release version, e.g.:
   ```bash
   cd java-does-usb
   ./mvnw versions:set -DnewVersion=1.3.1 -DgenerateBackupPoms=false
   ```

2. **Update the version everywhere it's pinned for users.** This means:
   - `README.md` — the Maven (`<version>...</version>`) and Gradle (`version: '...'`) snippets
     under "Getting Started", and the version history table if you're adding an entry.
   - Every example's dependency on `java-does-usb`, pinned via a single property so it's one edit
     per file (`java-does-usb.version` in the Maven examples, `javaDoesUsbVersion` in the Gradle
     one):
     - `examples/enumerate/pom.xml`
     - `examples/bulk_transfer/pom.xml`
     - `examples/monitor/pom.xml`
     - `examples/monitor_kotlin/pom.xml`
     - `examples/stm_dfu/pom.xml`
     - `examples/epaper_display/pom.xml`
     - `examples/enumerate_native/pom.xml`
     - `examples/monitor_native/pom.xml`
     - `examples/enumerate_kotlin/build.gradle.kts`
   - The example's *own* project version (its `<version>` / `version = "..."`, separate from the
     `java-does-usb.version`/`javaDoesUsbVersion` property), which by convention tracks the library
     release it was last verified against, for `enumerate`, `bulk_transfer`, `monitor`,
     `monitor_kotlin`, `stm_dfu`, `epaper_display`, and `enumerate_kotlin`. (`enumerate_native` and
     `monitor_native` version themselves independently as `1.0-SNAPSHOT` and don't need this.)
   - The sample console output hardcoded in each example's own `README.md` (e.g. build log lines
     like `[INFO] Building enumerate 1.2.1` or jar filenames like `stm_dfu-1.2.1.jar`), which
     embeds the example's own project version from the point above.

   A simple search for the previous version number across `README.md` and `examples/` will locate
   every occurrence listed above.

3. **Commit, tag, and publish.**
   ```bash
   git add -A
   git commit -m "Release 1.2.2"
   git tag v1.2.2
   git push origin main v1.2.2
   cd java-does-usb
   ./mvnw clean deploy -Prelease   # or whatever profile/flags drive maven-gpg-plugin + central-publishing-maven-plugin
   ```
   (Adjust to however signing/publishing is actually invoked locally today — this step is manual
   and not run by CI.)

4. **Prepare `main` for the next development iteration.**
   ```bash
   cd java-does-usb
   ./mvnw versions:set -DnewVersion=1.2.3-SNAPSHOT -DgenerateBackupPoms=false
   git add pom.xml
   git commit -m "Prepare for next development iteration"
   git push origin main
   ```
   Deliberately do **not** touch `README.md` or the examples in this step — they should keep
   pointing at the release just made (1.2.2) until the *next* release checklist run.

## How CI stays honest

Because the examples pin the released version, a naive CI setup would silently compile them
against whatever is available on Maven Central instead of the code actually being tested — i.e.
after step 4 above, `main` might contain a breaking change that no CI run would ever catch until
the next release.

To prevent that, `.github/workflows/continuous-integration.yaml`:

1. Builds and `install`s the library from the checked-out `pom.xml` (whatever version — released
   or `-SNAPSHOT` — is on HEAD) into the local Maven repository.
2. Reads that exact version back out with `mvn help:evaluate -Dexpression=project.version`.
3. Passes it to each example build as an override:
   - Maven examples: `-Djava-does-usb.version=<version>`
   - The Gradle example (`enumerate_kotlin`): `-PjavaDoesUsbVersion=<version>`

This means CI always compiles every example against the exact commit under test, while the
committed example files keep showing users the last real release.

If you add a new example, give its `java-does-usb` dependency the same treatment: introduce a
`java-does-usb.version` property (Maven) or an overridable `javaDoesUsbVersion` val (Gradle)
defaulting to the current released version, add a CI step following the existing pattern, and add
it to the list in step 2 of the release checklist above.
