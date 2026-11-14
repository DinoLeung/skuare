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
(`30 01`…`30 05`, 18 bytes each), all empty. These are the B5000 sizes, so the GA-B2100 has
**5 reminders**. Writing one hasn't been captured yet.

## Watch settings (0x13) and connection settings (0x11)

```
11 0f 0f 0f 06 00 00 00 00 00 01 00 00 26 03     at start and end
13 03 00 00 01 00 00 00 00 00 00 00              4 reads, unchanged
```

- The app reads `0x13` once each for key sounds, light, watch display and power saving.
  Summer time sent nothing, and connection time with app is `0x11`.
- 12 bytes, which parses with skuare's B5000 layout: byte 1 `03` = 24 h + key tone muted,
  byte 2 `00` = short light, byte 4 `01` = DD.MM, byte 5 (weekday language) `00`, byte 8
  (font) `00`.
- The settings menu has no weekday language and no font style.
- Nothing was written, so which bit each B2100 setting uses isn't confirmed yet.

## Open questions

- Which `0x13` bits the B2100's key sounds, light, watch display and power saving settings
  use (needs a capture that changes each one and saves).
- What summer time in settings writes (probably `1e` of the home clock in a `21` session).
- Reminder writes: confirm the B5000 layout.
- Alarm byte 1 (`0x40`): does the watch accept skuare's `00`?
- Timer write: does the watch accept skuare's 8-byte packet, or does it need 15?
