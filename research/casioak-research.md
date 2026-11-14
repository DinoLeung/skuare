# CASIOAK research (GA-B2100)

Official Casio app on a GA-B2100 (module 5689). The GBM-2100 (5729) and GM-B2100 (5691) are
assumed to behave the same, but haven't been captured. Hand adjustment is in
`hand-adjust-research.md`, battery and watch status in `battery-research.md`.

| Capture | Date | What the user did |
|---|---|---|
| `connect_time_sync_bat*_2100.pcapng` | 11-13 | connect with time sync |
| `world_time_nyc_2100.pcapng` | 11-13 | set the world clock to New York |
| `alarm_set_5_2100.pcapng` | 11-14 | set alarm 5 to 13:00 |
| `timer_2100.pcapng` | 11-14 | set the timer from 10 to 3 minutes |
| `reminder_2100.pcapng` | 11-14 | opened the reminders screen |
| `settings_2100.pcapng` | 11-14 | opened each setting in turn: key sounds, light, watch display, power saving, summer time, connection time with app |
| `settings_set_2100.pcapng` | 11-14 | key sound on, light 1.5 s → 3 s, 12 h + MM/DD, power saving off; connection time left at 3 min |
| `summer_time_2100.pcapng` | 11-14 | summer time on, off, then auto |
| `reminders_2100.pcapng` | 11-14 | set all 5 reminders, one per repeat type |

The watch had just come back from flat, so everything it reported was empty or at its defaults.

## Connect and world time

Connect: `22` → `10` → write `23` name → `11` read/write → `26` → `28` → watch status
(`00 05 ac`, 172 bytes) → `20`/`28` twice → second transfer (`00 01 …`) → `1d` → `1e` ×2 →
`1f` ×2 → writes `1d`, `1e` ×2, `1f` ×2 → `09` time.

- 2 clocks. City names have no 3-letter short names (only `1f00`, `1f01`).
- No `0x24` (coordinates / radio ID), on connect or on a world clock write.
- Only the single-packet IO pair (0x000c/0x000e), even though the watch has the batched
  characteristics (`26eb002e`/`26eb002f`).

World clock write (New York): `21 00 01`, read/write `1d`, `1e` ×2, `1f` ×2, `21 01 01`.

```
1d 00 01 03 02 7f76 ca00 ff…   world clock 1 → time zone 0x00ca
1e 01 ca00 ec 04 01            0x00ca, UTC offset ec (−20 quarter hours = −5 h), 04 01 (DST)
1f 01 "NEW YORK"
```

The capture ends 0.8 s after `21 01 01` with no `09` time write. skuare writes the time after
every clock write. It's unclear whether the capture was cut short.

## Alarms (0x15, 0x16)

Read, then written:

```
read   15 00 00 00 00
read   16 00000000 00000000 00000000 00000000
write  15 00 40 00 00
write  16 00400000 00400000 00400000 40400d00
```

- Same layout as the B5000: `0x15` = hourly signal flag + alarm 1, `0x16` = alarms 2–4 plus
  the fifth slot (the snooze alarm on the B5000). Each alarm is `<flags> <b1> <hh> <mm>`.
- Alarm 5 is a normal alarm in the fifth slot: `40 40 0d 00` = enabled (`0x40`), 13:00. It has
  no extra flag bit compared with the other alarms.
- **Byte 1 of every alarm is written as `0x40`**, also for disabled alarms, though the watch
  reported `00`. The official app does the same on the GW-BX5600. skuare writes `00`. What it
  means is unknown, maybe a daily/one-time mode.

## Timer (0x18)

```
read   18 000a00 0000000000000000000000   10:00
write  18 000300 0000000000000000000000   3:00
```

15 bytes in both directions, like the GW-BX5600's reply: `hh mm ss`, then the B5000 fields,
then zero padding. skuare reads 15 bytes but always writes 8.

## Reminders (0x30, 0x31)

The official app reads 5 reminder configs (`31 01`…`31 05`, 9 bytes each) and 5 titles
(`30 01`…`30 05`, 18 bytes each). These are the B5000 sizes, so the GA-B2100 has
**5 reminders**.

`reminders_2100.pcapng` sets all 5, writing config then title for each, with no `21` session:

```
31 01 01 261114 261114 00 00   once, 2026-11-14
30 01 "Reminder 1"
31 02 05 261114 261114 01 00   weekly, day bits 01 (Sunday in ReminderDayOfWeekBitmask)
30 02 "reminder 2"
31 03 11 261118 261118 00 00   monthly, 2026-11-18
30 03 "reminder 3"
31 04 09 271111 271111 00 00   yearly, 2027-11-11
30 04 "reminder 4"
31 05 0b 261114 261121 00 00   daily, 2026-11-14 to 2026-11-21
30 05 "reminder 5"
```

Every config parses with skuare's B5000 layout (`ReminderBitmask`, `ReminderRecurrence`).

## Watch settings (0x13) and connection settings (0x11)

```
11 0f 0f 0f 06 00 00 00 00 00 01 00 00 26 03     unchanged throughout (connection time 3 min)
13 03 00 00 01 00 00 00 00 00 00 00              before
```

The app reads `0x11` and `0x13` before each change. In `settings_set_2100.pcapng` each change
is written on its own:

| Change | Write | skuare's B5000 layout |
|---|---|---|
| key sound on | `13 01 00 00 01 …` | byte 1: `MUTE_TONE` (0x02) cleared |
| light 1.5 s → 3 s | `13 01 01 00 01 …` | byte 2: `BacklightDuration.SHORT` → `LONG` |
| 12 h and MM/DD | `13 00 01 00 00 …` | byte 1: `MILITARY_TIME` (0x01) cleared; byte 4: `DateFormat.DDMM` → `MMDD` |
| power saving off | `13 10 01 00 00 …` | byte 1: `POWER_SAVING_OFF` (0x10) set |

- **Same layout as the B5000.** 12 bytes. Byte 5 (weekday language) and byte 8 (font) stay
  `00` throughout.
- **Weekday language: yes, font style: no** (user, 2026-11-14). Language wasn't changed in the
  captures; skuare's existing byte 5 handling is expected to work as on the B5000.
- The "watch display" screen sets 12/24 h and the date order together in one write.
- The auto light bit (0x04) never changed, and the menu has no auto light setting.

## Summer time (home clock DST)

`summer_time_2100.pcapng`: summer time set to on, then off, then back to auto. Each change is a
full clocks write (read/write `1d`, `1e` ×2, `1f` ×2), and only the home clock's DST byte in
`1d` (byte 3) changes:

| Setting | `1d` byte 3 | `DstBitMask` |
|---|---|---|
| auto (start) | `03` | auto + on (Adelaide is on DST in November) |
| on | `01` | on |
| off | `00` | none |
| auto | `03` | auto + on |

- The DST bits are skuare's existing `DstBitMask`.
- **The session is `21 00 03` … `21 01 03`**, not `21 00 01` … `21 01 01` as for a world
  clock write. skuare always sends `01`. Maybe byte 2 says what changed (`01` world clocks,
  `03` home clock too?), but there isn't enough data to tell. No B5000 capture of a summer time
  change to compare with.
- No `09` time write after any of the three changes, so the watch shifts its own time.

## Tested with skuare (2026-11-14)

Device round on the GA-B2100, all working: connect, time sync, world clock edit, summer time
on/off/auto, alarms, timer, reminders, settings, background sync.

- **Batched characteristics work** for clocks and time zones, though the official app never
  uses them on this watch.
- **`21 00 01` works for a summer time change.** The watch doesn't need the official app's
  `03` session byte.
- **Alarm byte 1 `00` works.** The official app's `0x40` isn't needed.
- **15-byte timer writes work** (skuare now matches the reply size). Whether an 8-byte write
  would also work wasn't tested.

## Open questions

- What the clocks session's third byte (`01` vs `03`) means. Both work for a summer time change.
- What alarm byte 1 (`0x40` from the official app) means.
