# Battery research

Battery level findings from official Casio app HCI snoop captures (2026-10-16/17) and a logcat
of skuare on the GW-BX5600 (2026-10-18).

## Captures

| File | Watch | Module | App |
|---|---|---|---|
| `connect_time_sync.pcapng` | `CASIO GMW-B5000#` | 3539 | 2/5 |
| `connect_time_sync_gold.pcapng` | `CASIO GMW-B5000` | 3459 | 3/5 |
| `connect_time_sync_green.pcapng` | `CASIO GW-B5600#` | 3540 | 4/5 |
| `connect_time_sync_bx.pcapng` | `CASIO GW-BX5600` | 3578 | 5/5 |

## Where the app reads it

The official app's 1–5 bars come from the watch status, not from 0x28 (watch condition):

| Module | 0x28 reply | Watch status battery byte | App |
|---|---|---|---|
| 3539 | `28 0f 1d 00` | byte 7 = `11` | 2/5 |
| 3459 | `28 0f 19 00` | byte 7 = `12` | 3/5 |
| 3540 | `28 13 1a 01` | byte 7 = `13` | 4/5 |
| 3578 | `28 18 1b 00 00 00 02 00 00` | byte 158 = `18` | 5/5 |

- 0x28 byte 1 doesn't match the bars: `0f` showed as 2/5 on one watch and 3/5 on another.
  Byte 2 is the temperature in °C on all four.
- The BX 0x28 reply is 9 bytes, 5 more than the older modules.

## Watch status transfer

| Handle | UUID | Properties | Use |
|---|---|---|---|
| 0x0011 | `26eb0023-…` | notify, write | watch status request (GShockAPI: DATA_REQUEST_SP) |
| 0x0014 | `26eb0024-…` | notify, write w/o response | watch status data (GShockAPI: CONVOY) |

- The app enables notifications on 0x0014, writes `00 05 ab 00 00` to 0x0011, gets one
  notification on 0x0014, then writes `04 05 ab 00 00` to end the transfer.
- The BX request is `00 05 d8 00 00` → 216 bytes (older modules `ab` → 171 bytes). The size
  field is ignored: skuare sent `00 05 ab 00 00` and the BX still replied with 216 bytes
  (logcat 2026-10-18). The reply size is fixed per model.
- The app picks the size (`ab` vs `d8`) right after 0x26, before it reads 0x20, so it decides
  from the advertised name.

## Layout, older modules (171 bytes)

Starts with a timestamp `yy mm dd hh mm`, e.g. `26 10 16 22 41 01 01 12 86 …`. Byte 7 is the
battery: `11` = 2/5, `12` = 3/5, `13` = 4/5. Bytes 12–23 repeat the battery level, probably a
battery history. Parsed by skuare's `WatchStatus`.

## Layout, GW-BX5600 (216 bytes)

Completely different, no timestamp at the start. From the capture:

- [0..143] `ff 7f ff 00 00 00` × 24, probably empty hourly slots
- [144..157] `00 3f` × 7
- [158] battery `18`, [159] probably temperature `1c` (28 °C)
- rest `ff` / `7f` filler, ends `00 00`

A day later (logcat 2026-10-18) 14 of the 24 slots were filled, e.g. `18 20 00 00 00 00`,
`18 1d 01 00 00 00`, `18 19 00 00 01 00`. Looks like an hourly log: byte 0 battery (`18` in all
14), byte 1 maybe temperature (`20` 32 °C … `18` 24 °C), meaning of the `01` bytes unknown.
[158] was still `18`, [159] was `20`. The tail had more values than the capture: [160..162]
`18 18 18`, [178..180] `19 19 1d`, [196..198] `00 00 01`, [214] `3d`. Maybe more logs with
`ff` / `7f` as the empty marker, not checked.

On the BX the 0x28 battery byte and the watch status battery byte agree (`18`).

## What GShockAPI does

[izivkov/GShockAPI](https://github.com/izivkov/GShockAPI) 1.7.8, `api/.../WatchInfo.kt`:

- Battery from 0x28 byte 1, linear `(raw - lower) / (upper - lower)`.
- Limits 9..19 for the B5000 family, which doesn't match the official app (see above).
- Limits 14..24 for the GW-BX5600, which would put `18` at full, matching the 5/5 observed.

## In skuare

`WatchStatus` only knows the 171-byte layout and requires exactly 171 bytes, so it fails on the
BX (confirmed in the logcat). `requestWatchStatus()` only logs the error.

## To do

- Watch status request size and parser per model.
- BX battery scale.

## Open questions

- BX battery scale (only 5/5 observed).
- Whether the filled BX watch status slots are an hourly battery / temperature log.
