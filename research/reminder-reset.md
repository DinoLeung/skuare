# Reminder reset (parked idea)

Requested 2026-11-14, parked before implementation. Not part of the CASIOAK work.

## Request

A way to reset each reminder to its factory values, through a reset button or a context menu
opened by pressing and holding the card. A confirmation dialog must come before the reset.

## Factory values

A reminder that was never set reads back as all zeros (GA-B2100 captures `reminder_2100.pcapng`
and `reminders_2100.pcapng`, before anything was saved):

```
31 0n 00 000000 000000 00 00     config: disabled, once, no dates, no days
30 0n 00 × 18                    title: empty
```

What skuare would write for an "empty" reminder isn't quite the same:

- **Title:** `ReminderTitle("")` encodes to 18 zero bytes, the same as the factory value.
- **Config:** `ReminderConfig.fromBytes` reads the `000000` dates as `DEFAULT_DATE`
  (2000-01-01), and `bytes` writes that back as `000101`, not `000000`. So a reset through the
  normal model gives `31 0n 00 000101 000101 00 00`. Writing the true factory value needs zero
  packets built directly, or `DEFAULT_DATE` encoded as `000000`. Whether the watch treats the
  two differently is unknown.

## Existing code to build on

- `ui/component/ClockCard.kt`: press and hold opens a `DropdownMenu` (`CardView.onLongClick`).
- `ui/component/PairedWatchCard.kt`: "Forget watch" button with an `AlertDialog` confirmation.
- `ui/component/ReminderCard.kt`: no header row. Its content is almost all text fields and
  dropdowns, which take the touch, so press and hold would only work on the card's edges.
- `ui/screen/reminders/RemindersScreenViewModel.kt`: edits stay pending until Save. Every
  emission of `watch.reminders` overwrites the pending titles and configs of **all** reminders
  with what the watch sent.

## Open decisions

1. **When the reset reaches the watch:**
   - *Pending until Save* (recommended): confirming fills that card with factory values as
     unsaved edits, and Save writes them like any other edit. Other cards' edits are safe.
     Writes `000101` dates unless the config encoding changes.
   - *Immediately:* confirming writes the zero packets for that reminder only, then reads it
     back. The view model must first stop a read-back from overwriting other reminders' pending
     edits, or they're lost.
2. **Entry point:**
   - A header row with "Reminder N" and a ⋮ button opening a menu with "Reset", plus press and
     hold opening the same menu like `ClockCard` (recommended).
   - Press and hold only (works only on the card's edges).
   - A visible "Reset" text button, like "Forget watch".
3. **Factory value:** true zeros (`000000` dates), or skuare's empty reminder (`000101`).

Confirmation dialog either way, e.g. "Reset reminder 3? Its title, dates and repeat go back to
the factory values." with Reset / Cancel.
