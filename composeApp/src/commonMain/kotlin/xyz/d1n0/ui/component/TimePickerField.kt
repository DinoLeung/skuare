package xyz.d1n0.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import xyz.d1n0.lib.helper.toHHMMString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
	value: LocalTime,
	onValueChange: (LocalTime) -> Unit,
//	label: String,
	trailingIcon: @Composable() (() -> Unit)? = null,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
) {
	var expanded by remember { mutableStateOf(false) }

	Box(
		modifier = modifier.height(IntrinsicSize.Min).width(IntrinsicSize.Min)
	) {
		OutlinedTextField(
			value = value.toHHMMString(),
			onValueChange = { /* no-op */ },
//			label = { Text(label) },
			readOnly = true,
			enabled = enabled,
			trailingIcon = trailingIcon,
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

	TimePicketDialog(
		time = value,
		visible = expanded,
		onConfirm = {
			onValueChange(it)
			expanded = false
		},
		onDismiss = { expanded = false },
	)
}