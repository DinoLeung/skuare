package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Clock
import xyz.d1n0.lib.model.HomeClock

@Preview
@Composable
fun Clock (
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    Card (
        modifier = Modifier.padding(8.dp),
        shape = MaterialTheme.shapes.medium,
    ) {

        Column (modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            Row (
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(8.dp).align(Alignment.Start).fillMaxWidth(),
            ) {
                if (clock is HomeClock) {
                    Icon(
                        imageVector = Icons.Sharp.Home,
                        contentDescription = "Home Time",
                    )
                } else {
                    Icon(
                        imageVector = Icons.Sharp.Public,
                        contentDescription = "World Time",
                    )
                }

                AssistChip(
                    enabled = clock.dstSettings.enable,
                    label = { Text("DST") },
                    onClick = { },
                )
            }
            Row (
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally).fillMaxWidth(),
            ) {
                Text(
                    text = clock.timeZone.cityName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = clock.offsetString(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(0.5f)
                )
            }
        }
    }
}
