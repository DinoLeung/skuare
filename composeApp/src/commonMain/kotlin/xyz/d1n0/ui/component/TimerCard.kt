package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Timer
import kotlin.time.Duration

@Preview
@Composable
fun TimerCard(
	timer: Timer,
	onValueChange: (Duration) -> Unit,
	saveButtonEnabled: Boolean = true,
	saveButtonOnClick: () -> Unit,
	inputEnabled: Boolean = true,
	isError: Boolean = false,
	supportingText: @Composable() (() -> Unit)? = null,
	modifier: Modifier = Modifier,
) {
	CardView(modifier = modifier, leadingIcon = { }, title = { }, indicator = {
		AssistChip(
			label = { Text(timer.status.toString()) },
			onClick = { },
		)
	}, content = {
		Row(
			horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
		) {
			DurationTextInput(
				duration = timer.duration,
				onDurationChange = { onValueChange(it) },
				enabled = inputEnabled,
				isError = isError,
				supportingText = supportingText,
				modifier = Modifier.weight(3f)
			)
			Spacer(modifier = Modifier.weight(0.2f))
			Button(
				onClick = saveButtonOnClick,
				enabled = saveButtonEnabled,
				modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
			) {
				Text(text = "Save", maxLines = 1)
			}
		}
	})
}