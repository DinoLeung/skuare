package xyz.d1n0.ui.screen.clocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClocksScreen(innerPadding: PaddingValues) {
    Box (modifier = Modifier
        .fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center
    ){
        Text(
            text = "Clocks Screen",
            style = MaterialTheme.typography.headlineLarge
        )
    }
}