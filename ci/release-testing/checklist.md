## Test setup

Record the exact build before starting. The rolling release replaces its APK on
every pipeline, so copy the version and SHA-256 from the release details above.

| Field | Value |
| --- | --- |
| LACE version (app info) / pipeline / SHA-256 | |
| WearTAK version (release, Settings > About) | |
| Watch model / Wear OS version | |
| TAK Server / receiving EUD (ATAK/WinTAK/iTAK + version) | |
| Tester / date | |

Install as an update (`adb install -r` or the normal update path). **Do not
uninstall or clear app data.** Release LACE must be tested with **release WearTAK**
(both production-signed). Use a test callsign and do not attach precise home/work
locations to this issue.

## Core acceptance checklist

Check an item only after it passes. For unavailable hardware or unsupported
features, leave it unchecked and record **N/A** with the reason in the results.

- [ ] **Update in place:** the new version installs over the previous one.
- [ ] **WearTAK binds the plugin:** with release WearTAK installed, LACE appears
  in WearTAK's plugin menu as **LACE / Unfunded Request** and stays connected.
- [ ] **Open from WearTAK:** tapping the LACE menu item opens the LACE REPORT form;
  the back button returns to WearTAK.
- [ ] **Callsign:** no callsign entry is required in LACE; WearTAK supplies its own UID and callsign.
- [ ] **Selectors:** each of L / A / C / E switches between GREEN, YELLOW, RED and
  BLACK, and shows the matching meaning.
- [ ] **Text and steppers:** liquid needs, ammo type/count and equipment remarks
  accept typed and voice input; WIA/KIA steppers stay between 0 and 99.
- [ ] **Transmit:** **TRANSMIT REPORT** shows "Sent to All Chat Rooms". The
  receiving EUD shows the report in **All Chat Rooms** reading
  `LACE: L:x A:x C:x E:x - notes`, and it appears in the watch's WearTAK chat.
- [ ] **Sender identity:** the chat comes from the same contact as the watch's PLI
  (not a separate LACE contact).
- [ ] **Routing:** bonded to the companion app, the report reaches ATAK through the phone;
  unbonded, it reaches TAK Server / multicast directly.
- [ ] **Old WearTAK:** with a WearTAK build lacking plugin chat, transmit shows
  "Update WearTAK" and nothing is sent.
- [ ] **WearTAK not connected:** with WearTAK stopped, transmit shows a clear
  "WearTAK not connected" message and does not crash.
- [ ] **Reset:** **RESET** needs a second tap and clears all report fields.

## Results

**Overall:** PASS / FAIL / BLOCKED

**N/A items and reasons:**

**Failures:** for each failure, include steps, expected/actual behavior,
timestamp, and a screenshot or short recording. Attach sanitized logs where
available.

**Release blockers:** release WearTAK cannot bind the plugin, reports not received
by TAK, wrong LACE values in the chat, or crashes preventing use.