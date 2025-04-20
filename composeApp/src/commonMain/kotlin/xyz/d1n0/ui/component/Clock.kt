package xyz.d1n0.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Clock
import xyz.d1n0.lib.model.HomeClock

@Preview
@Composable
fun Clock (
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    CardView(
        leadingIcon = {
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
        },
        indicator = {
            AssistChip(
                enabled = clock.dstSettings.enable,
                label = { Text("DST") },
                onClick = { },
            )
        },
        content = {
            Text(
                text = clock.timeZone.cityName.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = clock.offsetString(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    )
}
