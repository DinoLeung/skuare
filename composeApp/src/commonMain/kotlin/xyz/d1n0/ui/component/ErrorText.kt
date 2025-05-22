package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ErrorText(text: String) {
	Text(
		modifier = Modifier.fillMaxWidth(),
		style = MaterialTheme.typography.bodySmall,
		color = MaterialTheme.colorScheme.error,
		text = text
	)
}