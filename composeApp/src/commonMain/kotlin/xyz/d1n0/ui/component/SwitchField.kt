package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwitchField(
	modifier: Modifier = Modifier,
	title: @Composable() (() -> Unit),
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
			title()
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

@Composable
fun SwitchField(
	modifier: Modifier = Modifier,
	title: String,
	check: Boolean,
	onCheckedChange: ((Boolean) -> Unit)? = null,
	enabled: Boolean = true,
) = SwitchField(
	modifier = modifier,
	title = {
		Text(
			text = title,
			textAlign = TextAlign.Start,
			style = MaterialTheme.typography.bodyMedium
		)
	},
	check = check,
	onCheckedChange = onCheckedChange,
	enabled = enabled
)

@Preview
@Composable
private fun SwitchCardPreview() {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.safeContentPadding()
	) {
		CardView(modifier = Modifier.fillMaxWidth()) {
			SwitchField(
				title = "Test 123",
				check = true,
				onCheckedChange = {},
				enabled = true,
				modifier = Modifier.fillMaxWidth()
			)
		}

		CardView(modifier = Modifier.fillMaxWidth()) {
			SwitchField(
				title = "Test 123",
				check = true,
				onCheckedChange = {},
				enabled = false,
				modifier = Modifier.fillMaxWidth()
			)
		}

		CardView(modifier = Modifier.fillMaxWidth()) {
			SwitchField(
				title = "Test 456",
				check = false,
				onCheckedChange = {},
				enabled = true,
				modifier = Modifier.fillMaxWidth()
			)
		}

		CardView(modifier = Modifier.fillMaxWidth()) {
			SwitchField(
				title = "Test 456",
				check = false,
				onCheckedChange = {},
				enabled = false,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}