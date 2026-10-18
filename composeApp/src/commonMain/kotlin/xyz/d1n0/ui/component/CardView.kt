package xyz.d1n0.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun CardView(
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	onClick: (() -> Unit)? = null,
	onLongClick: (() -> Unit)? = null,
	border: BorderStroke? = null,
	leadingIcon: @Composable (() -> Unit)? = null,
	title: @Composable (() -> Unit)? = null,
	indicator: @Composable (() -> Unit)? = null,
	content: @Composable (() -> Unit)? = null,
) {
	val cardContent: @Composable ColumnScope.() -> Unit = {
		Column(modifier = Modifier.padding(16.dp)) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.align(Alignment.Start).fillMaxWidth(),
			) {
				leadingIcon?.let { Box { it() } }
				title?.let { Box(modifier = Modifier.weight(1f)) { it() } }
				indicator?.let { Box { it() } }
			}
			content?.let { it() }
		}
	}

	if (onLongClick != null) {
		// Card(onClick) has no long click, so handle both clicks here and apply the disabled colors it would use
		val colors = CardDefaults.cardColors().let {
			if (enabled) it
			else it.copy(containerColor = it.disabledContainerColor, contentColor = it.disabledContentColor)
		}
		Card(
			modifier = modifier
				.clip(MaterialTheme.shapes.medium)
				.combinedClickable(
					enabled = enabled,
					onClick = onClick ?: {},
					onLongClick = onLongClick,
				),
			shape = MaterialTheme.shapes.medium,
			border = border,
			colors = colors,
			content = cardContent,
		)
	} else if (onClick == null) {
		Card(
			modifier = modifier,
			shape = MaterialTheme.shapes.medium,
			border = border,
			content = cardContent,
		)
	} else {
		Card(
			onClick = onClick,
			modifier = modifier,
			enabled = enabled,
			shape = MaterialTheme.shapes.medium,
			border = border,
			content = cardContent,
		)
	}
}
