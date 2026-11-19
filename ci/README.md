# LACE CI/CD and releases

Same model as WearTAK WearOS (`core/weartak-core/wearos-tak-civ`) and the WearTAK plugin
template. TAK.gov (`git.tak.gov/weartak-plugins/weartak-plugin-lace`) is canonical; GitHub
(`aegorsuch/weartak-plugin-lace`) is a mirror.

## Pipeline

| Trigger | Jobs |
| --- | --- |
| Merge request | `release_publisher_tests`, `release_checklist_tests`, `unit_tests` (debug build, no production signing) |
| Push to protected `main` | tests, `main_release` (production-signed APK, 30-day artifact), then `publish_github_main` |
| Tag `vX.Y.Z[.B]` | tests, `official_release` (production-signed APK, kept forever) |

Release builds are signed with the **WearTAK production key** and the job fails unless the
APK signer SHA-256 is `1579b600…348d`. Release WearTAK only binds plugins with the same
signature (signature-level `BIND_PLUGIN`), so debug builds only work with WearTAK `.dev`.

The WearTAK plugin SDK is not in this repository; `scripts/fetch-sdk.sh` pulls it from
`weartak-plugins/weartak-plugin-template` (`WEARTAK_SDK_REF`, default `main`) using
`SDK_READ_TOKEN` if set, otherwise `CI_JOB_TOKEN`.

## Versioning

Same scheme as WearTAK (WEARTAK-239), in `app/build.gradle.kts`:

- `baseVersionCode` / `baseVersionName` (currently `584` / `5.8.4`), aligned with the
  WearTAK 5.8 line. Bump them for each official `vX.Y.Z[.B]` tag.
- CI builds: versionCode = `base * 10000 + CI_PIPELINE_IID` (build fails at IID ≥ 10000),
  versionName = `<base>-p<IID>+<short sha>`, e.g. `5.8.4-p12+1a2b3c4d`. Every pipeline is
  therefore an in-place upgrade (MDMs such as Knox accept it).
- Local builds: versionCode = base, versionName = base.

## Rolling GitHub release

After **all** earlier jobs pass, `publish_github_main` publishes the verified `main_release`
APK to the GitHub mirror as **TAK.gov Forge CI/CD Pipeline Release (<version>)** on the
fixed tag `TAK.gov-Forge-CI-CD-Pipeline-Release`, asset
`WearTAK-Plugin-LACE-<version>-p<IID>.apk` (the base version comes from versionName without
its pipeline suffix or `+sha`; `<IID>` is the current GitLab project pipeline IID). It is a
stable release (`prerelease=false`)
that does not take GitHub's "Latest" flag. The body records versionName/versionCode,
pipeline ID/IID and URL, source SHA and APK SHA-256.

Safety rules (identical to WearOS, see `ci/publish_github_release.py`):

- Runs only for protected `main` push pipelines of this exact project; the script
  re-checks the gates.
- Uploads only the signed APK, verified against `verified-apk.sha256` and Gradle metadata;
  it downloads the upload again and checks size + SHA-256 before publishing.
- Never pushes commits or creates/moves tags; the anchor tag must already exist.
- Older pipelines never replace newer ones; only older publisher-owned assets are deleted,
  after verification. Resource group `lace-github-main` serializes writers.

### Administrator setup

1. Signing variables: `WEARTAK_RELEASE_KEYSTORE_B64`, `RELEASE_STORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (protected, masked). Projects in the
   `weartak-plugins` group inherit them; elsewhere they need separate approval.
2. Protect `main` on git.tak.gov.
3. Create tag `TAK.gov-Forge-CI-CD-Pipeline-Release` on a commit already in the GitHub mirror.
4. GitHub token able to write releases on `aegorsuch/weartak-plugin-lace`: a classic
   token with only the `public_repo` scope (the mirror is public), or a fine-grained token
   scoped to this repository with **Contents: read and write**. Add it as GitLab variable
   `LACE_GITHUB_TOKEN` (masked, protected, hidden, no expansion).
5. If `CI_JOB_TOKEN` cannot read the template, add `SDK_READ_TOKEN` (read_repository).

## Release testing

After a verified publish, the job sends `repository_dispatch` `lace-release-published`.
`.github/workflows/release-testing.yml` (on the GitHub default branch) re-reads the release
and opens one issue per pipeline, `Release testing: <release title>`, with the APK link and
`ci/release-testing/checklist.md`, assigned to the repository variable `RELEASE_TESTER`
(needs Write access). Existing issues, including closed ones, are never recreated.
Edit the checklist when features change.

## Market refresh (WEARTAK-322)

After APK digest readback, stable release verification, and older owned asset cleanup,
`publish_github_main` dispatches `publish-market-catalog.yml` on `main` in
`aegorsuch/weartak-wearos`. This is independent of the release-testing notification and
does not replace the hourly catalog refresh. Skipped, stale, failed, debug, and
prerelease builds do not dispatch.

Before deploying the hook, make sure the [catalog workflow](https://github.com/aegorsuch/weartak-wearos/actions/workflows/publish-market-catalog.yml)
is enabled on `main` with `workflow_dispatch`. [Create a fine-grained GitHub token](https://github.com/settings/personal-access-tokens/new)
for **only** `aegorsuch/weartak-wearos`, with repository permission **Actions: read and write**.
Add it in [LACE GitLab CI/CD settings](https://git.tak.gov/weartak-plugins/weartak-plugin-lace/-/settings/ci_cd)
as `MARKET_CATALOG_DISPATCH_TOKEN` (masked, protected, hidden, no expansion). Do not
reuse or broaden `LACE_GITHUB_TOKEN`.

Missing credentials or a rejected/failed dispatch fail the job as a notification
failure; the verified release stays published. HTTP 204 confirms dispatch acceptance,
not catalog completion. After fixing the problem, retry `publish_github_main` on the same
pipeline: it re-verifies the existing APK and can dispatch again unless superseded.

## Local validation

```text
python -m unittest discover -s ci/tests -v
node --test ci/release-testing/create_release_test_issue.test.cjs
```