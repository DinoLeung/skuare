package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Timer

@Preview
@Composable
fun TimerCard (
    timer: Timer,
    modifier: Modifier = Modifier,
) {
    CardView(
        modifier = modifier,
        leadingIcon = { },
        title = { },
        indicator = {
            AssistChip(
                label = { timer.status.name },
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
                    onDurationChange = {},
                )
            }
        }
    )
}