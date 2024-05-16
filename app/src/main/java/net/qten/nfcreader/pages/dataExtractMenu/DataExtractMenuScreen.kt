package net.qten.nfcreader.pages.dataExtractMenu

import android.icu.math.BigDecimal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.qten.nfcreader.R
import net.qten.nfcreader.component.Button
import net.qten.nfcreader.component.QtenScreenTitle
import net.qten.nfcreader.component.QtenTextButton
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.utils.Utils
import net.qten.nfcreader.viewModels.DosageReadingViewModel

@Composable
fun DataExtractMenuScreen(navController: NavHostController, viewModel: DosageReadingViewModel) {
    val scope = rememberCoroutineScope()

    val onClick = { mode: String ->
        scope.launch {
            viewModel.setIsFromMenu(true)
        }
        navController.navigate("${NfcConstant.Route.DATA_LIST_MENU.routeName}/${NfcConstant.Route.LIST.routeName}?mode=${mode}")
    }

    Scaffold(
        Modifier.fillMaxHeight()
    ) { innerPadding ->
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            QtenTextButton(
                text = stringResource(R.string.menu_main_title),
                onClick = { navController.navigate(NfcConstant.Route.HOME_MENU.routeName) },
            )
        }

        Column {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QtenScreenTitle(text = stringResource(id = R.string.data_extract_menu_title))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Button(text = stringResource(id = R.string.data_list_mode_main_body),
                        fontSize = 20,
                        onClick = {
                            onClick(NfcConstant.ReaderMode.DEVICE_MEMORY.toString())
                        })
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(text = stringResource(id = R.string.data_list_mode_main_nfc),
                        fontSize = 20,
                        onClick = {
                            onClick(NfcConstant.ReaderMode.NFC.toString())
                        })
                }
            }
        }
    }
}

fun createDosageDtoFromData(
    dataRow: List<String>, serialNumber: String = NfcConstant.EMPTY_STRING
): DosageDto {
    val id = StringBuilder().append(
        Utils.dateToString(
            Utils.stringToDate(dataRow[0])!!,
            DosageReadingViewModel.ID_DATE_FORMAT_YYYY_MM_DD_HH_MM_SS
        )
    ).append(serialNumber).toString()

    return DosageDto(
        id = id,
        serialNumber = when (serialNumber.isNotEmpty()) {
            true -> serialNumber
            false -> NfcConstant.EMPTY_STRING
        },
        absTime = Utils.stringToDate(dataRow[0])!!,
        batteryVol = BigDecimal(dataRow[1]),
        temperature = dataRow[2].toInt(),
        accumulatedCounter = dataRow[3].toUInt(),
        hp10DoseValue = BigDecimal(dataRow[4]),
        hp007DoseValue = BigDecimal(dataRow[5]),
        bgCorrectionError = dataRow[6].toBoolean(),
        cpuError = dataRow[7].toBoolean(),
        temperatureError = dataRow[8].toBoolean(),
        batteryVoltageError = dataRow[9].toBoolean(),
        betaSensorError = dataRow[10].toBoolean(),
        gammaSensorError = dataRow[11].toBoolean(),
        unequipped = dataRow[12].toBoolean(),
        impact = dataRow[13].toBoolean()
    )
}