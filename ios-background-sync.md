# iOS background time adjustment

Reference for adding background time adjustment on iOS. Android is done (Companion Device
Manager), this is what's left for iOS. Written 2026-10-17, nothing here is implemented yet.

## Current iOS state

- `PlatformModule.ios.kt` binds `UnsupportedWatchPresenceMonitor`, so background sync never
  runs (`isSupported = false`).
- `rememberWatchPicker` returns null on iOS, the pairing screen's + FAB searches with the Kable
  scanner and connects to the first watch that advertises (no choice between several watches).
- The pairing screen only asks for notification permission where `WatchPresenceMonitor.isSupported`,
  so iOS doesn't ask yet.
- Koin starts in `MainViewController` (`initKoin()` in `ComposeUIViewController`'s `configure`).
- The Room database lives in the documents directory (`SkuareDatabase.ios.kt`).

## Shared code iOS can reuse

- `WatchSessionHandler`:
  - `runForeground(watch)`: pairing (writes our APP_INFO token, stores the watch, registers it
    with the presence monitor), watch status, time adjustment.
  - `runBackground(peripheral)`: connect if idle, check the token and the database row, then by
    connect reason: auto/manual sync adjusts the time and disconnects, lower-left long press
    adjusts the time and holds the watch in `pendingForegroundWatch`, find phone is ignored.
  - `forget(address)`.
- `WatchConnectionManager` (`connectIfIdle` for background sessions), `AppInfo`, Room database
  and repositories.
- The pairing screen already takes `pendingForegroundWatch` and opens the watch screens.

iOS needs its own `WatchPresenceMonitor` and background trigger.

## Approach

- `bluetooth-central` in `UIBackgroundModes`.
- A Bluetooth central with a restore identifier, so iOS can relaunch the app for Bluetooth
  events after it was terminated.
- A pending connect to each paired watch. CoreBluetooth connect requests don't time out, iOS
  connects when the watch advertises and wakes the app.
- On connect, run `WatchSessionHandler.runBackground` with that peripheral.
- `OpenAppRequested` (lower-left long press): keep the watch in `pendingForegroundWatch`; if the
  user opens the app themselves, the pairing screen takes the waiting watch.
- `WatchPresenceMonitor.register` / `unregister` start and cancel the pending connect.
- Once `isSupported` is true, the pairing screen also asks for notification permission.

## Things to sort out

- Kable 0.45 has a `stateRestoration` flag, but its `willRestoreState` callback does nothing
  (`CentralManagerDelegate.kt:118`). Restoring the connection after iOS relaunches the app may
  need our own `CBCentralManager` or a Kable patch. Spike this first.
- iOS doesn't expose the watch's MAC address. Its peripheral identifier is a UUID that iOS
  assigns per phone. `PairedWatch.address` is the address the watch reports in 0x10 (bytes
  1–6), which works for checking the token and finding the row, but reconnecting on iOS needs
  the CoreBluetooth identifier. `platformAssociationId` is an `Int`, so storing a UUID needs a
  new column, i.e. a new schema version with a migration (never a destructive one, it would
  wipe the install token and unpair every watch).
- Whether a pending connect survives the user force quitting the app is up to iOS, test it.
