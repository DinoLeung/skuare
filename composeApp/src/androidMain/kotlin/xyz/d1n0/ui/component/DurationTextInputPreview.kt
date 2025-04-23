package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Preview
@Composable
fun DurationTextInputPreview() {
	var duration by remember { mutableStateOf(69.minutes) }

	Card(modifier = Modifier.padding(8.dp)) {
		DurationTextInput(
			duration = duration,
			onDurationChange = { duration = it },
			modifier = Modifier.padding(8.dp),
			isError = duration > 12.hours,
			supportingText = {
				if (duration > 12.hours) {
					Text("Cannot exceed 12 hours.")
				}
			}
		)
	}
}