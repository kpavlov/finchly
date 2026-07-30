# How to Release to Maven Central

Releases are published by the `Release` GitHub Actions workflow
(`.github/workflows/release.yml`), not by running `mvn release:*` locally.

1. Create a GitHub Release with tag `vX.Y.Z` (e.g. `v0.2.0`) and publish it.
   This triggers the `release` workflow, which checks out the tag, sets the
   Maven version to `X.Y.Z`, and runs `mvn -P release clean deploy` to sign
   and publish artifacts to Maven Central.

   Alternatively, trigger the workflow manually via `workflow_dispatch` with
   the `release_version` input, without creating a GitHub Release first.

2. Watch the `publish` job in the Actions tab. Central publishing is
   configured with `autoPublish=true` and `waitUntil=validated`, so no manual
   step is needed on Sonatype's side.

The `reports` module is excluded from release builds (it only exists to
aggregate test coverage) via the `tests` profile in the root `pom.xml`, which
is active by default but deactivated when `-P release` is passed.
