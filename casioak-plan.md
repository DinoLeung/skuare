# CASIOAK profile plan

Branch `feat/casioak`. A new `WatchModel.CASIOAK` for:

| Model | Module |
|---|---|
| GA-B2100 | 5689 |
| GBM-2100 | 5729 |
| GM-B2100 | 5691 |

Hand adjustment comes with it.

Only the GA-B2100 has been captured or tested. The GBM-2100 and GM-B2100 are assumed to use
the same protocol, and the README must say they're untested.

## To do

Captures from the official app on the GA-B2100, one file each (user):

- [x] `0x15`/`0x16` alarms (`alarm_set_5_2100.pcapng`)
- [x] `0x18` timer (`timer_2100.pcapng`)
- [x] Reminders, read only (`reminder_2100.pcapng`): the watch has 5
- [x] Settings, read only (`settings_2100.pcapng`)
- [x] Decoded into `research/casioak-research.md`, features table updated
- [x] `0x13` settings with changes (`settings_set_2100.pcapng`): B5000 layout
- [x] Summer time on/off/auto (`summer_time_2100.pcapng`): `DstBitMask`, but session byte `03`
- [x] All 5 reminders (`reminders_2100.pcapng`): B5000 layout
- [x] Decoded into `research/casioak-research.md`. No new features: settings, reminders and
      DST all use skuare's existing layouts
- [ ] Ongoing, not blocking: battery capture at 3/5 (see `research/battery-research.md`)

Implementation:

- [x] Phase 1: `WatchFeatures` (`hasSnoozeAlarm`, `hasTimeZoneLocation`, `canAdjustHands`),
      `CustomWatchModel` + dialog switches, DB v3 migration, `WatchModel.CASIOAK`, tests
- [x] Phase 2: skip `0x24`, alarm 5 label, watch status layout by reply length, timer write
      size matching the reply
- [ ] Device round on the GA-B2100 (user): connect, time sync, world time edit (batched
      path), summer time on/off/auto (does it accept skuare's `21 00 01` session?), alarms
      (does it accept skuare's `00` in alarm byte 1?), timer, reminders, settings,
      background sync, a custom watch keeping or dropping its model. One square as a
      regression check
- [ ] Phase 3: hand adjust library, device experiments (user), then the UI
- [ ] Phase 5: docs, README with GBM-2100 / GM-B2100 marked untested

The captures are decoded in `research/casioak-research.md` (protocol) and
`research/hand-adjust-research.md` (hands). This plan only keeps what they mean for the code.

## What the official app does on connect (GA-B2100)

`22` → `10` → write `23` name → `11` read/write → `26` → `28` → watch status (`00 05 ac`, 172
bytes) → `20`/`28` twice → second transfer (`00 01 …`) → `1d` → `1e` ×2 → `1f` ×2 → writes
`1d`, `1e` ×2, `1f` ×2 → `09` time.

Differences from what skuare does today:

1. **2 clocks**, and the city names have no 3-letter short names (only `1f00`, `1f01`).
2. **Single packets, not the batched characteristics.** The watch has `26eb002e`/`26eb002f`,
   so `Watch.hasBatchCharacteristics` is true and skuare takes the batched path, which the
   official app never uses on this watch. That stays as it is (decided by the services, not a
   feature). The Phase 2 device check confirms the batched path works on the GA-B2100.
3. **No `0x24`** (coordinates / radio ID) is read or written, on connect or on a world clock
   write, but skuare writes it on every time sync.
4. **Watch status is 172 bytes, with battery at [110].** `WatchStatus.fromPacket` requires
   171 bytes, so it fails on every connect (logged, not fatal). The BX has the same problem.
5. **No snooze alarm:** alarm 5 sits in the B5000's snooze slot as a normal alarm, with the
   same packets.
6. **Hands:** hand adjustment (opcode `0x1A`).
7. **5 reminders**, the same packet sizes as the B5000.
8. **The timer is written with 15 bytes**, the same size as the reply. skuare always writes 8.

## WatchFeatures changes

Differences 1, 3, 5, 6 and 7 become features, so screens and writes are decided by
`WatchFeatures` and never by checking for a particular model. 2 stays with
`Watch.hasBatchCharacteristics`, and 4 and 8 are decided by the reply (below).

| Feature | CLASSIC_SQUARE | MIP_SQUARE | DW_SQUARE | CASIOAK | Custom default |
|---|---|---|---|---|---|
| `clockCount` | 6 | 3 | 2 | 2 | 2 |
| `reminderCount` | 5 | 0 | 0 | 5 | 0 |
| `hasFontStyle` | no | yes | no | no | no |
| `hasShortCityNames` | no | yes | no | no | no |
| `hasWeekdayLanguage` | yes | no | yes | yes | no |
| **`hasSnoozeAlarm`** (new) | yes | yes (today's behaviour) | yes (today's behaviour) | no | yes |
| **`hasTimeZoneLocation`** (new, `0x24`) | yes | yes | yes | no | no |
| **`canAdjustHands`** (new) | no | no | no | yes | no |

- **`hasSnoozeAlarm`:** the alarm packets probably stay the same (`0x15` = signal + alarm 1,
  `0x16` = alarms 2–5), with the fifth slot being a plain alarm. The Alarms screen shows it as
  "Alarm 5" instead of "Snooze Alarm". An alarm capture confirms the layout (Phase 4).
- **`hasTimeZoneLocation`:** with it off, `0x24` is skipped in both
  `writeClocksAndTimeZoneData` paths. Not a "has radio" flag: the DW-B5600 has no radio and
  takes `0x24` today. A device test shows whether the GA-B2100 errors on `0x24` or just
  ignores it. Either way we follow the official app.
- **`canAdjustHands`:** whether the hand adjustment screen is shown. The hand groups and
  their revolution sizes (clock `1`/2160, function `2`/180) are a `HandGroup` enum, because
  CASIOAK is the only analog profile so far. If a later model has other groups or sizes,
  they move into the profile. Custom models stay off, because the sizes can't be guessed.
- **Watch status layout** isn't a feature: the watch ignores the requested size and always
  replies with its own fixed size, so the parser picks the layout from the reply length
  (171 → [7], 172 → [110], 216 → [158]). Custom models get this for free.
- **Timer write size** isn't a feature either: the write uses the size of the last timer reply
  (8 or 15 bytes), padded with zeros. The BX gets the same fix.

Custom models (DB v2 → v3, a Room auto migration with column defaults):

- `hasSnoozeAlarm` is stored with `defaultValue = 1`, so custom watches that are already paired
  keep their snooze alarm.
- `hasTimeZoneLocation` is stored with `defaultValue = 0`, and `CustomWatchModel()` defaults to
  false, in line with the minimal default profile. Custom watches that are already paired stop
  sending `0x24` after the update, until the user turns it on.
- `CustomWatchModel.from(features)` copies both, so a watch kept on the CLASSIC_SQUARE features
  keeps `0x24`.
- `CustomWatchModelDialog` gets two switches: "Snooze alarm" and "Time zone location".
- `canAdjustHands` isn't stored (always false).
- Export the v3 schema and extend `CustomWatchModelMigrationTest`.

## Phase 1: features and profile

1. Add the three new `WatchFeatures` properties, with the values for existing models from the
   table above (no change in behaviour), the custom model fields, and the DB migration to v3.
2. `WatchModel.CASIOAK` and the three model names, with module comments.
3. Tests: `WatchModelTest` covers the three names, with and without `#`. Tests that use
   `CASIO GA-B2100` as the "unknown watch" (`WatchModelTest`, `CustomWatchModelTest`,
   `CustomWatchModelMigrationTest`, `PairedWatchRepositoryTest`, `WatchSessionHandlerTest`)
   switch to another unknown name, e.g. `CASIO GBD-H2000`.
4. A GA-B2100 already paired with a custom model drops it by itself (`resolveWatchModel`,
   `clearStoredCustomModel`). Check this on the device.

## Phase 2: features in use

- `writeClocksAndTimeZoneData`: skip `0x24` without `hasTimeZoneLocation`, in both the
  batched and the single-packet path.
- Alarms screen: the fifth alarm's label and icon follow `hasSnoozeAlarm`.
- Watch status: the layout comes from the reply length. Decoding the battery stays deferred
  until the battery experiment settles the scale. Until then, an unknown byte gives
  "battery unknown" instead of an exception.
- Device check on the GA-B2100: connect, time sync, editing world time (batched path),
  alarms, a background sync. Regression check on one square.

## Phase 3: hand adjustment (opcode 0x1A)

Library:

- `OpCode.HAND_ADJUST(0x1A)`. The `1a 04 00 00 00 00` acks go through `handlePacket`, so
  waits on `packets` work.
- `HandGroup` enum (`CLOCK(1, 2160)`, `FUNCTION(2, 180)`) and a packet builder for
  `1a 04 <cmd> <steps LE16> 00`, where a negative n becomes `total − n`. Unit tests use the captured values: clock −2 →
  `6e 08`, function −2 → `b2 00`.
- Watch extensions, following the captured sequence:
  - `startHandAdjust()`: `06`, wait for the ack
  - `selectHandGroup(g)`: `x2`
  - `moveToAdjustPosition(g)`: `x8 0`, wait for the ack (clock took 10.7 s, so allow ~20 s)
  - `moveHands(g, n)`: `x8 n`, wait for the ack, then `x9`
  - `finishHandAdjust()`: `04`, `06` (wait for the ack), `07`

UI:

- A hand adjustment screen, opened from Settings and shown only with `canAdjustHands`:
  choose a group, wait while the hands move into place, step −1/+1 (repeat on hold), then Done.
- Leaving the screen or the app sends the exit sequence. Disconnecting mid-adjustment is an
  open question (below).
- Block background sync while adjustment is open, if the connection model allows it.

Device experiments, before the UI is final (the results go into `hand-adjust-research.md`):

1. Does `07` alone leave adjust mode *and* keep the new position, or is `04` needed?
2. Is `x9` needed after every move, or only once at the end?
3. Is `x2` needed before `x8`? Can both groups be adjusted in one session?
4. Disconnect mid-adjustment: what state are the hands left in?
5. A large step (e.g. +60) to check byte 5 and the ack timing.

## Phase 4: leftovers from the captures

All captures are in. Alarms, settings, reminders and DST use the B5000 layouts, and the timer
is handled in Phase 2. What's left depends on the device round:

- **Clocks session byte:** the official app sends `21 00 03` … `21 01 03` for a summer time
  change and `01` for a world clock change. skuare always sends `01`. If the watch rejects or
  ignores a DST change sent with `01`, the session byte is chosen by what changed (home clock
  → `03`), for every model.
- **Alarm byte 1:** if the watch doesn't accept `00`, write `0x40` like the official app (the
  BX already gets `0x40` from the official app, so probably for every model).

## Phase 5: docs

- README: supported models and modules, and hand adjustment in the features list. Mark the
  GBM-2100 (5729) and GM-B2100 (5691) as untested: assumed to use the same protocol as the
  GA-B2100, but never connected. Same for the fastlane full description.
- `research/hand-adjust-research.md`: experiment results. `research/battery-research.md`: the
  watch status layout picked by reply length.
- Check `research/casioak-research.md` is up to date before this plan is deleted.
- Delete this plan when the branch merges (as with earlier plans).

## Order

Phase 1 → 2 (one device test round) → 4 if the device round needs it → 3 library +
experiments → 3 UI → 5. One commit per step, `feat:`/`docs:`/`test:` style.
