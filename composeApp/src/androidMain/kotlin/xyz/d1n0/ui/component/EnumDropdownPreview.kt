package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.WeekdayLanguage

@Preview
@Composable
fun EnumDropdownPreview() {
	var connectionTimeout by remember { mutableStateOf(ConnectionTimeout.MINUTES_5) }
	var selectedLanguage by remember { mutableStateOf(WeekdayLanguage.EN) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.safeContentPadding(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		CardView(modifier = Modifier.fillMaxWidth()) {
			EnumDropdown(
				selectedOption = connectionTimeout,
				onOptionSelected = { connectionTimeout = it },
				label = "Select delay",
				options = ConnectionTimeout.values(),
				modifier = Modifier.fillMaxWidth()
			)
		}

		CardView(modifier = Modifier.fillMaxWidth()) {
			EnumDropdown(
				selectedOption = selectedLanguage,
				onOptionSelected = { selectedLanguage = it },
				label = "Select language",
				options = WeekdayLanguage.values(),
				modifier = Modifier.wrapContentWidth()
			)
		}

		CardView(modifier = Modifier.fillMaxWidth()) {
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				SwitchField(
					title = "Test 123",
					check = true,
					onCheckedChange = {},
					enabled = true,
					modifier = Modifier.fillMaxWidth()
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					EnumDropdown(
						selectedOption = connectionTimeout,
						onOptionSelected = { connectionTimeout = it },
						label = "Select delay",
						options = ConnectionTimeout.values(),
						modifier = Modifier.weight(1f)
					)
					EnumDropdown(
						selectedOption = selectedLanguage,
						onOptionSelected = { selectedLanguage = it },
						label = "Select language",
						options = WeekdayLanguage.values(),
						modifier = Modifier.weight(1f)
					)
				}

			}
		}

	}
}