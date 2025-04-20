package xyz.d1n0.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Alarm
import androidx.compose.material.icons.sharp.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.helper.toHourMinuteString
import xyz.d1n0.lib.model.Alarm

@Preview
@Composable
fun Alarm(
    alarm: Alarm,
    isSnooze: Boolean = false,
    modifier: Modifier = Modifier
) {

    CardView(
        leadingIcon = {
            if (isSnooze)
                Icon(
                    imageVector = Icons.Sharp.Snooze,
                    contentDescription = "Snooze Alarm",
                )
            else
                Icon(
                    imageVector = Icons.Sharp.Alarm,
                    contentDescription = "Alarm",
                )
        },
        indicator = {
            Switch(
                checked = alarm.enable,
                onCheckedChange = {  },
            )
        },
        content = {
            Text(
                text = alarm.time.toHourMinuteString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
            )
        }
    )
}