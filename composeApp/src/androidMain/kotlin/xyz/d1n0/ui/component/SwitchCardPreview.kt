package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun SwitchCardPreview() {
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