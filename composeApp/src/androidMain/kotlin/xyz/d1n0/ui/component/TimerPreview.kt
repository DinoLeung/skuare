package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.model.Timer
import kotlin.time.Duration.Companion.minutes

@Preview
@Composable
fun TimerPreview() {
	var timer by remember {
		mutableStateOf(
			Timer(
				status = TimerStatus.NOT_STARTED,
				duration = 69.minutes
			)
		)
	}

	CardView(
		modifier = Modifier.fillMaxWidth(),
		title = { Text("Timer") },
		indicator = {
			AssistChip(
				label = { Text(timer.status.toString()) },
				onClick = { },
			)
		},
	) {
		DurationTextInput(
			duration = timer.duration,
			onDurationChange = { timer = timer.copy(duration = it) },
			modifier = Modifier.fillMaxWidth()
		)
	}
}