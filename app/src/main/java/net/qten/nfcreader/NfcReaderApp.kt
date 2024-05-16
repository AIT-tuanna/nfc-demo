package net.qten.nfcreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.pages.dataExtractMenu.DataExtractMenuScreen
import net.qten.nfcreader.pages.dataReadingMenu.DataReadingMenuScreen
import net.qten.nfcreader.pages.dosageDataListScreen.DosageDataListScreen
import net.qten.nfcreader.pages.dosageDetailDataScreen.DosageDetailDataScreen
import net.qten.nfcreader.pages.dosageReadingMenu.DosageReadingMenu
import net.qten.nfcreader.pages.nfcReaderScreen.NfcReaderScreen
import net.qten.nfcreader.viewModels.DosageReadingViewModel
import net.qten.nfcreader.viewModels.NfcReaderViewModel

@Composable
fun NfcReaderApp(
    navController: NavHostController,
    nfcReaderViewModel: NfcReaderViewModel,
    enableNfc: () -> Unit,
    disableNfc: () -> Unit
) {
    val viewModel = remember {
        DosageReadingViewModel()
    }
    NfcReaderNavHost(navController, nfcReaderViewModel, enableNfc, disableNfc, viewModel)
}

@Composable
fun NfcReaderNavHost(
    navController: NavHostController,
    nfcReaderViewModel: NfcReaderViewModel,
    enableNfc: () -> Unit,
    disableNfc: () -> Unit,
    sharedViewModel: DosageReadingViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = NfcConstant.Route.HOME_MENU.routeName
) {
    NavHost(
        modifier = modifier, navController = navController, startDestination = startDestination
    ) {
        composable(NfcConstant.Route.HOME_MENU.routeName) {
            DosageReadingMenu(onNavigateToScanMenu = { navController.navigate(NfcConstant.Route.DATA_READING_MENU.routeName) },
                onNavigateToList = { navController.navigate(NfcConstant.Route.DATA_LIST_MENU.routeName) })
        }

        composable(NfcConstant.Route.DATA_READING_MENU.routeName) {
            DataReadingMenuScreen(
                onNavigateBack = { navController.navigate(NfcConstant.Route.HOME_MENU.routeName) },
                onNavigateToScanScreenWithMode = { mode ->
                    navController.navigate(NfcConstant.Route.NFC_READER.routeName)
                    nfcReaderViewModel.mode = mode
                })
        }

        // We need to define nfc screen reader route here
        // to navigate from menu:
        composable(NfcConstant.Route.NFC_READER.routeName) {
            NfcReaderScreen(
                onNavigateBack = { navController.navigate(NfcConstant.Route.DATA_READING_MENU.routeName) },
                nfcReaderViewModel = nfcReaderViewModel,
                enableNfc = enableNfc,
                disableNfc = disableNfc
            )
        }

        composable(
            route = "${NfcConstant.Route.DATA_LIST_MENU.routeName}/${NfcConstant.Route.LIST.routeName}?mode={mode}",
            arguments = listOf(navArgument("mode") { nullable })
        ) {
            DosageDataListScreen(navController, sharedViewModel)
        }

        composable(
            route = "${NfcConstant.Route.DATA_LIST_MENU.routeName}?mode={mode}&id={id}",
            arguments = listOf(navArgument(name = "mode") { nullable },
                navArgument(name = "id") { nullable })
        ) {
            DosageDetailDataScreen(
                navController,
                sharedViewModel
            )
        }

        composable(route = NfcConstant.Route.DATA_LIST_MENU.routeName) {
            DataExtractMenuScreen(navController = navController, viewModel = sharedViewModel)
        }
    }
}

