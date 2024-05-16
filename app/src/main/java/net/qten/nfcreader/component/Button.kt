package net.qten.nfcreader.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Button(
    text: String,
    fontSize: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(2.dp, Color.Black),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            modifier = Modifier.align(alignment = Alignment.CenterVertically)
        )
    }
}