package xyz.d1n0.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SwitchCard(
	title: String,
	check: Boolean,
	onCheckedChange: ((Boolean) -> Unit)? = null,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	CardView(
		modifier = modifier,
		leadingIcon = {},
		title = {
			Text(
				text = title,
				textAlign = TextAlign.Start,
				style = MaterialTheme.typography.bodyMedium
			)
		},
		indicator = {
			Switch(
				checked = check,
				onCheckedChange = onCheckedChange,
				enabled = enabled,
			)
		},
		content = {},
	)
}