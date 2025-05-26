package xyz.d1n0.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.constant.ConnectionTimeout
import xyz.d1n0.lib.constant.ReminderDayOfWeek
import xyz.d1n0.lib.constant.ReminderRecurrence
import xyz.d1n0.lib.constant.WeekdayLanguage

@Composable
fun <T : Enum<T>> EnumDropdown(
	selectedOption: T,
	options: Set<T>,
	onOptionSelected: (T) -> Unit,
	label: String,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	BaseEnumDropdown(
		value = selectedOption.toString(),
		options = options,
		selectedChecker = { it == selectedOption },
		onItemClick = onOptionSelected,
		label = label,
		enabled = enabled,
		modifier = modifier
	)
}

@Composable
fun <T : Enum<T>> EnumDropdown(
	selectedOptions: Set<T>,
	options: Set<T>,
	onOptionSelected: (Set<T>) -> Unit,
	allSelectedText: String = "",
	noneSelectedText: String = "",
	label: String,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	BaseEnumDropdown(
		value = when (selectedOptions.size) {
			0 -> noneSelectedText
			options.size -> allSelectedText
			else -> selectedOptions.sorted().joinToString()
		},
		options = options,
		selectedChecker = { it in selectedOptions },
		onItemClick = { option ->
			onOptionSelected(
				if (option in selectedOptions) selectedOptions - option
				else selectedOptions + option
			)
		},
		label = label,
		isMultiSelect = true,
		enabled = enabled,
		modifier = modifier
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> BaseEnumDropdown(
	value: String,
	options: Set<T>,
	selectedChecker: (T) -> Boolean,
	onItemClick: (T) -> Unit,
	label: String,
	isMultiSelect: Boolean = false,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	var expanded by remember { mutableStateOf(false) }

	ExposedDropdownMenuBox(
		expanded = expanded,
		onExpandedChange = { if (enabled) expanded = !expanded },
		modifier = modifier
	) {
		OutlinedTextField(
			value = value,
			singleLine = true,
			readOnly = true,
			enabled = enabled,
			onValueChange = { /* no-op */ },
			label = { Text(label) },
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
			options.forEach { option ->
				val isSelected = selectedChecker(option)
				DropdownMenuItem(
					text = { Text(option.toString()) },
					leadingIcon = if (isMultiSelect) {
						{
							if (isSelected)
								Icon(
									imageVector = Icons.Filled.Check,
									contentDescription = "Selected",
									modifier = Modifier.size(20.dp)
								)
						}
					} else null,
					modifier = Modifier.background(
						color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
						else MaterialTheme.colorScheme.surfaceContainer
					),
					onClick = {
						onItemClick(option)
						if (isMultiSelect == false)
							expanded = false
					}
				)
			}
		}
	}
}

@Preview
@Composable
private fun EnumSingleDropdownPreview() {
	var connectionTimeout by remember { mutableStateOf(ConnectionTimeout.MINUTES_5) }
	var selectedLanguage by remember { mutableStateOf(WeekdayLanguage.EN) }

	var selectedRecurrence by remember { mutableStateOf(ReminderRecurrence.REPEAT_WEEKLY) }
	var selectedWeekDays by remember { mutableStateOf(setOf<ReminderDayOfWeek>()) }

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
				options = ConnectionTimeout.values().toSet(),
				modifier = Modifier.fillMaxWidth()
			)
		}

		CardView(modifier = Modifier.fillMaxWidth()) {
			EnumDropdown(
				selectedOption = selectedLanguage,
				onOptionSelected = { selectedLanguage = it },
				label = "Select language",
				options = WeekdayLanguage.values().toSet(),
				modifier = Modifier.wrapContentWidth(),
				enabled = false
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
						selectedOption = selectedRecurrence,
						onOptionSelected = { selectedRecurrence = it },
						label = "Recurrence",
						options = ReminderRecurrence.values().toSet(),
						modifier = Modifier.weight(1f)
					)
					EnumDropdown(
						selectedOptions = selectedWeekDays,
						onOptionSelected = { selectedWeekDays = it },
						label = "On",
						options = ReminderDayOfWeek.values().toSet(),
						modifier = Modifier.weight(2f)
					)
				}

			}
		}

	}
}