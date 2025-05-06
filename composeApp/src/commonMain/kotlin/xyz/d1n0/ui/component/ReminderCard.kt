package xyz.d1n0.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.d1n0.lib.model.Reminder

@Preview
@Composable
fun ReminderCard(
	reminder: Reminder,
	modifier: Modifier = Modifier,
) {
	CardView(modifier = modifier, title = {
		Text(
			text = reminder.title?.let { it.value } ?: "<Empty>",
			textAlign = TextAlign.Center,
			style = MaterialTheme.typography.headlineSmall,
		)
	}, indicator = {
		Switch(
			checked = reminder.config?.enable ?: false,
			onCheckedChange = { },
		)
	}, content = {
		Row(
			horizontalArrangement = Arrangement.SpaceBetween,
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			reminder.config?.let {
				Text(
					text = it.recurrence.displayName,
					style = MaterialTheme.typography.titleMedium,
				)
				Text(
					text = it.recurrenceDisplayString,
					style = MaterialTheme.typography.titleMedium,
				)
			}
		}
	})
}