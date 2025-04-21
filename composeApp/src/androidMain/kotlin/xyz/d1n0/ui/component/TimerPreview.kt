package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.TimerStatus
import xyz.d1n0.lib.model.Timer
import kotlin.time.Duration.Companion.minutes

@Preview
@Composable
fun TimerPreview() {
    Column {
        TimerCard(
            timer = Timer(
                status = TimerStatus.NOT_STARTED,
                duration = 69.minutes
            )
        )
    }
}