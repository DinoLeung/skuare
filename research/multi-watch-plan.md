# Multiple connected watches plan

Several watches connected at once. The pairing screen shows which paired watches are connected,
and the watch screens open for the one the user picks, instead of opening as soon as a watch
connects. Needed before the GBD-H2000, which is expected to keep its connection open.

Nothing here is implemented. Branch 2 was written once (2026-11-16, compiled and passed the unit
tests, never run on a device) and discarded, so the peripheral id column could go in first. Its
design is what branch 2 below describes.

## What blocks it today

- `WatchConnectionManager` wraps `SingleConnectionSlot`: a connect from the app disconnects the
  current watch, and a background connect returns `Busy` if any watch is connected.
- `KoinApp.kt` has `factory { requireWatch() }`, the five screen view models and
  `NavBarViewModel` inject that one "current watch".
- `PairingScreenViewModel.connect()` calls `onConnect()` right after the session, and so does
  its collector of `pendingForegroundWatch` (a watch held after a lower-left long press while
  the app was closed).
- `WatchSyncService` has one `session` job, `AndroidWatchConnectionNotifier` one
  `connectionActive` flag and one notification.
- `RootNavGraph` pops the pairing screen when the watch screens open, so back exits the app.
- Common code assumes the peripheral identifier is the watch's BLE address:
  `resolveInitialNotificationName` looks the paired watch up by it, and
  `runBackground(peripheral, address)` needs the address before connecting.

`Watch` has no shared state, several instances can run side by side.

## Decisions

- Each connected watch gets its own ongoing notification, tapping it opens that watch's
  screens. Opening the app from the launcher shows the list. This replaces the automatic
  opening after a lower-left long press while the app was closed.
- A connected watch nobody opens stays connected until it times out by itself, no timeout of
  our own.
- `PairedWatch` gets a `peripheralId` column, see below.
- Connecting from the app no longer opens the watch screens, the user taps the connected card.

## iOS compatibility

iOS never exposes the MAC address and can't reach a peripheral by one. A peripheral has a UUID
that iOS assigns per phone, and reconnecting or waking the app for a watch is done with that
UUID. The address is only known after connecting, from the watch's 0x10 reply. See
`ios-background-sync.md`.

Rules for every branch:

- `PairedWatch.address` stays the primary key. Anything about a connected, paired watch keys on
  it: the list, forget, sync records, the connected status.
- A connection is identified by the peripheral identifier (`Watch.id`: the address on Android,
  the UUID on iOS), known before connecting on both.
- `PairedWatch.peripheralId` stores that identifier when pairing, so a paired watch can be found
  before connecting, and so `WatchPresenceMonitor.register` has what iOS needs to start a
  pending connect.
- `WatchConnectionNotifier` calls take the `Watch.id` as an opaque key, never the address.
- A notification tap goes into a small common class holding "open this watch id" requests,
  collected by the common nav graph. Android's activity feeds it, an iOS notification delegate
  can later.
- The one-session-per-watch guard lives in the common registry and session handler, not in
  `WatchSyncService`.
- No new `expect` declarations, `rememberWatchPicker` stays the only one.

## To do

The list and its navigation come before several connections: they work with one watch, and
several connections need them (a status to know when a watch can be opened, a way back to the
list, more than one held watch).

Branch 1, `feat/peripheral-id-column`:

- [ ] `PairedWatch.peripheralId: String?`, database v5 as a Room auto-migration like v2–v4
      (never destructive, it would wipe the install token). Existing rows start with null and
      are filled on the next connect
- [ ] `PairedWatchDao` / `PairedWatchRepository.findByPeripheralId` matches `peripheralId` or
      `address` in one query, so a row from before the migration is still found before
      connecting on Android, where the two are the same. Without it the first background sync
      after the update loses the stored advertised name and custom model. A UUID never equals
      an address, so it is safe on iOS
- [ ] `Watch.id` and `ConnectedWatch.id` from the peripheral identifier, `pair` stores it,
      a background session fills it in when missing
- [ ] `resolveInitialNotificationName` and `runBackground` look the stored watch up by
      peripheral id, `runBackground` drops its `address` parameter
- [ ] Tests: migration, repository (a row with and without `peripheralId`), session handler

Branch 2, `refactor/connection-registry` (no behaviour change):

- [ ] `ConnectionRegistry` replaces `SingleConnectionSlot`: one connection per id, connecting
      serialized per id so different watches can connect at the same time, released when the
      disconnection is observed. `connect(id)` replaces only the same id,
      `connectIfAbsent(id)`, `disconnect(id)`, `values` as a `StateFlow` of a map. Tests
      replace `SingleConnectionSlotTest`
- [ ] `WatchConnectionManager`: `watches`, `find(id)`, `findByAddress(address)`,
      `disconnect(id)` instead of `watch` and `requireWatch()`. It still connects one watch at
      a time (a mutex around both connects, the app's connect releases the others, the
      background one returns null if any watch is connected) until branch 4
- [ ] `WatchSessionHandler.forget` disconnects the watch by address
- [ ] Watch screens open for a watch id: `RootNavGraph` keeps the id like `watchStartRoute`,
      `NavBarScaffold` finds the watch and goes back to the pairing screen if it is gone
- [ ] The six view models take their `Watch` as a constructor parameter, passed with
      `parametersOf(watch)`. Remove the Koin `factory { requireWatch() }`
- [ ] A test that builds the view models through Koin's module verification, so a wiring
      mistake fails in CI rather than on a device. Adds `koin-test`
- [ ] `pendingForegroundWatch` and the automatic opening stay as they are
- [ ] Device check (user): connect, save, disconnect, background sync, lower-left long press
      with the app closed

Branch 3, `feat/select-connected-watch` (still one watch at a time):

- [ ] Connection status per watch: `Connecting`, `Syncing` (a session is running), `Ready`
- [ ] `PairedWatchCard` shows the status, matched by address. A `Ready` card opens the watch
      screens, a `Syncing` one doesn't, the background session disconnects it when done.
      Disconnect action on connected cards
- [ ] Remove both automatic openings and `pendingForegroundWatch`, a held watch is a `Ready`
      one. `WatchSyncService` waits for that watch to disconnect instead
- [ ] The pairing screen stays on the back stack. Back from the watch screens returns to the
      list and keeps the connection, with a confirmation if there are unsaved changes
- [ ] Until branch 4: the one notification opens the app on the list, also after a lower-left
      long press with the app closed, and connecting another watch from the list still
      disconnects the connected one
- [ ] Device check (user): connect and stay on the list, open, back and open again, unsaved
      changes confirmation, disconnect from the card, forget a connected watch, lower-left
      long press with the app closed

Branch 4, `feat/multi-watch-connections`:

- [ ] Lift the one-at-a-time rule: the app's connect leaves other watches alone, background
      sync is `Busy` only when that same watch is connected
- [ ] `WatchConnectionNotifier` calls take the watch id
- [ ] Android: one job per watch in `WatchSyncService`, stopped only when no watch is
      connected. One ongoing notification per connected watch, the service's own notification
      moves to another watch when its watch disconnects
- [ ] Each notification has its own `PendingIntent` request code and carries its watch id,
      otherwise Android reuses one intent and every notification opens the same watch
- [ ] Common open requests: hold the last "open this watch id" until `RootNavGraph` collects
      it, consumed once. `MainActivity` feeds it from `onNewIntent` (`singleTop`) and from
      `onCreate`, the cold start after a lower-left long press with the app closed, but not
      again when the activity is recreated
- [ ] An open request only opens a `Ready` watch, a `Syncing` one opens once it is `Ready`, a
      watch that is gone shows the list
- [ ] Opening another watch while the open one has unsaved changes asks the same confirmation
      as back
- [ ] README: tick "Handle multiple paired watches are connect"
- [ ] Device check (user): two watches connected, both notifications and tapping each (app
      open, in the background, and closed), opening each from the list in turn, a background
      sync of one while the other is open, switching with unsaved changes

## Left for the GBD-H2000 work

- A per-model "keep connected" flag, so a background session doesn't disconnect after syncing.
- Reconnecting after a drop. The Companion Device Manager only reports a watch that is
  advertising, an always-connected watch needs its own reconnect path.
