# Battery research

Battery level findings from official Casio app HCI snoop captures (2026-10-16/17 and
2026-11-12), a logcat of skuare on the GW-BX5600 (2026-10-18), and the GW-BX5600's diagnostic
menu.

## Summary

- The app's 1–5 bars come from the **battery byte in the watch status**, not from 0x28
  (watch condition). On the B5000 family 0x28 byte 1 reads `0f` at both 2/5 and 3/5.
- The battery byte is most likely the **watch's own level index**, one step per level, not a
  voltage. On the BX it stepped from `18` to `17` exactly when the diagnostic menu went from H1
  to H2, and it otherwise holds for weeks.
- Raw values per bar so far: `11` = 2/5, `12` = 3/5, `13` = 4/5 (B5000 family),
  `17`/`18` = 5/5 (BX). The jump from `13` to `17` is probably a different scale per module
  family (see [Levels](#levels)). 1/5 and the B5000 5/5 haven't been seen.
- `BatteryLevel` maps exactly `10`–`14` and throws on anything else, including the BX's
  `17`/`18`. `WatchStatus` only parses the 171-byte layout, so it fails on the BX.

## Captures

All captures are in `~/Documents`.

| File | Date | Watch | Module | FW | App | Diag menu |
|---|---|---|---|---|---|---|
| `connect_time_sync.pcapng` | 10-16 | `CASIO GMW-B5000#` (silver) | 3539 | 01.01 | 2/5 | |
| `connect_time_sync_gold.pcapng` | 10-16 | `CASIO GMW-B5000` (gold) | 3459 | 01.01 | 3/5 | |
| `connect_time_sync_green.pcapng` | 10-16 | `CASIO GW-B5600#` (green) | 3540 | 01.01 | 4/5 | |
| `connect_time_sync_bx.pcapng` | 10-17 | `CASIO GW-BX5600` | 3578 | 01.01 | 5/5 | |
| `pairing_bx.pcapng` | 11-12 15:54 | `CASIO GW-BX5600` | 3578 | 01.01 | 5/5 | H1, 2.52 V, 31 °C |
| `update_01_02_bx.pcapng` | 11-12 ~16:00 | `CASIO GW-BX5600` | 3578 | → 01.02 | | |
| `connect_time_sync_alt_bx.pcapng` | 11-12 16:04 | `CASIO GW-BX5600` | 3578 | 01.02 | 5/5 | H2, 2.435 V, 31 °C |
| `connect_time_sync_bat2_silver.pcapng` | 11-12 16:12 | `CASIO GMW-B5000#` (silver) | 3539 | 01.01 | 2/5 | |
| `connect_time_sync_bat3_gold.pcapng` | 11-12 16:17 | `CASIO GMW-B5000` (gold) | 3459 | 01.01 | 3/5 | |
| `connect_time_sync_bat4_green.pcapng` | 11-12 16:20 | `CASIO GW-B5600#` (green) | 3540 | 01.01 | 4/5 | |

Firmware update details are in `firmware-update-research.md`.

## Observed values

| Watch | Date | App | Watch status battery | 0x28 |
|---|---|---|---|---|
| 3539 | 10-16 | 2/5 | byte 7 `11` | `28 0f 1d 00` |
| 3539 | 11-12 | 2/5 | byte 7 `11` | `28 0f 1d 00` |
| 3459 | 10-16 | 3/5 | byte 7 `12` | `28 0f 19 00` |
| 3459 | 11-12 | 3/5 | byte 7 `12` | `28 0f 1c 00` |
| 3540 | 10-16 | 4/5 | byte 7 `13` | `28 13 1a 01` |
| 3540 | 11-12 | 4/5 | byte 7 `13` | `28 13 1f 00` |
| BX | 10-17 | 5/5 | [158] `18` | `28 18 1b 00 00 00 02 00 00` |
| BX | 11-12 15:54 (H1) | 5/5 | [158] `18` | `28 18 1f 00 00 00 02 00 00` |
| BX | 11-12 16:04 (H2) | 5/5 | [158] empty | `28 17 1f 23 00 00 02 00 00` |

- Every B5000-family watch held its level for the month between captures, in both bytes.
- On the B5000 family, 0x28 byte 1 doesn't follow the bars (`0f` at 2/5 and 3/5) and is two
  below the watch status byte on the weaker watches. The gap was the same a month later, so it
  isn't a momentary sag under radio load. On the 3540 and the BX the two bytes agree.
- At H2 the BX watch status had just been wiped by the firmware update, so the app's 5/5 came
  from 0x28 byte 1 (`17`) or from a value cached before the update. A weaker point than H1.

## 0x28 watch condition

| Byte | Meaning |
|---|---|
| 1 | battery (see above) |
| 2 | temperature in °C, confirmed against the BX diagnostic menu twice (`1f` = 31 °C) |
| 3 | unknown. Usually `00`. 3540 `01` in October; BX `23` after the 01.02 update. Changes without a firmware update, so not a version field |
| 4–8 | BX only (9-byte reply, 4 bytes on older modules): `00 00 02 00 00` in every capture |

## Watch status transfer

| Handle | UUID | Properties | Use |
|---|---|---|---|
| 0x0011 | `26eb0023-…` | notify, write | session control (GShockAPI: DATA_REQUEST_SP) |
| 0x0014 | `26eb0024-…` | notify, write w/o response | data (GShockAPI: CONVOY) |

- The app enables notifications on 0x0014, writes `00 05 ab 00 00` to 0x0011, gets one
  notification on 0x0014, then writes `04 05 ab 00 00` to end the transfer. The same channel
  carries firmware updates (session `01`), see `firmware-update-research.md`.
- The BX request is `00 05 d8 00 00` → 216 bytes (older modules `ab` → 171 bytes). The size
  field is ignored: skuare sent `00 05 ab 00 00` and the BX still replied with 216 bytes
  (logcat 2026-10-18). The reply size is fixed per model.
- The app picks the size right after 0x26, before it reads 0x20, so it decides from the
  advertised name.

## Watch status layout, older modules (171 bytes)

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
| 160–171 | 12-entry list, `18` × 6 on 11-12 (battery?), empty `ff` |
| 178–189 | 12-entry list, `18 15 19 16 17 1b` on 11-12 (temperatures?), empty `7f` |
| 196–207 | 12-entry list, `03 00 01 00 00 06` on 11-12, empty `ff` |
| 214 | unknown (`3d`, `57`) |

Hourly slot: byte 0 battery (`18` in all 24), byte 1 temperature in °C (a 20–29 °C daily
curve on 11-12), byte 2 unknown (nonzero in a few slots, up to `08`), bytes 3–4 only set in
slot 0 (`28 02`). Whether slot 0 is the newest hour or 00:00 isn't known.

All lists were empty on 10-17, partly filled a day later, full on 11-12, and **wiped by the
01.02 firmware update**.

## Levels

The BX diagnostic menu shows a battery level, voltage and temperature. Photos of other MIP
watches' diagnostic screens online show **M1 at 2.40 V**, and the levels are probably **H1,
H2, M1, M2, L1, L2**.

Hypothesis, not verified: the raw byte is the level index, one step per level, and the app
merges H1 and H2 into 5/5:

| Level | Voltage | BX raw | Bars | B5000 raw (if 3 below) |
|---|---|---|---|---|
| H1 | 2.52 V | `18` ✓ | 5 ✓ | `15` |
| H2 | 2.435 V | `17` ✓ | 5 ✓ | `14` |
| M1 | 2.40 V (other watch) | `16` | 4 | `13` ✓ 4/5 |
| M2 | | `15` | 3 | `12` ✓ 3/5 |
| L1 | | `14` | 2 | `11` ✓ 2/5 |
| L2 | | `13` | 1 | `10` |

✓ = observed. This explains:

- the jump from 4/5 (`13`) to 5/5 (`17`): different scales per module family, not a wide top
  bar;
- why the byte holds for weeks: a level only changes at a threshold;
- why it isn't a voltage: H1 → H2 was one step for 0.085 V, but M1 is only 0.035 V below H2.

GShockAPI shifts the BX by 5 (below), which would make BX `17`/`18` 3–4 bars, so that shift
doesn't fit.

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

- The raw bytes aren't decivolts: at 0.1 V per step, 2/5–4/5 would be 1.5–1.9 V, below the
  cut-off, and the BX's `18` would be 2.4 V while the menu showed 2.52 V.
- From ~40% to ~95% used the cell stays within about 0.05 V of 2.3 V, so voltage-based levels
  can't be linear in capacity. The bars are thresholds, and GShockAPI's linear scale is wrong.

## What GShockAPI does

[izivkov/GShockAPI](https://github.com/izivkov/GShockAPI) 1.7.8, `api/.../WatchInfo.kt`:

- Battery from 0x28 byte 1, linear `(raw - lower) / (upper - lower)`.
- Limits 9..19 for the B5000 family, which can't tell 2/5 from 3/5 (both `0f`).
- Limits 14..24 for the GW-BX5600, which puts `18` at full.

## In progress (from 2026-11-12)

- Silver 3539 (2/5, `11`) kept in the dark to reach 1/5. Expected `10`.
- Green 3540 (4/5, `13`) worn and in light to reach 5/5. Expected `14`.
- GA-B2100, delivered flat, charging under a lamp to capture the lowest connectable level.
  It's a new module, so its scale may differ again.

The battery history in bytes 12–23 shows a change even if the capture is taken later. Note the
app's bars, any [LOW]/[CHG] indicator, and the diagnostic menu (where there is one) with each
capture.

## To do

- `BatteryLevel` per module family, with thresholds rather than exact values, once 1/5 and the
  B5000 5/5 are known.
- Watch status request size and parser per model (BX: 216 bytes, battery at [158]).

## Open questions

- B5000-family 1/5 and 5/5 raw values, and whether they have six levels too.
- BX levels below H2 (M1 should read `16` and show 4/5).
- Whether 1/5 is still connectable or already [LOW] (manual: [LOW] and [CHG] turn off phone
  connections).
- 0x28 byte 3, and BX bytes 4–8.
- Which end of the temperature log and the BX hourly slots is newest.
