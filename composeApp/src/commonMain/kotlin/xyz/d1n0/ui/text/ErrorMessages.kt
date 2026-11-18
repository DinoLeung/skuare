package xyz.d1n0.ui.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import xyz.d1n0.lib.model.DurationTooLongException
import xyz.d1n0.lib.model.TextTooLongException
import xyz.d1n0.lib.model.ValueOutOfRangeException
import xyz.d1n0.lib.model.WatchErrorPacket
import xyz.d1n0.resources.*

/**
 * What to show the user for an error, in their language. Validation errors say what is wrong, the
 * watch's error packets show their bytes, and anything unexpected keeps its English message as a
 * detail for bug reports.
 */
val Throwable.userMessage: String
	@Composable get() = when (this) {
		is TextTooLongException ->
			pluralStringResource(Res.plurals.error_text_too_long, maxLength, maxLength)

		is DurationTooLongException -> limit.inWholeHours.toInt().let { hours ->
			pluralStringResource(Res.plurals.error_duration_too_long_hours, hours, hours)
		}

		is ValueOutOfRangeException ->
			stringResource(Res.string.error_value_out_of_range, range.first, range.last)

		is WatchErrorPacket -> stringResource(Res.string.error_watch_replied, hex)

		else -> message?.let { stringResource(Res.string.error_unexpected, it) }
			?: stringResource(Res.string.common_unknown_error)
	}
