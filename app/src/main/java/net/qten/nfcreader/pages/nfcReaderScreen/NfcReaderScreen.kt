package net.qten.nfcreader.pages.nfcReaderScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.qten.nfcreader.R
import net.qten.nfcreader.component.LoadingOrContent
import net.qten.nfcreader.component.QtenTextButton
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.viewModels.NfcReaderViewModel
import net.qten.nfcreader.viewModels.NfcViewModel

@Composable
fun NfcReaderScreen(
    onNavigateBack: () -> Unit,
    nfcReaderViewModel: NfcReaderViewModel,
    enableNfc: () -> Unit, disableNfc: () -> Unit, viewModel: NfcViewModel = viewModel()
) {
    val isReadingNfc by viewModel.loading.collectAsState()
    LaunchedEffect(Unit) {
        nfcReaderViewModel.isNfcEnabled.value = true
        enableNfc()
    }
    DisposableEffect(Unit) {
        onDispose {
            nfcReaderViewModel.isNfcEnabled.value = false
            nfcReaderViewModel.notification.value = NfcConstant.EMPTY_STRING
            nfcReaderViewModel.isError.value = false
            disableNfc()
        }
    }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            //Back to main menu
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                QtenTextButton(text = stringResource(R.string.menu_scan_title), onNavigateBack)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = if (NfcConstant.ReaderMode.NFC.mode == nfcReaderViewModel.mode) {
                        stringResource(R.string.button_mode_nfc_title)
                    } else {
                        stringResource(R.string.button_mode_main_body_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = painterResource(id = R.drawable.nfc_icon_256),
                    contentDescription = NfcConstant.EMPTY_STRING
                )

                LoadingOrContent(loading = isReadingNfc, text = "Reading...") {
                    // Notification
                    Text(
                        text = nfcReaderViewModel.notification.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = if (nfcReaderViewModel.isError.value) Color.Red else Color.Black
                    )
                }

                Spacer(modifier = Modifier.size(60.dp))
            }
        }
    }
}