package xyz.d1n0.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.HourlySignal

@Preview
@Composable
fun HourlySignalCard(
	hourlySignal: HourlySignal,
	modifier: Modifier = Modifier,
) {
	CardView(modifier = modifier, leadingIcon = {
		Icon(
			imageVector = Icons.Sharp.NotificationsActive,
			contentDescription = "Hourly Signal",
		)
	}, title = {
		Text(
			text = "Hourly Signal",
			textAlign = TextAlign.Center,
			style = MaterialTheme.typography.headlineSmall,
		)
	}, indicator = {
		Switch(
			checked = hourlySignal.enable,
			onCheckedChange = { },
		)
	}, content = {})
}