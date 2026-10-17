# GW-BX5600 (module 3578) research

Notes from official Casio app HCI snoop captures, collected 2026-10-16/17. Nothing here is
implemented in skuare yet.

## Captures

| File | Watch | Module | Notes |
|---|---|---|---|
| `connect_time_sync_gold.pcapng` | `CASIO GMW-B5000` | 3459 | app showed 3/5 battery |
| `connect_time_sync.pcapng` | `CASIO GMW-B5000#` | 3539 | app showed 2/5 battery |
| `connect_time_sync_green.pcapng` | `CASIO GW-B5600#` | 3540 | app showed 4/5 battery |
| `connect_time_sync_bx.pcapng` | `CASIO GW-BX5600` | 3578 | app showed 5/5 battery, 3 clocks |
| `world_clock_write_bx.pcapng` | GW-BX5600 | 3578 | world clock 2 changed Adelaide → Tokyo |
| `alarms_bx.pcapng` | GW-BX5600 | 3578 | alarm 1 switched on |
| `timer_bx.pcapng` | GW-BX5600 | 3578 | timer 10 min → 3 min |
| `disaply font_bx.pcapng` | GW-BX5600 | 3578 | display font changed |

3459, 3539 and 3540 use an identical protocol. Everything below is about how 3578 differs.

## Model detection

- Module numbers never appear on the wire (searched BCD, binary and ASCII).
- Advertising data only has flags, service `0x1804`, TX power and the name. No manufacturer or
  service data.
- The official app picks the watch status size (`ab` vs `d8`) right after 0x26, so the
  decision is made before it reads 0x20. The advertised name is the only thing that tells
  the BX apart at that point.
- Trailing `#` on the name marks the newer modules (3539/3540). It is part of the name, not
  noise.
- 0x20 (version information) bytes 12–13 also split the modules, could be a more reliable signal:

  | Module | 0x20 reply |
  |---|---|
  | 3459 | `20 23 07 01 01 00 00 00 00 00 00 00 00 00 …` |
  | 3539 / 3540 | `20 23 07 01 01 00 00 00 00 00 00 00 01 01 …` |
  | 3578 | `20 23 03 01 01 00 00 00 00 00 00 00 03 17 …` |

  Meaning unknown, only 3 data points.

### What GShockAPI does

[izivkov/GShockAPI](https://github.com/izivkov/GShockAPI) 1.7.8 (used by CasioGShockSmartSync),
`api/.../WatchInfo.kt`:

- Exact match of the BLE device name (with `CASIO ` removed) in `exactModelMap`, unknown names
  fall back to `GENERIC`. No 0x20, module ID or GATT checks.
- `GMW-B5000` → `GMW`, `GMW-B5000#` / `GW-B5600#` → `GCW_B5000` (commented as module 3539),
  `GW-BX5600` → `GW_BX5600` with `MipProtocol`, `hasMultipleFonts`, battery limits 14..24.
- Battery from 0x28 byte 1, linear `(raw - lower) / (upper - lower)`, 9..19 for the B5000
  family. This does not match the official app on the B5000s, see below.

## GATT

The BX has two extra characteristics in the Casio service (`26eb000d-…`):

| Handle | UUID | Properties | Use |
|---|---|---|---|
| 0x000c | `26eb002c-…` | write w/o response | requests (same as older modules) |
| 0x000e | `26eb002d-…` | notify, write | replies / writes (same as older modules) |
| 0x0011 | `26eb0023-…` | notify, write | watch status request (GShockAPI: DATA_REQUEST_SP) |
| 0x0014 | `26eb0024-…` | notify, write w/o response | watch status data (GShockAPI: CONVOY) |
| **0x0017** | **`26eb002e-…`** | write w/o response | **batched requests, BX only** |
| **0x0019** | **`26eb002f-…`** | notify, write | **batched replies / writes, BX only** |

The app enables notifications on 0x0019 at connect. MTU negotiated to 517.

## Battery / watch status

| Module | 0x28 reply | Watch status battery byte | App |
|---|---|---|---|
| 3539 | `28 0f 1d 00` | byte 7 = `11` | 2/5 |
| 3459 | `28 0f 19 00` | byte 7 = `12` | 3/5 |
| 3540 | `28 13 1a 01` | byte 7 = `13` | 4/5 |
| 3578 | `28 18 1b 00 00 00 02 00 00` | byte 158 = `18` | 5/5 |

- BX 0x28 reply is 9 bytes (5 bytes longer). Byte 2 is still temperature (27 °C).
- BX watch status request is `00 05 d8 00 00` → 216 bytes (older modules `00 05 ab 00 00` → 171).
  The size field is used, the reply length matched exactly.
- BX watch status layout is completely different, no timestamp at the start:
  - [0..143] `ff 7f ff 00 00 00` × 24, probably empty hourly slots
  - [144..157] `00 3f` × 7
  - [158] battery `18`, [159] probably temperature `1c` (28 °C)
  - rest `ff` / `7f` filler, ends `00 00`
- On the BX the 0x28 battery byte and the watch status battery byte agree (0x18). Scale is
  unknown from one reading; GShockAPI's 14..24 range would put 0x18 at full.
- skuare's current `WatchStatus` would fail on the BX (wrong request size, then size check or
  `BatteryLevel.fromByte`). `requestWatchStatus()` only logs the error.

## World time (batched)

Only world time uses the batched characteristics. Alarms, timer and settings stay on
0x000c / 0x000e.

### Batch format

- Request on 0x0017: `count, (opcode, index) × count`, e.g. `05 1d00 1d00 2400 2401 2402`
- Reply / write on 0x0019: `count, (length16 LE, record) × count`, e.g.
  `05 | 0f00 1d00… | 0f00 1d02… | 1400 2400… | …`
- Each record is the same packet as the older modules (`1d`, `1e`, `1f`, `24`).
- For `1d` and `1e` the index is ignored, the watch walks the records itself (`1d00` twice →
  `1d00`, `1d02`; `1e00` three times → `1e00`, `1e01`, `1e02`). `1f` and `24` use the index.

### Clocks and names

- 3 clocks (home + 2 world). Clock record `1d02` has the unused 4th slot: read as `0000`,
  written back as `ffff`.
- Names come in pairs: full name at index, 3-letter short name at index + 6
  (`1f00` ADELAIDE / `1f06` ADL, `1f01` HONG KONG / `1f07` HKG, `1f02` / `1f08`).
- Clock flag bytes on clocks 0 and 1 are `07` / `06` (B5000: `03` / `02`). The extra `0x04`
  bit is unknown; the app keeps it untouched, so preserve it on write.

### Connect sync (`connect_time_sync_bx`)

```
0x0017  05 1d00 1d00 2400 2401 2402    → 0x0019 reply (5 records)
0x0019  02 | 1d00… | 1d02…             (clocks written back)
0x0017  03 1e00 1e00 1e00              → reply
0x0019  06 | 1e00 1e01 1e02 2400 2401 2402
0x0017  06 1f00 1f06 1f01 1f07 1f02 1f08 → reply
0x0019  06 | same six name records
0x000e  09 …                           (time write on the old IO characteristic)
```

No `21` session on connect sync.

### User write (`world_clock_write_bx`)

Same sequence, wrapped in `21 00 01` … `21 01 01` on 0x000e, like the B5000.
Each group is read in a batch then the whole group is written back.

| Record | Read (Adelaide) | Written (Tokyo) |
|---|---|---|
| `1d02` | `1d 02 03 03 00 0500 0000 ff…` | `1d 02 03 02 00 1901 ffff ff…` |
| `1e02` | `1e 02 0500 26 04 04` | `1e 02 1901 24 04 00` |
| `2402` | Adelaide coordinates | `… 40 41 d8 41 35 54 75 a3 40 61 76 22 7d 02 8a 1e 02` |
| `1f02` / `1f08` | `ADELAIDE` / `ADL` | `TOKYO` / `TYO` |

- Records are byte-for-byte the same as the B5000 for the same city (Tokyo `0x0119`), so
  skuare's packet builders can be reused.
- The app also rewrote `2401` (Hong Kong), which wasn't edited: the watch had zero
  coordinates with radio ID 03, the app wrote real coordinates with radio ID 05. The app
  rewrites coordinates from its own city database.

## Alarms (`alarms_bx`)

Same opcodes and sizes as the B5000 (0x15 5 bytes, 0x16 17 bytes), old characteristics.

```
read   15 00 00 00 00                          write  15 40 40 00 00
read   16 00000000 00000000 00000000 00000000  write  16 00400000 00400000 00400000 00400000
```

The app sets `0x40` in byte 1 of every alarm even when the watch reports `00`. Also seen on
a non-BX watch (`hourly_signal_on_to_off.pcapng`), so not BX specific. Meaning unknown,
skuare's `Alarm.bytes` always writes `00` there.

## Timer (`timer_bx`)

```
read   18 00 0a 00 00000000000000000000000   (10 min)
write  18 00 03 00 00000000000000000000000   (3 min)
```

15 bytes instead of 8; hours / minutes / seconds in the same place. skuare's
`Timer.fromPacket` requires 8 bytes and `Timer.packet` writes 8.

## Display font (`disaply font_bx`)

```
read   13 17 00 00 00 00 00 00 00 00 00 00
write  13 17 00 00 00 00 00 00 20 00 00 00
```

Settings byte 8 = `0x20` is the font (GShockAPI `SettingsIO`: `0x20` = Classic). skuare's
`WatchSettings` doesn't read byte 8 and writes `00`, so saving settings would reset the font.

## Other

- No reminders on this model.
- Time write (0x09) still goes to 0x000e. GShockAPI uses a separate `GwBx5600TimeIO` for it
  (not compared yet).

## To do for BX support

1. Model profile from the advertised name (keep the `#`), log 0x20 bytes 12–13 as a cross-check.
2. Watch status request size and parser per model, BX battery scale.
3. Batched world time on 0x0017 / 0x0019 with 3 clocks and short names.
4. 15-byte timer packet.
5. Preserve unknown bytes on write: alarm byte 1, settings byte 8, clock flag `0x04`.

## Open questions

- What 0x20 bytes 12–13 encode.
- BX battery scale (only 5/5 observed).
- Whether the BX still answers single 0x1d / 0x1e / 0x1f / 0x24 requests on 0x000c.
- Meaning of the alarm `0x40` byte and the clock flag `0x04` bit.
- Whether the app uses the name or a value saved at pairing (needs a first-pairing capture).
