package xyz.d1n0.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.OutlinedSecureTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.d1n0.ui.navigation.navBarItems

@Preview
@Composable
fun ScreenContainerPreview() {
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Test Screen") },
			)
		},
		bottomBar = {
			AnimatedVisibility(
				visible = true,
				enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
				exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight })
			) {
				NavBar(
					items = navBarItems,
					currentRoute = null,
					onItemClick = {},
				)
			}
		},
	) { scaffoldPadding ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(scaffoldPadding)
				.consumeWindowInsets(scaffoldPadding)
				.imePadding()
		) {
			SaveScreenScaffold(
				modifier = Modifier
					.background(color = Color.Cyan),
				saveFabVisible = true,
				saveFabOnClick = {},
			) {
				Column(
					modifier = Modifier.fillMaxSize(),
					verticalArrangement = Arrangement.SpaceBetween
				) {
					OutlinedSecureTextField(
						state = rememberTextFieldState(),
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp),
					)

					Box(
						modifier = Modifier
							.fillMaxWidth()
							.height(18.dp)
							.background(color = Color.Green)
					)
				}
			}
		}
	}
}