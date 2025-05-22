package xyz.d1n0.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.WeekdayLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> EnumDropdown(
	selectedOption: T,
	options: Array<T>,
	onOptionSelected: (T) -> Unit,
	label: String,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	var expanded by remember { mutableStateOf(false) }

	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = { expanded = !expanded },
		modifier = modifier
	) {
		OutlinedTextField(
			value = selectedOption.toString(),
			onValueChange = { /* no-op */ },
			label = { Text(label) },
			readOnly = true,
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
			colors = ExposedDropdownMenuDefaults.textFieldColors(),
			modifier = Modifier
				.menuAnchor(MenuAnchorType.PrimaryNotEditable)
				.fillMaxWidth()
		)
		ExposedDropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false },
		) {
			options.forEach {
				val isSelected = it == selectedOption
				DropdownMenuItem(
					text = { Text(it.toString()) },
					leadingIcon = {
						if (isSelected) {
							Icon(
								imageVector = Icons.Filled.Check,
								contentDescription = "Selected",
								modifier = Modifier.size(20.dp)
							)
						}
					},
					modifier = Modifier.background(
						color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
						else MaterialTheme.colorScheme.surfaceContainer
					),
					onClick = {
						onOptionSelected(it)
						expanded = false
					}
				)
			}
		}
	}
}

@Preview
@Composable
private fun EnumDropdownPreview() {
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