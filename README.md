# LACE (WearTAK plugin)

LACE (Liquid, Ammunition, Casualty, Equipment) report tool for WearTAK on Wear OS. Pick GREEN /
YELLOW / RED / BLACK for each category, add WIA/KIA counts and notes, then **TRANSMIT REPORT**.
The report is sent as a GeoChat (`b-t-f`) to **All Chat Rooms** from the watch's WearTAK identity, so every
TAK client can read it, with a structured `<lace/>` detail attached. LACE hands the CoT to WearTAK core
(`requestCotDispatch`), which relays it to the TAK network.

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

## CoT format

```xml
<event type="b-t-f" how="m-g" uid="GeoChat.<WearTAK UID>.All Chat Rooms.<msg id>" ...>
  <point lat="..." lon="..." hae="..." ce="..." le="..."/>
  <detail>
    <__chat chatroom="All Chat Rooms" id="All Chat Rooms" senderCallsign="ALPHA-1" messageId="...">
      <chatgrp uid0="<WearTAK UID>" uid1="All Chat Rooms" id="All Chat Rooms"/>
    </__chat>
    <link uid="<WearTAK UID>" type="a-f-G-U-C" relation="p-p"/>
    <remarks source="BAO.F.ATAK.<WearTAK UID>" to="All Chat Rooms">ALPHA-1 LACE: L:YELLOW A:GREEN C:RED E:GREEN (WIA U:1 P:0 R:2 KIA:0) - Liquid: Water 2L</remarks>
    <lace liquid="YELLOW" ammo="GREEN" casualty="RED" equipment="GREEN"
          urgent_wia="1" priority_wia="0" routine_wia="2" kia="0"/>
  </detail>
</event>
```

Liquid and ammunition use GREEN >75%, YELLOW 25-75%, RED <25%, BLACK out.

The sender UID is WearTAK's own device UID, read from the host device-status telemetry key `deviceUid`.
WearTAK builds without that key cannot send LACE reports (LACE never invents a sender).
Position comes from WearTAK's self PLI, falling back to the watch's last known location. The plugin
SDK has no map-center query, so with no fix the report is not sent (never 0,0).