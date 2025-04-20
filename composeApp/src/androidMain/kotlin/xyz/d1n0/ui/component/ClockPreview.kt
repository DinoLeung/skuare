package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.DstSettings

@Preview
@Composable
fun ClockPreview() {
    Column {
        ClockCard(
            clock = xyz.d1n0.lib.model.HomeClock.fromTimeZoneId(30335, DstSettings(false, true))
        )
        ClockCard(
            clock = xyz.d1n0.lib.model.WorldClock.fromTimeZoneId(95, DstSettings(true, false))
        )
    }
}