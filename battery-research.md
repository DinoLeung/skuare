# Battery research

Battery level findings from official Casio app HCI snoop captures (2026-10-16/17, 2026-11-12
and 2026-11-13), a logcat of skuare on the GW-BX5600 (2026-10-18), the GW-BX5600's diagnostic
menu and the GA-B2100's battery indicator.

Battery values are hex as sent, with decimal in brackets where it helps.

## Summary

- The app's 1–5 bars come from the **battery byte in the watch status**, not from 0x28 byte 1
  (watch condition), which reads `0f` (15) at 1/5, 2/5 and 3/5.
- Working model: the GMW-B5000, GW-B5600 and GA-B2100 share one range, and the MIP GW-BX5600
  has its own (see [Levels](#levels)):

  | Raw | Bars | Seen on |
  |---|---|---|
  | `0f` (15) | 1/5 (lowest level) | GA-B2100 |
  | `10` (16) | 1/5 | |
  | `11` (17) | 2/5 | 3539, GA-B2100 |
  | `12` (18) | 3/5 | 3459 |
  | `13` (19) | 4/5 | 3540 |
  | `14` (20) | 5/5, top | |

  BX: `17` (23) and `18` (24) are both 5/5, the rest of its range is unknown.
- The range is best picked by model (advertised name). The only packet field that separates the
  BX is 0x20 byte 2 (`03` vs `07`), not yet confirmed as a MIP marker.
- Two alternatives still fit every value: one scale for all modules, and a shared `0f`–`18`
  range on the 9-byte-0x28 watches. A GA-B2100 capture at 3/5 tells them apart.
- skuare: `BatteryLevel` maps exactly `10`–`14` and throws on anything else, including `0f` and
  the BX's `17`/`18`. `WatchStatus` only parses the 171-byte layout, so it fails on the BX
  (216 bytes) and the GA-B2100 (172 bytes).

## Captures

All captures are in `~/Documents`.

| File | Date | Watch | Module | FW | App | Watch's own level |
|---|---|---|---|---|---|---|
| `connect_time_sync.pcapng` | 10-16 | `CASIO GMW-B5000#` (silver) | 3539 | 01.01 | 2/5 | |
| `connect_time_sync_gold.pcapng` | 10-16 | `CASIO GMW-B5000` (gold) | 3459 | 01.01 | 3/5 | |
| `connect_time_sync_green.pcapng` | 10-16 | `CASIO GW-B5600#` (green) | 3540 | 01.01 | 4/5 | |
| `connect_time_sync_bx.pcapng` | 10-17 | `CASIO GW-BX5600` | 3578 | 01.01 | 5/5 | |
| `pairing_bx.pcapng` | 11-12 15:54 | `CASIO GW-BX5600` | 3578 | 01.01 | 5/5 | menu H1, 2.52 V, 31 °C |
| `update_01_02_bx.pcapng` | 11-12 ~16:00 | `CASIO GW-BX5600` | 3578 | → 01.02 | | |
| `connect_time_sync_alt_bx.pcapng` | 11-12 16:04 | `CASIO GW-BX5600` | 3578 | 01.02 | 5/5 | menu H2, 2.435 V, 31 °C |
| `connect_time_sync_bat2_silver.pcapng` | 11-12 16:12 | `CASIO GMW-B5000#` (silver) | 3539 | 01.01 | 2/5 | |
| `connect_time_sync_bat3_gold.pcapng` | 11-12 16:17 | `CASIO GMW-B5000` (gold) | 3459 | 01.01 | 3/5 | |
| `connect_time_sync_bat4_green.pcapng` | 11-12 16:20 | `CASIO GW-B5600#` (green) | 3540 | 01.01 | 4/5 | |
| `connect_time_sync_bat1_2100.pcapng` | 11-13 16:30 | `CASIO GA-B2100` | 5594 | 03.01 | 1/5 | dial M |
| `connect_time_sync_bat1_2100_1.pcapng` | 11-13 17:19 | `CASIO GA-B2100` | 5594 | 03.01 | 1/5 | dial M, L right after |
| `connect_time_sync_bat2_charging_2100.pcapng` | 11-13 19:04 | `CASIO GA-B2100` | 5594 | 03.01 | 2/5 | dial M, charging under a lamp |

Firmware update details are in `firmware-update-research.md`.

## Observed values

Watch status battery byte: [7] on the 171-byte layout (3459/3539/3540), [158] on the BX (216),
[110] on the GA-B2100 (172).

| Watch | Date | Watch's own level | App | Watch status battery | 0x28 byte 1 | Full 0x28 |
|---|---|---|---|---|---|---|
| 3539 (silver) | 10-16 | | 2/5 | `11` (17) | `0f` (15) | `28 0f 1d 00` |
| 3539 (silver) | 11-12 | | 2/5 | `11` (17) | `0f` (15) | `28 0f 1d 00` |
| 3459 (gold) | 10-16 | | 3/5 | `12` (18) | `0f` (15) | `28 0f 19 00` |
| 3459 (gold) | 11-12 | | 3/5 | `12` (18) | `0f` (15) | `28 0f 1c 00` |
| 3540 (green) | 10-16 | | 4/5 | `13` (19) | `13` (19) | `28 13 1a 01` |
| 3540 (green) | 11-12 | | 4/5 | `13` (19) | `13` (19) | `28 13 1f 00` |
| BX 3578 | 10-17 | | 5/5 | `18` (24) | `18` (24) | `28 18 1b 00 00 00 02 00 00` |
| BX 3578 | 11-12 15:54 | H1 (menu) | 5/5 | `18` (24) | `18` (24) | `28 18 1f 00 00 00 02 00 00` |
| BX 3578 | 11-12 16:04 | H2 (menu) | 5/5 | `ff` (wiped) | `17` (23) | `28 17 1f 23 00 00 02 00 00` |
| GA-B2100 5594 | 11-13 16:30 | M (dial) | 1/5 | `0f` (15) | `0f` (15) | `28 0f 19 01 00 00 02 00 00` |
| GA-B2100 5594 | 11-13 ~16:45 | L (dial), no Bluetooth | | | | |
| GA-B2100 5594 | 11-13 17:19 | M (dial) | 1/5 | `0f` (15) | `0f` (15) | `28 0f 19 00 00 00 02 00 00` |
| GA-B2100 5594 | 11-13 ~17:20 | L (dial), disconnected | | | | |
| GA-B2100 5594 | 11-13 19:04 | M (dial), charging | 2/5 | `11` (17) | `0f` (15) | `28 0f 19 00 00 00 02 00 00` |

Every 0x28 read within a capture returned the same reply (2–3 reads per capture).

### B5000 family (3459, 3539, 3540)

- Every watch held its level for the month between captures, in both bytes.

### GW-BX5600

- At H2 the watch status had just been wiped by the 01.02 firmware update: [158] is `ff` and
  [159] (temperature) `7f`, the same empty markers as the unfilled hourly slots. So the app's
  5/5 came from 0x28 byte 1 (`17`) or from a value cached before the update. A weaker point
  than H1.

### GA-B2100

- Delivered flat and charged under a lamp until it connected. Its own indicator (second hand
  on H/M/L) showed **M** while the app showed **1/5**, so the app's lowest bar isn't the
  watch's L, and a watch at 1/5 still connects.
- After two hand-adjust sessions (16:40 and 16:41, lots of hand movement) the dial showed
  **L** and the watch **refused to enter Bluetooth mode**. So `0f` at M is its last connectable
  level, and the app can't read the value at L.
- At 17:19 the battery list (byte 114) had one entry, `0e` (14). Not treated as a battery
  level: the third list (byte 150) got the same `0e`, on the BX that list holds unrelated
  values, and by 19:04 all three lists were empty again, probably wiped by the second drop to
  L. What `0e` means there is unknown.
- At 17:19 it was back at M and 1/5, and shortly after the sync **the watch disconnected
  itself and the dial dropped to L** again. Both drops came right after load (hand movement,
  Bluetooth sync), so the watch sits on the M/L boundary (`0f`/`0e`), the level reacts to load
  at least near that boundary, and reaching L ends the connection from the watch's side.
- At 19:04, charging under the lamp during the capture: dial still M, app **2/5**, watch status
  **`11` (17)**, 0x28 byte 1 still `0f`. 1/5 → 2/5 went `0f` → `11`, so `10` (16) still hasn't
  been seen. The charge current may lift the reading the way load lowered it near L.

## 0x28 watch condition

The app writes `28` to the request characteristic (0x000c); the watch replies on 0x000e. 4
bytes on the B5000 family, 9 on the BX and GA-B2100.

| Byte | Meaning |
|---|---|
| 0 | opcode `28` |
| 1 | battery-like value, but not what the bars follow (below) |
| 2 | temperature in °C, confirmed against the BX diagnostic menu twice (`1f` = 31 °C) |
| 3 | unknown. Usually `00`. `01` on the 3540 in October and on the GA-B2100 at 16:30 (just back from flat), `23` on the BX right after the 01.02 update; `00` again in each watch's next capture. Maybe a flag for a recent event |
| 4–8 | BX and GA-B2100 only: `00 00 02 00 00` in every capture |

Byte 1 vs the watch status battery byte:

| App | Watch | Watch status | 0x28 byte 1 |
|---|---|---|---|
| 1/5 | GA-B2100 | `0f` (15) | `0f` (15) |
| 2/5 | 3539 | `11` (17) | `0f` (15) |
| 2/5 | GA-B2100 | `11` (17) | `0f` (15) |
| 3/5 | 3459 | `12` (18) | `0f` (15) |
| 4/5 | 3540 | `13` (19) | `13` (19) |
| 5/5 | BX | `18` (24) | `18` (24) |

- Byte 1 equals the watch status from `13` up and reads `0f` below that, so it looks like a
  **floor**, not an offset (the gap is 2 on the 3539 and 3 on the 3459). Three watches at
  three levels all landing on exactly `0f` argues against a voltage sag under radio load.
- **Confirmed on the GA-B2100** at 2/5: `0f` in 0x28 with `11` in the watch status, the same
  as the 3539. Its 9-byte 0x28 behaves like the B5000 family's 4-byte one.
- Remaining test: the silver 3539 at 1/5 (floor: `0f` with `10`; sag: below `0f`).

## Watch status transfer

| Handle | UUID | Properties | Use |
|---|---|---|---|
| 0x0011 | `26eb0023-…` | notify, write | session control (GShockAPI: DATA_REQUEST_SP) |
| 0x0014 | `26eb0024-…` | notify, write w/o response | data (GShockAPI: CONVOY) |

- The app enables notifications on 0x0014, writes `00 05 ab 00 00` to 0x0011, gets one
  notification on 0x0014, then writes `04 05 ab 00 00` to end the transfer. The same channel
  carries firmware updates (session `01`), see `firmware-update-research.md`.
- The third byte is the size: `ab` → 171 bytes (B5000 family), `ac` → 172 (GA-B2100), `d8` →
  216 (BX). The watch ignores it: skuare sent `00 05 ab 00 00` and the BX still replied with
  216 bytes (logcat 2026-10-18). The reply size is fixed per model.
- The app picks the size right after 0x26, before it reads 0x20, so it decides from the
  advertised name.

## Watch status layout, B5000 family (171 bytes)

| Bytes | Meaning |
|---|---|
| 0–4 | timestamp `yy mm dd hh mm`, e.g. `26 11 12 16 12` |
| 5–6 | `01 01` in every capture |
| 7 | **battery**: `11` = 2/5, `12` = 3/5, `13` = 4/5 |
| 8 | `86`–`94`, maybe part of the temperature log |
| 12–23 | battery history, 12 entries, all equal to byte 7 in every capture |
| 24–35 | **temperature log**, 12 entries, about 1 °C per step |

The temperature log on the three watches 5 minutes apart on 11-12, sitting in the same place:

```
3539  8c 87 82 7d 7b 7d 7e 7f 82 85 87 86   live 29 °C
3459  8c 87 82 7d 7b 7c 7d 7f 82 85 87 86   live 28 °C
3540  90 8a 85 81 7e 7f 81 82 85 88 95 89   live 31 °C
```

The 3540 runs about 3 higher in every entry, matching its 2–3 °C higher live temperature, plus
one spike (`95`) where it was probably handled. The last entry minus the live temperature is
`0x69`–`0x6a` on all three (104–109 in October), so maybe °C + ~`0x6a`. Which end is newest
isn't settled. Parsed (battery only) by skuare's `WatchStatus`.

## Watch status layout, GW-BX5600 (216 bytes)

No timestamp at the start.

| Bytes | Meaning |
|---|---|
| 0–143 | 24 hourly slots of 6 bytes, empty = `ff 7f ff 00 00 00` |
| 144–157 | 7 pairs `xx 3f`, e.g. `04 3f 4f 3f 0b 3f 00 3f …` (empty `00 3f`) |
| 158 | **battery** (`18`) |
| 159 | temperature in °C, close to the live reading (`20` = 32 °C at a live 31 °C) |
| 160–177 | list, `18` × 6 on 11-12 (battery?), empty `ff` |
| 178–195 | list, `18 15 19 16 17 1b` on 11-12 (temperatures?), empty `7f` |
| 196–213 | list, `03 00 01 00 00 06` on 11-12, empty `ff` |
| 214 | unknown (`3d`, `57`) |

Hourly slot: byte 0 battery (`18` in all 24), byte 1 temperature in °C (a 20–29 °C daily
curve on 11-12), byte 2 unknown (nonzero in a few slots, up to `08`), bytes 3–4 only set in
slot 0 (`28 02`). Whether slot 0 is the newest hour or 00:00 isn't known.

All lists were empty on 10-17, partly filled a day later, full on 11-12, and **wiped by the
01.02 firmware update**.

## Watch status layout, GA-B2100 (172 bytes)

Captured right after the watch came back from flat, so almost everything is empty. The tail
matches the BX's from the battery byte on.

| Bytes | Meaning |
|---|---|
| 0–1 | `00 05` (same as the request's first two bytes; maybe a header, maybe log data) |
| 2–109 | all `00`, probably an empty log |
| 110 | **battery**: `0f` = 1/5, `11` = 2/5 |
| 111 | temperature in °C (`19` = 25 °C, same as 0x28 byte 2) |
| 112–113 | unknown: `0f 00` at 16:30, `03 00` at 17:19, `13 00` at 19:04 |
| 114–131 | battery list (BX: 160–177), empty `ff`. 17:19: first entry `0e` |
| 132–149 | temperature list in °C (BX: 178–195), empty `7f`. 17:19: first entry `1d` (29 °C) |
| 150–167 | unknown list (BX: 196–213), empty `ff`. 17:19: first entry `0e` |
| 168–171 | `00 00 00 00` |

The lists fill from the front. They were empty at 16:30, had one entry at 17:19 and were empty
again at 19:04, after the watch dropped to L at ~17:20, so L probably clears them. Bytes 0–109
were still all `00` at 19:04. A capture after a few days of wear should show whether they are hourly slots
like the BX's.

## Levels

### The watches' own indicators

- **BX diagnostic menu:** level, voltage and temperature. Photos of other MIP watches' menus
  show M1 at 2.40 V, so the levels are probably H1, H2, M1, M2, L1, L2. Seen: H1 (2.52 V) =
  `18`, H2 (2.435 V) = `17`, both 5/5 in the app.
- **GA-B2100 dial:** the second hand points at H, M or L. M = `0f` (1/5) and `11` (2/5,
  charging); at L Bluetooth is off, so no value can be read.

The app's bars don't follow these directly: the GA-B2100 at M shows 1/5. The battery byte held
for weeks on every watch and moved one step with H1 → H2 on the BX, so it's most likely the
watch's level index rather than a voltage (see [Battery cell](#battery-cell)).

### Working model (user, 2026-11-13)

The GMW-B5000, GW-B5600 and GA-B2100 share one range: `0f`/`10` (15/16) = 1/5, `11` (17) = 2/5,
`12` (18) = 3/5, `13` (19) = 4/5, `14` (20) = 5/5. Six internal levels, `0f`–`14`, with the
first bar covering two of them (`0f` and `10`). Nothing below `0f` is on the scale: at L the
watch turns Bluetooth off, so the app never reads a lower value. 15, 17, 18 and 19 are seen; 16
and 20 aren't.

The **MIP models** (BX) use a different range: two values at 5/5 (`17`/`18`, 23/24), so a
different shape, not just an offset. The rest of it is unknown.

### Alternatives that still fit

- **One scale for all modules:** `10` or less = 1/5, `11`, `12`, `13`, `14` and up = 5/5. The
  same as the working model for the non-MIP watches, but puts the BX on it too.
- **Shared `0f`–`18` on the 9-byte-0x28 watches (BX, GA-B2100), two values per bar:** `0f`/`10`
  = 1/5 … `17`/`18` = 5/5. This is GShockAPI's linear 14..24 for the BX.

GA-B2100 predictions:

| Raw | Working model / one scale | Shared `0f`–`18` |
|---|---|---|
| `0f` (15) | 1/5 ✓ | 1/5 ✓ |
| `10` (16) | 1/5 | 1/5 |
| `11` (17) | 2/5 ✓ | 2/5 ✓ |
| `12` (18) | 3/5 | 2/5 |
| `13` (19) | 4/5 | 3/5 |
| `14` (20) | 5/5 | 3/5 |

### Ruled out

- **The 0x28 length picks the range** (9 bytes → BX range `18` … `13`): the GA-B2100 has a 9-byte
  0x28 and reads `0f`, below that range.
- **0x28 byte 1 for the bars** (GShockAPI): it reads `0f` at 1/5, 2/5 and 3/5.

### Deciding tests

- **GA-B2100 at 3/5:** `12` → working model or one scale, `13` → shared `0f`–`18`.
- **Green 3540 fully charged:** `14` (20) and never higher → the working model's top.
- **Silver 3539 at 1/5:** `10` (16), or `0f` (15).
- **BX at `16`** (22, probably diag M1): 4/5 → working model or shared `0f`–`18`, 5/5 → one
  scale.
- **A second MIP watch** (GBD-H2000, DW-H5600): its range, and whether 0x20 byte 2 reads `03`.

### Range markers in the packets

Compared every 0x000e reply of all five modules (11-12 and 11-13 captures) for anything that
could declare a battery range:

| Reply | 3539 | 3459 | 3540 | BX (MIP) | GA-B2100 |
|---|---|---|---|---|---|
| 0x10 | address only differs | | | | |
| 0x11 byte 13 | `20` | `22` | `24` | `1e` | `26` |
| 0x11 bytes 6, 8 | `50 04` | `50 04` | `50 04` | `50 04` | `00 00` |
| 0x20 byte 2 | `07` | `07` | `07` | **`03`** | `07` |
| 0x20 bytes 13–14 | `01 01` | `00 00` | `01 01` | `03 17` | `03 17` |
| 0x26 | per-watch ID | | | | |
| 0x28 length | 4 | 4 | 4 | 9 | 9 |
| Watch status size | 171 | 171 | 171 | 216 | 172 |

0x1d/0x1e/0x1f, 0x22 and 0x24 are identical or missing on some watches.

**0x20 byte 2 is the only field that separates the BX from the other four.** It sits next to
the firmware version (bytes 3–4), so it may be a hardware or display type, but with one MIP
watch that can't be told apart from "the BX is just a different module". The official app
decides at least one thing per model from the advertised name before reading anything (the
watch status size), so it does carry per-model tables, and a per-model battery range would fit
that.

## Battery cell

The watches use a Panasonic CTL1616 (cobalt titanium lithium, rechargeable). Casio's manuals
name it for 3539 and 3578; the 3459 and 3540 manuals say only "one rechargeable battery".

From the [CTL1616F datasheet](https://energy.panasonic.com/dam/master/pdf/en/datasheet/coin-rechargeable/CTL1616F_Datasheet_EN.pdf):
nominal 2.3 V, charge 2.5–2.7 V (2.6 V standard), 13 mAh, rated capacity measured to a 2.0 V
cut-off. Discharge curve (20 kΩ, 20 °C), read off the chart:

| Capacity used | Voltage |
|---|---|
| 0 mAh (full) | ~2.6 V |
| 2 mAh | ~2.45 V |
| 4 mAh | ~2.4 V |
| 6 mAh | ~2.35 V |
| 8–12 mAh | ~2.3 V, flat |
| ~13 mAh | knee, drops to 2.0 V |

- The raw bytes aren't decivolts: at 0.1 V per step, 2/5–4/5 would be 1.7–1.9 V, below the
  cut-off, and the BX's `18` would be 2.4 V while the menu showed 2.52 V.
- H1 → H2 on the BX was one step for 0.085 V, while M1 is only 0.035 V below H2, so the steps
  aren't even in voltage either.
- From ~40% to ~95% used the cell stays within about 0.05 V of 2.3 V, so voltage-based levels
  can't be linear in capacity. The bars are thresholds, and GShockAPI's linear scale is wrong.

## What GShockAPI does

[izivkov/GShockAPI](https://github.com/izivkov/GShockAPI) 1.7.8, `api/.../WatchInfo.kt`:

- Battery from 0x28 byte 1, linear `(raw - lower) / (upper - lower)`.
- Limits 9..19 for the B5000 family, which can't tell 2/5 from 3/5 (both `0f`).
- Limits 14..24 for the GW-BX5600, which puts `18` at full and `0f` at 10 %.

## In progress (from 2026-11-12)

- Silver 3539 (2/5, `11`) kept in the dark to reach 1/5. Expected `10` (16).
- Green 3540 (4/5, `13`) worn and in light to reach 5/5. Expected `14` (20).
- GA-B2100: 2/5 (`11`) at 19:04 while charging. Next capture at 3/5, ideally a few minutes
  away from the lamp, noting the dial.

The battery history (B5000: bytes 12–23, GA-B2100: 114–131) shows a change even if the capture
is taken later. Note the app's bars, any [LOW]/[CHG] indicator, and the watch's own level
(diag menu or dial) with each capture.

## To do

- `BatteryLevel` per range (non-MIP vs MIP, picked by model), with thresholds rather than exact
  values, once the tests above settle the model.
- Watch status request size and parser per model (BX: 216 bytes, battery at [158]; GA-B2100:
  172 bytes, battery at [110]).

## Open questions

- B5000-family 1/5 and 5/5 raw values.
- BX values below `17`, and the shape of the MIP range.
- GA-B2100 raw values at each bar and at dial H.
- Whether 1/5 on the B5000 family is still connectable or already [LOW] (manual: [LOW] and
  [CHG] turn off phone connections). The GA-B2100 connects at 1/5 (dial M) and refuses
  Bluetooth at dial L.
- The GA-B2100 has the batched characteristics (`26eb002e` at 0x0017, `26eb002f` at 0x0019)
  and the app enabled notifications on 0x0019, but read world time with single 0x1d/0x1e/0x1f
  packets (2 cities). skuare's `Watch.hasBatchCharacteristics` would pick the batched path on
  this watch, which the official app never used here.
- 0x28 byte 3, and bytes 4–8 on the 9-byte replies.
- Which end of the temperature log and the BX hourly slots is newest.
