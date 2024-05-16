package net.qten.nfcreader.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.utils.Utils

@Composable
fun DosageDataItem(
    onClick: () -> Unit, item: DosageDto?, backgroundColor: Int
) {
    ListItem(
        headlineContent = {
            Text(
                text = "SN: ${item?.serialNumber}", fontSize = 16.sp
            )
        },
        supportingContent = {
            Text(
                text = Utils.dateToString(item?.absTime!!), fontSize = 16.sp
            )
        },
        modifier = Modifier
            .clickable { onClick() }, colors = ListItemDefaults.colors(Color(backgroundColor))
    )
}