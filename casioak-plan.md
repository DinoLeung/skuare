# CASIOAK profile plan

Branch `feat/casioak`. A new `WatchModel.CASIOAK` for:

| Model | Module |
|---|---|
| GA-B2100 | 5689 |
| GBM-2100 | 5729 |
| GM-B2100 | 5691 |

Hand adjustment comes with it.

What we know about the GA-B2100 comes from `connect_time_sync_bat*_2100.pcapng` (connect
sync), `world_time_nyc_2100.pcapng` (world clock write), `research/hand-adjust-research.md`,
and the watch itself: 5 alarms plus the hourly signal, no snooze alarm.

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
3. **No `0x24`** (coordinates / radio ID) is read or written, but skuare writes it on every
   time sync. Confirmed for a user-initiated write too: `world_time_nyc_2100.pcapng` (world
   clock set to New York) is `21 00 01`, then read/write `1d`, `1e` ×2, `1f` ×2, then
   `21 01 01`. No `0x24`, and everything on 0x000c/0x000e (no batched characteristics).
   The capture ends 0.8 s after `21 01 01` with no `09` time write, while skuare writes the
   time after every clock write. It's unclear whether the capture was cut short.
4. **Watch status is 172 bytes, with battery at [110].** `WatchStatus.fromPacket` requires
   171 bytes, so it fails on every connect (logged, not fatal). The BX has the same problem.
5. **No snooze alarm:** the fifth alarm is a normal alarm.
6. **Hands:** hand adjustment (opcode `0x1A`).

## WatchFeatures changes

Differences 1, 3, 5 and 6 become features, so screens and writes are decided by
`WatchFeatures` and never by checking for a particular model. 2 stays with
`Watch.hasBatchCharacteristics`, and 4 is decided by the reply (below).

| Feature | CLASSIC_SQUARE | MIP_SQUARE | DW_SQUARE | CASIOAK | Custom default |
|---|---|---|---|---|---|
| `clockCount` | 6 | 3 | 2 | 2 | 2 |
| `reminderCount` | 5 | 0 | 0 | 0 | 0 |
| `hasFontStyle` | no | yes | no | no | no |
| `hasShortCityNames` | no | yes | no | no | no |
| `hasWeekdayLanguage` | yes | no | yes | no (until a `0x13` capture) | no |
| **`hasSnoozeAlarm`** (new) | yes | yes (today's behaviour) | yes (today's behaviour) | no | yes |
| **`hasTimeZoneLocation`** (new, `0x24`) | yes | yes | yes | no | yes |
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

Custom models (DB v2 → v3, a Room auto migration with column defaults, so watches already
paired keep today's behaviour):

- `hasSnoozeAlarm` and `hasTimeZoneLocation` are stored (`defaultValue = 1`).
  `CustomWatchModel.from(features)` copies them.
- `CustomWatchModelDialog` gets a "Snooze alarm" switch. `hasTimeZoneLocation` is stored but
  not shown, since users can't tell from the watch whether it has it (it can go into the
  dialog later if needed).
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

## Phase 4: captures still needed

From the GA-B2100, opening and saving each screen in the official app:

- **alarms:** confirm `0x15`/`0x16` match the B5000 layout, with alarm 5 in the snooze slot
  and no extra flag bit
- **watch settings (`0x13`):** whether the layout is the same, and the weekday language /
  light duration settings
- **timer (`0x18`):** whether it has one, 8 or 15 bytes

Until then CASIOAK uses the B5000 layouts. If a capture shows a different layout, it becomes a
feature.

## Phase 5: docs

- README: supported models and modules, and hand adjustment in the features list. fastlane
  full description.
- `research/hand-adjust-research.md`: experiment results. `research/battery-research.md`: the
  watch status layout picked by reply length.
- The world time findings above (the connect sequence and `world_time_nyc_2100.pcapng`) go into
  a research note before this plan is deleted.
- Delete this plan when the branch merges (as with earlier plans).

## Order

Phase 1 → 2 (one device test round) → 3 library + experiments → 3 UI → 4 when captures
arrive → 5. One commit per step, `feat:`/`docs:`/`test:` style.
