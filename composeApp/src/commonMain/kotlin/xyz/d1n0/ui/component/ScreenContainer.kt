package xyz.d1n0.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScreenContainer(
	modifier: Modifier,
	saveVisible: Boolean,
	saveOnClick: () -> Unit,
	content: @Composable () -> Unit,
) {
	Box(modifier = modifier) {

		content()

		AnimatedVisibility(
			visible = saveVisible,
			enter = slideInVertically { it },
			exit = slideOutVertically { it },
			modifier = Modifier
				.align(Alignment.BottomEnd)
				.padding(16.dp)
		) {
			FloatingActionButton(
				onClick = saveOnClick,
			) {
				Icon(
					imageVector = Icons.Outlined.Save,
					contentDescription = "Save",
				)
			}
		}
	}
}