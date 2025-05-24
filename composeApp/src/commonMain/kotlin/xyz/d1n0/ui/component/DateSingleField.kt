package xyz.d1n0.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.helper.toDayMonthYear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSinglePickerField(
	value: LocalDate,
	onValueChange: (LocalDate) -> Unit,
	label: String,
	leadingIcon: @Composable() (() -> Unit)? = null,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	var expanded by remember { mutableStateOf(false) }

	Box(
		modifier = modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Min)
	) {
		OutlinedTextField(
			value = value.toDayMonthYear(),
			onValueChange = { /* no-op */ },
//			textStyle = TextStyle(fontSize = TextUnit(value = 3.5f, type = TextUnitType.Em)),
			label = { Text(label) },
			readOnly = true,
			enabled = enabled,
			leadingIcon = leadingIcon,
			colors = ExposedDropdownMenuDefaults.textFieldColors(),
			modifier = Modifier.fillMaxWidth()
		)
		Surface(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 8.dp)
				.clip(MaterialTheme.shapes.extraSmall)
				.clickable(enabled = enabled) { expanded = true },
			color = Color.Transparent,
		) {}

	}

	DateSinglePickerDialog(
		date = value,
		visible = expanded,
		onConfirm = {
			onValueChange(it)
			expanded = false
		},
		onDismiss = { expanded = false },
	)
}

@Preview
@Composable
private fun DateSinglePickerFieldPreview() {
	var date by remember { mutableStateOf(LocalDate(2025, 6, 9)) }

	Column(
		modifier = Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		DateSinglePickerField(
			value = date,
			onValueChange = { date = it },
			label = "on",
			leadingIcon = {
				Icon(
					imageVector = Icons.Outlined.CalendarMonth,
					contentDescription = "Alarm"
				)
			},
			enabled = false,
			modifier = Modifier.fillMaxWidth()
		)
		DateSinglePickerField(
			value = date,
			onValueChange = { date = it },
			label = "on",
			leadingIcon = {
				Icon(
					imageVector = Icons.Outlined.CalendarToday,
					contentDescription = "Snooze Alarm"
				)
			},
			enabled = true,
			modifier = Modifier.fillMaxWidth()
		)
	}
}