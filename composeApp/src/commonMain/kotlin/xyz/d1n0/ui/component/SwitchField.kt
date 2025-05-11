package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SwitchField(
	modifier: Modifier = Modifier,
	title: String,
	check: Boolean,
	onCheckedChange: ((Boolean) -> Unit)? = null,
	enabled: Boolean = true,
) {
	Row(
		modifier = modifier,
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Box(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				textAlign = TextAlign.Start,
				style = MaterialTheme.typography.bodyMedium
			)
		}
		Box {
			Switch(
				checked = check,
				onCheckedChange = onCheckedChange,
				enabled = enabled,
			)
		}
	}
}