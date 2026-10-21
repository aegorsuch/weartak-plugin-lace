# LACE (WearTAK plugin)

LACE (Liquid, Ammunition, Casualty, Equipment) report tool for WearTAK on Wear OS. Pick GREEN /
YELLOW / RED / BLACK for each category, add WIA/KIA counts and notes, then **TRANSMIT REPORT**.
LACE asks WearTAK core to send the report as a chat to **All Chat Rooms**
(`requestCotDispatch` with payload format `chat`). WearTAK sends it exactly like its own chat:
from the watch's UID and callsign, saved in the watch's chat history, and routed by WearTAK's
dispatch policy (only the companion app when bonded; otherwise TAK Server, SITX and multicast).
Requires a WearTAK build that advertises the `chatDispatchV1` host extra.

Built from the [WearTAK plugin template](https://git.tak.gov/weartak-plugins/weartak-plugin-template).

## WearTAK plugin SDK

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

## Build and install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds use the standard Android debug key so WearTAK debug builds can bind the plugin.

## Releases

Pushes to `main` on git.tak.gov build a WearTAK production-signed APK and publish it to the
GitHub mirror as **TAK.gov Forge CI/CD Pipeline Release (<version>)**; `vX.Y.Z[.B]` tags
build official releases. Versioning and release rules match WearTAK; see
[ci/README.md](ci/README.md). Release LACE works with release WearTAK; debug builds
work with WearTAK `.dev`.

## Report format

```text
ALPHA-1 LACE: L:YELLOW A:GREEN C:RED E:GREEN (WIA U:1 P:0 R:2 KIA:0) - Liquid: Water 2L; Ammo: 5.56 - 4 mags
```

WIA/KIA counts appear when any is above zero; notes appear when filled in.
Liquid and ammunition use GREEN >75%, YELLOW 25-75%, RED <25%, BLACK out.