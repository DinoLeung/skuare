package xyz.d1n0.ui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import xyz.d1n0.lib.constant.BacklightDuration
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.DateFormat
import xyz.d1n0.lib.constant.FontStyle
import xyz.d1n0.lib.constant.HandGroup
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.constant.WeekdayLanguage
import xyz.d1n0.resources.*

// Display names of the protocol enums, so lib/constant holds no UI text. The `when`s are
// exhaustive, a new enum value without a label doesn't compile.

val TimerStatus.label: StringResource
	get() = when (this) {
		TimerStatus.NOT_STARTED -> Res.string.timer_status_not_started
		TimerStatus.ACTIVE -> Res.string.timer_status_active
		TimerStatus.SUSPENDED -> Res.string.timer_status_suspended
	}

val HandGroup.label: StringResource
	get() = when (this) {
		HandGroup.CLOCK -> Res.string.hand_group_clock
		HandGroup.MODE -> Res.string.hand_group_mode
	}

val FontStyle.label: StringResource
	get() = when (this) {
		FontStyle.STANDARD -> Res.string.font_style_standard
		FontStyle.CLASSIC -> Res.string.font_style_classic
	}

val BacklightDuration.label: StringResource
	get() = when (this) {
		BacklightDuration.SHORT -> Res.string.backlight_duration_short
		BacklightDuration.LONG -> Res.string.backlight_duration_long
	}

val ReminderRecurrence.label: StringResource
	get() = when (this) {
		ReminderRecurrence.ONCE -> Res.string.reminder_recurrence_once
		ReminderRecurrence.REPEAT_DAILY -> Res.string.reminder_recurrence_daily
		ReminderRecurrence.REPEAT_WEEKLY -> Res.string.reminder_recurrence_weekly
		ReminderRecurrence.REPEAT_MONTHLY -> Res.string.reminder_recurrence_monthly
		ReminderRecurrence.REPEAT_YEARLY -> Res.string.reminder_recurrence_yearly
	}

val WeekdayLanguage.label: StringResource
	get() = when (this) {
		WeekdayLanguage.EN -> Res.string.weekday_language_english
		WeekdayLanguage.ES -> Res.string.weekday_language_spanish
		WeekdayLanguage.FR -> Res.string.weekday_language_french
		WeekdayLanguage.DE -> Res.string.weekday_language_german
		WeekdayLanguage.IT -> Res.string.weekday_language_italian
		WeekdayLanguage.RU -> Res.string.weekday_language_russian
	}

/** Abbreviated name, e.g. Mon. */
val ReminderDayOfWeek.label: StringResource
	get() = when (this) {
		ReminderDayOfWeek.SUNDAY -> Res.string.day_sunday_short
		ReminderDayOfWeek.MONDAY -> Res.string.day_monday_short
		ReminderDayOfWeek.TUESDAY -> Res.string.day_tuesday_short
		ReminderDayOfWeek.WEDNESDAY -> Res.string.day_wednesday_short
		ReminderDayOfWeek.THURSDAY -> Res.string.day_thursday_short
		ReminderDayOfWeek.FRIDAY -> Res.string.day_friday_short
		ReminderDayOfWeek.SATURDAY -> Res.string.day_saturday_short
	}

/** The watch's own date patterns, not translated. */
val DateFormat.label: String
	get() = when (this) {
		DateFormat.MMDD -> "MM.DD"
		DateFormat.DDMM -> "DD.MM"
	}

/** e.g. 3 minutes. The byte is the timeout in minutes. */
val ConnectionTimeout.label: String
	@Composable get() = byte.toInt().let { minutes ->
		pluralStringResource(Res.plurals.settings_connection_timeout_minutes, minutes, minutes)
	}
