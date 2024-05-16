package net.qten.nfcreader.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QtenTextButton(text: String, onClick: () -> Unit) {
    TextButton(modifier = Modifier
        .padding(vertical = 8.dp),
        onClick = { onClick() }) {
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Back to main menu",
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = text, fontSize = 20.sp
        )
    }
}