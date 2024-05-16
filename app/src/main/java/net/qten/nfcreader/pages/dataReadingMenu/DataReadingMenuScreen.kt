package net.qten.nfcreader.pages.dataReadingMenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.qten.nfcreader.R
import net.qten.nfcreader.component.Button
import net.qten.nfcreader.component.QtenScreenTitle
import net.qten.nfcreader.constant.NfcConstant

@Composable
fun DataReadingMenuScreen(
    onNavigateBack: () -> Unit, onNavigateToScanScreenWithMode: (Int) -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            //Back to main menu
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = onNavigateBack, modifier = Modifier.padding(0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Back to main menu",
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(R.string.menu_main_title), fontSize = 20.sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                QtenScreenTitle(text = stringResource(id = R.string.data_reading_menu))

                Spacer(modifier = Modifier.size(60.dp))

                // Button
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(text = stringResource(id = R.string.button_mode_main_body_title),
                        fontSize = 20,
                        onClick = { onNavigateToScanScreenWithMode(NfcConstant.ReaderMode.DEVICE_MEMORY.mode) })
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(text = stringResource(id = R.string.button_mode_nfc_title),
                        fontSize = 20,
                        onClick = { onNavigateToScanScreenWithMode(NfcConstant.ReaderMode.NFC.mode) })
                }
            }
        }
    }
}