package xyz.d1n0.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
	var selectedIndex by remember { mutableStateOf(0) }
	val listState = rememberLazyListState()

	LaunchedEffect(expanded) {
		selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
		listState.scrollToItem(selectedIndex)
	}

	Box(modifier = modifier.height(IntrinsicSize.Min)) {
		TextField(
			value = selectedOption.name,
			label = { Text(label) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
			colors = ExposedDropdownMenuDefaults.textFieldColors(),
			enabled = enabled,
			readOnly = true,
			onValueChange = { /* read-only */ },
			modifier = Modifier.fillMaxWidth()
		)
		Surface(
			modifier = Modifier
				.fillMaxSize()
				.clip(MaterialTheme.shapes.extraSmall)
				.clickable(enabled = enabled) { expanded = true },
			color = Color.Transparent,
		) {}
		if (expanded) {
			Dialog(onDismissRequest = { expanded = false }) {
				Surface(shape = MaterialTheme.shapes.medium) {
					LazyColumn(
						state = listState,
						modifier = Modifier.fillMaxWidth(),
					) {
						itemsIndexed(options) { index, option ->
							val isSelected = option == selectedOption
							DropdownMenuItem(
								text = { Text(option.name) },
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
									onOptionSelected(option)
									expanded = false
								}
							)
						}
					}
				}
			}
		}
	}
}