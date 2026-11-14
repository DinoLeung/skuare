# Hand adjustment research

Official Casio app running hand adjustment (home position correction) on a GA-B2100, captured
2026-11-13. Two captures, one per hand group, each with the same moves: 3 steps clockwise,
then 2 steps anticlockwise, then 1 step anticlockwise.

- `hand_adjust_clock_2100.pcapng`: hour and minute hands
- `hand_adjust_function_2100.pcapng`: mode hand (called the function hand at the time)
- `hand_adjust_function_opposit_2100.pcapng`: mode hand moved from the adjusting position (3 o'clock) to 9 o'clock, 2026-11-14

## Opcode 0x1A on the 0x000c/0x000e pair

Everything is opcode **0x1A**, written to the IO characteristic (0x000e, write request). The
batched characteristics and the 0x21 session aren't used. Each write is 6 bytes:

```
1a 04 <cmd> <steps LE16> 00
```

Byte 1 is always `04`, probably the payload length (4 bytes follow). Byte 5 was always `00`, so
the step count could be LE24, but 16 bits is enough for the values seen.

The watch's only reply is the notification `1a 04 00 00 00 00`, which comes when it has finished
the command (for moves, when the hands stop). Some commands get no notification.

### Commands

`<cmd>` looks like hand group in the high nibble and action in the low nibble:

| cmd | Clock | Mode | Notify | Meaning (inferred) |
|---|---|---|---|---|
| `06` | | | yes | sent when the selection screen opens, and again on exit between `04` and `07` |
| `x2` | `12` | `22` | no | select hand group |
| `x8` steps 0 | `18` | `28` | yes, when the hands arrive | move the hands to the adjusting position |
| `x8` steps n | `18` | `28` | yes, when the hands stop | move the hands n steps |
| `x9` | `19` | `29` | no | sent right after each move's notification: confirm the move? |
| `04` | | | no | first write on exit: save? |
| `07` | | | no | last write on exit: leave adjust mode |

The hands go back to the normal position on exit, but the watch sends nothing when they get
there (see below).

Group `1` = clock hands (moved by the minute hand), `2` = mode hand. Other models probably have `3`, etc.
for more hands.

### Step encoding

Steps are **relative** to where the hands are and wrap at a fixed total per hand group, so
anticlockwise moves are sent as `total - n`:

| Move | Clock | Mode |
|---|---|---|
| +3 | `03 00` (3) | `03 00` (3) |
| −2 | `6e 08` (2158) | `b2 00` (178) |
| −1 | `6f 08` (2159) | `b3 00` (179) |

- Clock hands: the official app's adjustment only moves the minute hand, and it takes
  **15 steps from 12 to 1** (user, 2026-11-14). So 1 step is 2° of the minute hand, 20 seconds
  of time, and a full turn of the minute hand is 180 steps. The wrap at 2160 is 12 × 180:
  12 hours, one full turn of the hour hand, so `2160 − n` lands on the same spot as −n.
  (An earlier note had 1 step = 1 minute, which was wrong.)
- Mode hand: wraps at 180, a full turn, so 1 step is 2° like the minute hand. Its dial is a
  half circle, but it can be adjusted all the way round (user). Moving it from the adjusting
  position (3 o'clock) to 9 o'clock took one move of 91 steps (`1a 04 28 5b 00 00`,
  `hand_adjust_function_opposit_2100.pcapng`). The user was likely 1 step past 9 o'clock, so a
  half turn is 90 steps. (An earlier note had 1° per step, which was wrong.)

They're relative, not absolute: as absolute positions the user's moves would have been 3, then
−2 (2158), then −3 (2157), but the last write was 2159.

The watch doesn't spin a full revolution for a "2158 steps" move: the notification came 1.2 s
later, while the move to the adjusting position took 10.7 s. So it treats large values as
anticlockwise (or takes the shorter way).

## Sequence

Clock capture, with the official app's screens as the user saw them (times in seconds; the
mode hand capture is the same with `2x` commands):

| Screen / action | t | App writes | Watch |
|---|---|---|---|
| Open the selection screen (traffic before the choice appears) | 2.17 | `1a 04 06 00 00 00` | notify `1a 04 00…` at 2.95 |
| Select clock hands | 6.14 | `1a 04 12 00 00 00` | |
| Hands move to the adjusting position | 6.94 | `1a 04 18 00 00 00` | notify at 17.66, when they arrive (10.7 s) |
| Adjust +3 | 21.74 | `1a 04 18 03 00 00` | notify at 23.62 |
| | 23.64 | `1a 04 19 00 00 00` | |
| Adjust −2 | 27.61 | `1a 04 18 6e 08 00` | notify at 28.79 |
| | 28.80 | `1a 04 19 00 00 00` | |
| Adjust −1 | 31.55 | `1a 04 18 6f 08 00` | notify at 32.37 |
| | 32.39 | `1a 04 19 00 00 00` | |
| Exit adjust mode | 42.71 | `1a 04 04 00 00 00` | |
| | 43.51 | `1a 04 06 00 00 00` | notify at 44.30 |
| | 47.81 | `1a 04 07 00 00 00` | write response at 49.86, then nothing |
| Hands move back to the normal position | | | nothing |

The mode hand reached the adjusting position in 1.2 s, against 10.7 s for the hour and
minute hands.

**No packet when the hands return to normal.** The clock capture runs to 68.3 s, 18 s after
`07`'s write response, with no ATT traffic from the watch and no disconnect. That's longer than
the 10.7 s the hands took to reach the adjusting position, so they must have finished returning
inside the capture. The mode hand capture ends 3.5 s after `07`, so it can't tell us anything here.
Which of `04`/`06`/`07` starts the return is unknown: the exit writes are spread over 5 s, and
the `06` notification came within the usual write-response delay, so it doesn't seem to wait
for the hands.

Write responses took about 0.78 s throughout (slow connection interval), so notification times
under about 1 s don't tell us much.

The app also reads 0x11 (connection settings) through the request characteristic (`11` on
0x000c) once on the selection screen, and again whenever a command finishes, before the next tap.
The reply never changed (`11 0f 0f 0f 06 00 00 00 00 00 01 00 00 26 03`, the same as the BX
before its update). It's probably the official app refreshing its screen state, not part of
the protocol.

### Mode hand to 9 o'clock

`hand_adjust_function_opposit_2100.pcapng` follows the same sequence (`06`, `22`, `28` 0,
one move, `29`, then `04`, `06`, `07`), with two differences:

- **One move of 91 steps** (`1a 04 28 5b 00 00`, likely 1 step past 9 o'clock) at 58.1 s,
  35 s after the previous `0x11` read, then `29`. The watch replied 2.0 s after the write
  response. How the official app
  collected 91 steps into one write (many taps, a held button, typed number) isn't known.
- **`07` was sent twice**, at 73.2 s and again at 75.6 s right after the first one's write
  response (which took 2.4 s). The clock capture sent it once. Maybe a retry, or a second tap.

## Tested with skuare (2026-11-14)

Hand adjustment from skuare works on the GA-B2100 (user's device test). skuare sends the official
app's sequence, but **without the `0x11` reads** in between, so those are only the official app
refreshing its screen:

```
06 (wait for reply) → x2 → x8 0 (wait for reply) → [x8 n (wait for reply) → x9]… → 04 → 06 (wait for reply) → 07
```

Experiments 1–3 of the plan (is `04` needed, is `x9` needed after every move, is `x2` needed)
weren't run: they need a build that leaves out a command, and the full sequence works.

## Hand groups come from the model

The watch never says which hand groups it has or what their step moduli are, so the official app
must know them from the model (it picks the watch status size from the advertised name the
same way, see `battery-research.md`).

- During hand adjustment the watch only sends the `0x11` reply (identical every time) and
  `1a 04 00 00 00 00`, the same for both groups.
- On connect, `0x20` can't tell analog from digital: bytes 13–14 are `03 17` on both the
  GA-B2100 and the GW-BX5600, and bytes 3–4 are the firmware version (`03 01`).
- `0x26` differs between watches of the same module family (silver 3539 and green 3540 share
  only the first 6 bytes), so it looks like a per-watch ID rather than a feature list. It isn't
  decoded.

A second analog model whose `0x20` or `0x26` changes in line with its hands would be the only
thing to show otherwise.

## Open questions

- What `04`, `06` and `07` really are, and which one sends the hands back to normal. Is `04`
  needed to keep the new adjusting position, or does `07` alone save it? What happens if the
  app disconnects mid-adjustment?
- Why `06` is sent both when the selection screen opens and on exit.
- Whether `x9` is needed after every move, or only before switching group / finishing.
- Whether `x2` (select) is needed before `x8`, and whether both groups can be adjusted in one
  session (each capture adjusted only one group).
- Hand groups and step moduli on other analog models (G-Squad, other GA-B2100 variants).
  The watch never sends them (see "Hand groups come from the model"), so they come from the
  model.
- Whether byte 5 is part of the step count.
