package xyz.d1n0.screen.alarms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TempScreen(
    onConnect: () -> Unit
) {
    Box (modifier = Modifier
        .fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Button(
            onClick = onConnect,
        ) {
            Text(
                text = "Go",
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}