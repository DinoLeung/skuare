package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Preview
@Composable
fun TimerCard (
    timer: Timer,
    onValueChange: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    CardView(
        modifier = modifier,
        leadingIcon = { },
        title = { },
        indicator = {
            AssistChip(
                label = { Text(timer.status.displayName) },
                onClick = { },
            )
        },
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                DurationTextInput(
                    duration = timer.duration,
                    onDurationChange = { onValueChange(it) },
                    isError = timer.duration >= 24.hours,
                    supportingText = {
                        if (timer.duration >= 24.hours)
                            Text("Timer duration must not exceed 24 hours")
                    }
                )
            }
        }
    )
}