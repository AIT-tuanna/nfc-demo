package net.qten.nfcreader.pages.dosageDataListScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import net.qten.nfcreader.R
import net.qten.nfcreader.component.LoadingOrContent
import net.qten.nfcreader.component.QtenScreenTitle
import net.qten.nfcreader.component.QtenTextButton
import net.qten.nfcreader.component.ScrollContent
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.viewModels.DosageReadingViewModel

@Composable
fun DosageDataListScreen(
    navController: NavHostController, viewModel: DosageReadingViewModel
) {
    val context = LocalContext.current
    val mode = remember("Mode") {
        navController.currentBackStackEntry?.arguments?.getString("mode")
    }

    val isFromMenu by viewModel.isFromMenu.collectAsState()

    // Observe the loading and dosages from the ViewModel
    val loading by viewModel.loading.collectAsState()
    val items by viewModel.dosages.collectAsState()

    LaunchedEffect(mode) {
        // We need to process csv when navigate from menu
        // because of the change if exists in files
        if (isFromMenu) {
            viewModel.processCsv(context.getExternalFilesDir(null)?.toPath(), mode)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Back to データ呼出しメニュー
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                QtenTextButton(
                    text = stringResource(R.string.data_extract_menu_title),
                    onClick = {
                        navController.navigate(NfcConstant.Route.DATA_LIST_MENU.routeName)
                        resetLazyListState(viewModel)
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            QtenScreenTitle(
                text = stringResource(id = R.string.list_title).format(
                    when (mode) {
                        NfcConstant.ReaderMode.DEVICE_MEMORY.toString() -> stringResource(id = R.string.list_title_mode_main_body)
                        NfcConstant.ReaderMode.NFC.toString() -> stringResource(id = R.string.list_title_mode_nfc)
                        else -> throw Exception("Invalid mode")
                    }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LoadingOrContent(
                loading = loading,
                text = stringResource(id = R.string.loading)
            ) {
                ScrollContent(
                    navController = navController, mode = mode, items = items, viewModel = viewModel
                )
            }
        }

    }
}

/**
 * Reset all lazy list state when navigate from menu
 * @param viewModel [DosageReadingViewModel]
 */
fun resetLazyListState(viewModel: DosageReadingViewModel) {
    // Reset lazy list scroll state:
    viewModel.firstVisibleIndex = 0
    viewModel.firstVisibleIndexOffset = 0

    viewModel.firstVisibleIndexNfc = 0
    viewModel.firstVisibleIndexOffsetNfc = 0
}