# Firmware update research

Official Casio app updating a GW-BX5600 (module 3578) from firmware 01.01 to 01.02, captured
2026-11-12. `pairing_bx.pcapng` is the connect right before the update, `update_01_02_bx.pcapng`
is the update. 42 s in total, about 8 s of it the transfer itself.

## Firmware version is in 0x20

0x20 (version information, commented out in `OpCode`) is
`20 23 03 01 01 00 00 00 00 00 00 00 00 03 17 00 00 00 00 00` before and
`20 23 03 01 02 …` after. **Bytes 3–4 are the firmware version, major.minor** (`01 01` = 01.01).
The rest is unchanged.

| Watch | 0x20 reply | Firmware |
|---|---|---|
| 3459 | `20 23 07 01 01 00 … 00 00 …` | 01.01 |
| 3539 | `20 23 07 01 01 00 … 01 01 …` | 01.01 (still on 2026-11-12) |
| 3540 | `20 23 07 01 01 00 … 01 01 …` | 01.01 |
| 3578 (BX) | `20 23 03 01 01 00 … 03 17 …` | 01.01 → 01.02 |

Byte 2 is `07` on the B5000/B5600 modules and `03` on the BX (module family?). Bytes 13–14 differ
per module and didn't change with the update. Before sending the image the app writes 0x20 back with the new version
(`… 01 02 …`), so the watch knows the version it's about to receive.

## Transfer channel

Everything goes over the watch status pair, not the 0x000c/0x000e pair:

| Handle | Use here |
|---|---|
| 0x0011 | session control: app writes 5 bytes, watch notifies 7 bytes |
| 0x0014 | data: app writes without response, watch notifies |

0x0011 commands are `<cmd> <session> 00 00 00`. Sessions seen: `05` watch status (known),
`01` firmware, `0c` settings backup, `0d` settings restore. Commands seen: `00` start, `04` end,
`07` window ack, `09` transfer complete, `03` cancel.

In the pre-update connect (`pairing_bx`) the app opened session `01`, got `00 00 00` on 0x0014,
then cancelled with `03 00` on 0x0014 and `03 01 00 00 00` on 0x0011. That looks like an
update check that the user hadn't confirmed yet.

## Sequence

| # | Who | Where | Bytes | Meaning (guess where marked ?) |
|---|---|---|---|---|
| 1 | app | 0x0011 | `00 01 00 00 00` | start firmware session |
| 2 | watch | 0x0014 | `00 00 00` | ready |
| 3 | app | 0x000e | `20 23 03 01 02 …` | announce new version |
| 4 | watch | 0x0014 | `06 ff 00 00 20 00 00 00 00 00 00 ff 00 01` | ? capabilities / target info |
| 5 | app | 0x0014 | `01 00 60 00 00 00 60 00 00 fa f5 00` | image header: size 0x6000 twice (u32 LE), `f5fa` ? |
| 6 | watch | 0x0014 | `04 00 4e 00 4e 00 04 00 58 02` | ? current conn params: interval 78 (97.5 ms) ×2, latency 4, timeout 600 (6 s) |
| 7 | app | 0x0014 | `04 01 18 00 18 00 00 00 58 02` | ? asks for 24 (30 ms) ×2, latency 0, timeout 6 s |
| 8 | watch | 0x0011 | `00 01 00 00 00 00 00` | session open, send data |
| 9 | app | 0x0014 | `05 …` × 192 | 96 blocks, see below |
| 10 | watch | 0x0011 | `07 01 …`, app echoes `07 01 00 00 00` | window ack, 3× (after blocks 32, 64, 96) |
| 11 | watch | 0x0011 | `09 01 …`, app echoes | transfer complete / verified? |
| 12 | watch | 0x0011 | `04 01 …` | firmware session ending |
| 13 | app | 0x0011 | `00 0c` … `04 0c` | backup: reads 1d×2, 1e×3, 1f×6, 24×3, 13, 15, 16, 18 on 0x000c |
| 14 | — | — | — | watch drops the link, reconnects ~5 s later, GATT rediscovery |
| 15 | app | — | reads 0x22, 0x10 | 0x10 byte 8 (reason) = **`05`**, never seen before |
| 16 | app | 0x0011 | `00 0d` … `04 0d` | restore: writes everything from step 13 back on 0x000e |
| 17 | app | 0x0011 | `04 01 00 00 00` | closes the firmware session |
| 18 | app | — | normal connect sync | 0x10 reason back to `01`, 0x20 = 01.02 |

Step 6–7 look like a connection parameter negotiation done in-band (intervals in 1.25 ms units,
timeout in 10 ms units). Not verified against the HCI connection update events.

## Data blocks

Each block is 256 bytes plus a CRC, sent as two write commands on 0x0014:

- `05` + 251 bytes
- `05` + 5 bytes + CRC-16 (2 bytes, little-endian)

**The CRC is CRC-16/KERMIT** (poly 0x1021 reflected, init 0, no final xor) over the 256 bytes.
Checked on all three non-empty blocks (`df98`, `a41f`, `2cd2`), and all-zero blocks give `0000`,
which they do in the capture.

The app waits for the `07` ack after every 32 blocks (8 KB).

## Image

96 blocks = 24576 bytes = 0x6000, matching the header size. Reassembled from the capture:

- 0x0000–0x01ef: ~490 bytes of data, looks like 16-bit code (repeating `ff0b a55e` at the
  start, maybe a jump table)
- the rest zero up to the footer
- 0x5ff4: footer `5a 5a a5 a5 a5 5a 5a a5 fd ff ff ff` (magic + `0xfffffffd`)

So the 01.01 → 01.02 update is a ~0.5 KB patch in a 24 KB region, not a full firmware image.
Real zeros, not snoop filtering: the zero blocks carry CRC `0000` and the CRC tail packets are
intact.

The header's `fa f5` isn't a CRC-16 (XMODEM, CCITT-FALSE, ARC, MODBUS, KERMIT) of the whole
image, nor a byte or word sum. Unknown.

## After the update

- 0x11 (connection settings) read `11 0a 0a 04 06 …`, reset by the update; the app wrote back
  `11 0f 0f 0f 06 …` (the pre-update value).
- Watch status log wiped: all 24 slots back to `ff 7f ff 00 00 00`.
- 0x28 `28 17 19 23 00 00 02 00 00`: battery `17` (was `18`), temperature 25 °C, byte 3 `23`
  (was `00`, meaning unknown).
- Everything else (clocks, timezones, alarms, timer, settings) restored by the app in step 16.

## Open questions

- Header `fa f5 00` and the watch's `06 …` reply.
- Whether `09` means "CRC of the whole image OK".
- Whether skuare should do the backup/restore itself after a firmware update it didn't run,
  i.e. detect connect reason `05`.
- Where the app gets the image (Casio server, HTTPS, not captured).
