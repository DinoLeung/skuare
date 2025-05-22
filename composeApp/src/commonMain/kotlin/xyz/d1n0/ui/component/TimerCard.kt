package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.model.Timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun TimerCard(
	modifier: Modifier = Modifier,
	timer: Timer,
	onDurationChange: (Duration) -> Unit,
	enabled: Boolean = true,
	isError: Boolean = false,
	supportingText: @Composable (() -> Unit)? = null,
) {
	CardView(
		modifier = modifier,
		title = { Text("Timer") },
		indicator = {
			AssistChip(
				label = { Text(timer.status.toString()) },
				onClick = { /* no-op */ },
			)
		},
	) {
		DurationTextInput(
			duration = timer.duration,
			onDurationChange = onDurationChange,
			enabled = enabled,
			isError = isError,
			supportingText = supportingText,
			modifier = Modifier.fillMaxWidth()
		)
	}
}

@Preview
@Composable
private fun TimerCardPreview() {
	Column {
		TimerCard(
			modifier = Modifier.fillMaxWidth(),
			timer = Timer(duration = 69.seconds, status = TimerStatus.NOT_STARTED),
			onDurationChange = {},
			enabled = true,
			isError = false,
			supportingText = null,
		)
		TimerCard(
			modifier = Modifier.fillMaxWidth(),
			timer = Timer(duration = 69.minutes, status = TimerStatus.ACTIVE),
			onDurationChange = {},
			enabled = true,
			isError = true,
			supportingText = { Text("Something wrong") },
		)

		TimerCard(
			modifier = Modifier.fillMaxWidth(),
			timer = Timer(duration = 24.hours - 1.seconds, status = TimerStatus.SUSPENDED),
			onDurationChange = {},
			enabled = false,
			isError = false,
			supportingText = null,
		)
	}
}