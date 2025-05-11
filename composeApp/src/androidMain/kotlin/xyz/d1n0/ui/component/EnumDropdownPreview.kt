package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.lib.constant.AutoSyncDelay
import xyz.d1n0.lib.constant.WeekdayLanguage

@Preview
@Composable
fun EnumDropdownPreview() {
	var selectedDelay by remember { mutableStateOf(AutoSyncDelay.MINUTE_30) }
	var selectedLanguage by remember { mutableStateOf(WeekdayLanguage.EN) }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.safeContentPadding(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		EnumDropdown(
			selectedOption = selectedDelay,
			onOptionSelected = { selectedDelay = it },
			label = "Select delay",
			options = AutoSyncDelay.values(),
			modifier = Modifier
				.fillMaxWidth()
		)

		EnumDropdown(
			selectedOption = selectedLanguage,
			onOptionSelected = { selectedLanguage = it },
			label = "Select language",
			options = WeekdayLanguage.values(),
			modifier = Modifier
				.fillMaxWidth()
		)

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			EnumDropdown(
				selectedOption = selectedDelay,
				onOptionSelected = { selectedDelay = it },
				label = "Select delay",
				options = AutoSyncDelay.values(),
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