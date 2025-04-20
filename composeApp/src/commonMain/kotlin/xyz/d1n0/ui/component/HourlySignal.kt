package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.HourlySignal

@Preview
@Composable
fun HourlySignal(
    hourlySignal: HourlySignal,
    modifier: Modifier = Modifier
) {
    Card (
        modifier = Modifier.padding(8.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column (modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.align(Alignment.Start).fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Sharp.NotificationsActive,
                    contentDescription = "Home Time",
                )
                Switch(
                    checked = hourlySignal.enable,
                    onCheckedChange = {  },
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.align(Alignment.Start).fillMaxWidth(),
            ) {
                Text(
                    text = "Hourly Signal",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }
    }
}