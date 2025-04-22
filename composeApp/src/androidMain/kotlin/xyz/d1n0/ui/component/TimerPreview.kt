package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    Column {
        TimerCard(
            timer = timer,
            onValueChange = { timer = timer.copy(duration = it) }
        )
    }
}