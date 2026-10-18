# Plan: time sync on the GW-BX5600

Goal: connect sync (foreground and background) sets the time on the GW-BX5600 the same way it
does on the B5000 / B5600 today, without breaking those. Background is in
`bx5600-research.md`, read it first.

Not in this plan: watch status / battery, the 15-byte timer, display font, alarm byte 1.

## Phase 1: model profile on the `Watch` object

The `Watch` takes the advertised name when it is created, and decides what the watch has from it:

- **Classic route** (B5000 / B5600): 6 clocks, 5 reminders, same as skuare today.
- **3-clock route** (BX5600 / BZ5000): 3 clocks, 0 reminders.

### `WatchModel`

New `lib/model/WatchModel.kt`:

```kotlin
enum class WatchModel(val clockCount: Int, val reminderCount: Int) {
	CLASSIC_SQUARE(clockCount = 6, reminderCount = 5),
	MIP_SQUARE(clockCount = 3, reminderCount = 0);

	companion object {
		/**
		 * Exact match on the name without the leading "CASIO " and the trailing #.
		 * Null for unknown or null names, `Watch` falls back to [CLASSIC_SQUARE] and logs a warning.
		 */
		fun fromAdvertisedName(name: String?): WatchModel?
	}
}
```

| Matched name | Advertised as | Module | Model | Source |
|---|---|---|---|---|
| `GMW-B5000` | `CASIO GMW-B5000`, `CASIO GMW-B5000#` | 3459, 3539 | `CLASSIC_SQUARE` | capture |
| `GW-B5600` | `CASIO GW-B5600#` | 3461, 3540 | `CLASSIC_SQUARE` | capture |
| `GW-BX5600` | `CASIO GW-BX5600` | 3578 | `MIP_SQUARE` | capture, logcat |
| `GMW-BZ5000` | ? | 3575 | `MIP_SQUARE` | GShockAPI name, no capture |

- The trailing `#` only marks the newer B5000 / B5600 modules, which use the same protocol as
  the older ones, so it is dropped before matching. Keep the full name in `advertisedName` and
  `PairedWatch` for display and logs.
- Unknown names log a warning with the name, so new models show up in the logs.
- **BZ5000 needs a capture.** GShockAPI matches the same name but puts it on its standard
  protocol with the B5000 battery range (9..19), not the BX's MIP protocol. So it may
  have 3 clocks without the batched characteristics. Phase 2 and 3 only use single requests on
  0x000c, so they should work either way. Phase 4 must not assume the BZ has 0x0017 / 0x0019.
- GShockAPI also lists `GW-B5000`, `GCW-B5000`, `MRG-B5000`, `DW-B5600`. Not
  added, there's no capture, and the fallback already treats them as `CLASSIC_SQUARE`.
- Later BX differences (watch status size, timer size, font) become fields here, not
  `if (model == …)` checks.

### `Watch`

```kotlin
class Watch(
	private val peripheral: Peripheral,
	val advertisedName: String? = peripheral.name,
) : KoinComponent {
	val model = WatchModel.fromAdvertisedName(advertisedName)
	internal val _clocks = MutableStateFlow(ClocksSettings.empty(model.clockCount))
	internal val _reminders = MutableStateFlow(RemindersSettings.empty(model.reminderCount))
	…
}
```

- Replaces the current `advertisedName` getter, so `WatchSessionHandler.pair` and
  `WatchSyncService` keep working.
- Log `model` and the name on connect.
- `WatchConnectionManager.connect` / `connectIfIdle` get an optional `advertisedName` that is
  passed through.
- Background sync makes the peripheral from an address, so `peripheral.name` can be null.
  `WatchSessionHandler.runBackground` falls back to `PairedWatch.advertisedName`.

### Clocks sized by the model

| Where | Today | Change |
|---|---|---|
| `ClocksSettings.clocks` | `List(5) { null }` | `empty(clockCount)` = `List(clockCount - 1) { null }`, keep the default for previews / tests |
| `ClocksSettings.isInitialized` | `size == 6` | `pinnedClock != null && clocks.all { it != null }` |
| `ClocksSettings.setClock` | position must be in `1..5` | ignore positions `>= clockCount` (the BX `1d02` record holds an unused slot 3 = `0000`) |
| `ClocksSettings.clocksPackets` | 3 fixed pairs | `allClocks.chunked(2)`, an odd count gets a filler slot: DST `00`, id `ffff` (as in the capture) |
| `Watch.requestClocks` | `repeat(3)` | `repeat((clockCount + 1) / 2)`, i.e. 2 on the BX, a 3rd request errors |
| `requestTimeZoneConfigs` / `Names` / `CoordinatesAndRadioId` | `0..5` | `0 until clockCount` |
| `ClocksUiState.pendingClockErrors` | `List(5) { null }` | sized from `watch.model.clockCount - 1` |

The clocks screen already loops over the list, so a BX shows the pinned card and 2 others.

### Reminders sized by the model

- `RemindersSettings.empty(count)` and the `1..5` checks become `1..reminderCount`.
  `requestReminderTitles` / `Configs` loop over `1..reminderCount`, so nothing is sent at 0.
- The reminders tab only shows when `reminderCount > 0`. `navBarItems` and `saveableScreens` in
  `NavBarScaffold` are filtered by the connected watch's model.

### Tests

- `WatchModelTest`: every name in the table, with and without `#` (same model either way),
  null, unknown.
- `ClocksSettingsTest`: `empty(3)` gets initialized by the two logcat records
  (`1D000107067F767A00FF…`, `1D0203020019010000FF…`). Position 3 is ignored. `clocksPackets`
  for 3 clocks gives 2 packets, the second the same as the captured
  `1D 02 03 02 00 19 01 FF FF FF FF FF FF FF FF`. The 6-clock packets don't change.

After phase 1 a BX gets an initialized clocks state on connect. Clock writes stay off until
phase 4 (see phase 3).

## Phase 2: reading clocks reliably

From the logcat (2026-10-18): the first two single `1d` requests are answered, and after that every
`1d` gets `FF 81 1D`. Likely a session left open.

1. Route `FF xx 1D` errors to `_clocksError`, so `adjustTime` and the clocks screen fail fast
   and don't wait 10 s. Parse it as `error, ?, failed opcode`.
2. Test on one connection which message lets a second read work: `21 01 01`, the time write
   `09`, or a write-back of the records just read. Record the result in the research file.
3. Send that message after the clock reads on the 3-clock route, wherever the code ends the read.

If no message helps, reading moves to the batched characteristics on the BX (phase 4).

## Phase 3: time-only sync on the 3-clock route

`adjustTime` today: read clocks, write clocks, configs, names (and coordinates), then the time
`09`. Writing clocks back to a BX isn't safe yet (flag `0x04`, short names, see phase 4), so
on `MIP_SQUARE`:

1. Read clocks (phases 1–2).
2. Write only the time packet `09` for the pinned clock, the same `getCurrentDateTimePacket` as
   on the classic route.
3. Skip the clock, config, name and coordinate writes, and log that they were skipped.
4. Clocks screen: the clocks are read-only on `MIP_SQUARE` until phase 4 (`Watch.canWriteClocks`), so
   nothing can be edited and `writeClocksTimezones` is never called on a BX. It still refuses
   MIP_SQUARE as a safety net.

Check on the watch:

- The time is right after a foreground connect and after a background sync (button press and
  auto sync).
- Whether `09` on its own is accepted. The official app always writes the clocks back first, but
  `09` goes to 0x000e like on the B5000. If the watch refuses it, phase 4 comes first.
- Whether `09` also closes the session from phase 2.

Classic route: no change, the B5000 / B5600 keep the full write-back.

## Phase 4: full write-back, like the official app

Needed so the 3-clock route gets the DST / time zone data refreshed on every sync, and for
editing clocks on the BX.

1. **Clock flag `0x04`**: `DstSettings` keeps the raw byte and writes unknown bits back, so `07`
   / `06` aren't turned into `03` / `02`.
2. **Short names**: the BX stores a 3-letter name at index + 6 (`1f06`–`1f08`). The city CSVs
   in `data/` have none, so they need a source first (GShockAPI, the official app's database,
   or reading what the watch holds).
3. **Transport**: try the single writes on 0x000e first, the same as the classic route.
   If the BX refuses them, add the batched path on 0x0017 / 0x0019 (only for watches that have
   those characteristics, not assumed for the BZ5000).
4. Remove the temporary `Watch.canWriteClocks`, `checkClockWritesSupported` and
   `ClocksUiState.isEditable` with every check of them, so `MIP_SQUARE` writes clocks and the
   clocks screen is editable again. It was only a stopgap, not a model capability.

## Phase 5: device checks

| Watch | Check |
|---|---|
| GMW-B5000 (3459, 3539), GW-B5600# | logged as `CLASSIC_SQUARE`, sync and clocks screen unchanged, reminders tab shown |
| GW-BX5600 | logged as `MIP_SQUARE` in foreground and background, time right, 3 clocks shown, no reminders tab |
| GMW-BZ5000 | when there is one: capture first, then the same checks as the BX |
