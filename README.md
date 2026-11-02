# LACE (WearTAK plugin)

## Purpose and Capabilities

LACE (Liquid, Ammunition, Casualty, Equipment) report tool for WearTAK on Wear OS. Pick GREEN /
YELLOW / RED / BLACK for each category, add WIA/KIA counts and notes, then **TRANSMIT REPORT**.
LACE asks WearTAK core to send the report as a chat to **All Chat Rooms**
(`requestCotDispatch` with payload format `chat`). WearTAK sends it exactly like its own chat:
from the watch's UID and callsign, saved in the watch's chat history, and routed by WearTAK's
dispatch policy (only the companion app when bonded; otherwise TAK Server, SITX and multicast).
Requires a WearTAK build that advertises the `chatDispatchV1` host extra.
There is no separate callsign to enter in LACE; WearTAK supplies the sender identity.
After WearTAK accepts a report, the form clears for the next report. Failed sends keep
the form intact, as do any new edits made while a report is sending.

Built from the [WearTAK plugin template](https://git.tak.gov/weartak-plugins/weartak-plugin-template).

### Report format

```text
LACE: L:YELLOW A:GREEN C:RED E:GREEN (WIA U:1 P:0 R:2 KIA:0) - Liquid: Water 2L; Ammo: 5.56 - 4 mags
```

WIA/KIA counts appear when any is above zero; notes appear when filled in.
Liquid and ammunition use GREEN >75%, YELLOW 25-75%, RED <25%, BLACK out.

## Status

Release automation is configured as described below. Current deployment readiness and
device-validation results are not documented here.

### Releases

Pushes to `main` on git.tak.gov build a WearTAK production-signed APK and publish it to the
GitHub mirror as **TAK.gov Forge CI/CD Pipeline Release (<version>)**; `vX.Y.Z[.B]` tags
build official releases. Versioning and release rules match WearTAK; see
[ci/README.md](ci/README.md). Release LACE works with release WearTAK; debug builds
work with WearTAK `.dev`.

## Point of Contacts

Project contacts are not documented in this repository.

## Ports Required

LACE hands report dispatch to WearTAK through the plugin Binder interface; it does not
declare an Internet permission or configure network ports itself. Network ports for the
companion app, TAK Server, SITX and multicast depend on the WearTAK deployment and are
not specified in this repository.

## Equipment Required

A Wear OS watch running a compatible WearTAK host with the `chatDispatchV1` extra is
required for report transmission. The app requires watch hardware and Android API 30
or later (`app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`).
Release and debug host/plugin signing compatibility is described under [Releases](#releases).

## Equipment Supported

The app targets Wear OS watches. Specific watch models and a verified device-support
matrix are not documented in this repository.

## Compilation

### WearTAK plugin SDK

The WearTAK plugin SDK is not included in this repository. It is distributed to TAK.gov users through the
plugin template. With git.tak.gov access, fetch it into `app/libs/` before building:

```powershell
./scripts/fetch-sdk.ps1
```

```bash
./scripts/fetch-sdk.sh
```

Both scripts sparse-clone `weartak-plugins/weartak-plugin-template` and copy
`app/libs/weartak-plugin-sdk-{debug,release}.aar`. Pass a ref (branch or tag) to pin a version.

### Build and install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds use the standard Android debug key so WearTAK debug builds can bind the plugin.

Dependency routing matches WearTAK core. Both plugin and library resolution try TAK.gov
Artifactory first when credentials are available, with public repositories as fallbacks.
Username and password are selected independently from the first nonblank environment variable:

| Credential | Priority order |
| --- | --- |
| Username | `ARTIFACTORY_CONSUME_USER`, `ARTIFACTORY_USER`, `TAK_USERNAME` |
| Password | `ARTIFACTORY_CONSUME_PASSWORD`, `ARTIFACTORY_PASSWORD`, `TAK_PASSWORD` |

Both must be present to enable `https://artifacts.tak.gov/artifactory/maven-release`.
Without them, builds use public repositories only and remain subject to public rate limits.
Gradle logs only whether the plugin mirror is enabled, never credential values.
Gradle CI jobs exchange their job identity through TAK.gov's Artifactory token service,
then supply the per-job token to Gradle and Robolectric (which downloads outside Gradle's
repositories). The non-root launcher image uses the service's `_no_depends` template,
not the bundled Gradle init script. Successful logs show `Token exchange successful` and
`takArtifactory for plugins: enabled`; never log token values. If the service requests
an Artifactory login, the triggering user must sign in to `artifacts.tak.gov` once before
retrying. No static secrets need copying and no new permissions are granted by this setup.

## Developer Notes

### Testing and publishing

See [CI/CD and releases](ci/README.md) for pipeline gates, signing requirements,
publisher safety rules, administrator setup, release testing and local validation commands.

## License

The repository owner's original code is licensed under the GNU General Public License,
version 3 or (at your option) any later version (**GPL-3.0-or-later**). See [LICENSE](LICENSE)
for the full license text.

This declaration covers only the repository owner's original code, not third-party
components or code originating from the WearTAK plugin template. Those components
retain their own licensing terms and notices; this declaration does not grant rights
to them or change existing rights or attribution.

The Gradle wrapper scripts (`gradlew`, `gradlew.bat`) and
`gradle/wrapper/gradle-wrapper.jar` retain their Apache-2.0 notices, including the
license embedded in the JAR's `META-INF/LICENSE`. The separately obtained WearTAK
plugin SDK and other dependencies are not relicensed by this declaration.