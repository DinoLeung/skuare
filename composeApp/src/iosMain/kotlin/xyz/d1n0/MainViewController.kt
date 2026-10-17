package xyz.d1n0

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController(configure = { initKoin() }) { App() }
