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
import xyz.d1n0.lib.helper.toHHMMString
import xyz.d1n0.lib.model.Alarm

@Preview
@Composable
fun AlarmCard(
    alarm: Alarm,
    isSnooze: Boolean = false,
    modifier: Modifier = Modifier
) {

    CardView(
        modifier = modifier,
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
        title = {
            Text(
                text = alarm.time.toHHMMString(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.headlineLarge,
            )
        },
        indicator = {
            Switch(
                checked = alarm.enable,
                onCheckedChange = {  },
            )
        },
        content = { }
    )
}