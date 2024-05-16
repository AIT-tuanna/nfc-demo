package net.qten.nfcreader.pages.dosageDetailDataScreen

import android.icu.math.BigDecimal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.qten.nfcreader.R
import net.qten.nfcreader.component.QtenTextButton
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.constant.NfcConstant.Route
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.utils.Utils
import net.qten.nfcreader.viewModels.DosageReadingViewModel

const val DEGREE_CELSIUS_SYMBOL = "\u2103"
const val MICRO_SIEVERT_SYMBOL = "\u00b5Sv"
const val HOUR_SYMBOL = "h"
const val MILLI_SIEVERT_SYMBOL = "mSv"
const val VOLTAGE_SYMBOL = "V"

@Composable
fun DosageDetailDataScreen(
    navController: NavHostController, sharedViewModel: DosageReadingViewModel
) {
    val scope = rememberCoroutineScope()

    val mode = remember {
        navController.currentBackStackEntry?.arguments?.getString("mode")
    }

    val id = remember {
        navController.currentBackStackEntry?.arguments?.getString("id")
    }

    val route = remember {
        "${Route.DATA_LIST_MENU.routeName}/list?mode=${mode}"
    }

    val dosageDtoData = remember {
        mutableStateOf(DosageDto())
    }

    LaunchedEffect(null) {
        if (id != null) {
            sharedViewModel.findById(id)
            dosageDtoData.value = sharedViewModel.dosage.value!!
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Back to menu データ呼出メニュー
            QtenTextButton(
                text = stringResource(R.string.data_reading_menu),
                onClick = {
                    scope.launch {
                        sharedViewModel.setIsFromMenu(false)
                    }
                    navController.navigate(route)
                },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Datetime
            Row(
                horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()
            ) {
                // data?.integrationTime?.let { Text(formatDate(it)) }
                Text(
                    text = Utils.dateToString(dosageDtoData.value.absTime),
                    Modifier.padding(horizontal = 12.dp),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // UUID
            Row(
                horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()
            ) {
                // data?.id?.let { Text(it) }
                Text(
                    text = "SN: ${dosageDtoData.value.serialNumber}",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                // 積算時間
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    Row(
                        modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(id = R.string.integration_time),
                            fontSize = 20.sp,
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Row(modifier = Modifier.weight(1f)) {
                        // Text("${data?.integrationTime} h", fontSize = 24.sp)
                        Row(
                            modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End
                        ) {
                            // QA No.17:
                            Text(
                                text = dosageDtoData.value.accumulatedCounter.toString(),
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(0.05f))

                        Row(modifier = Modifier.weight(1f)) {
                            Text(HOUR_SYMBOL, fontSize = 20.sp)
                        }
                    }
                }

                // 積算線量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.cumulative_dose),
                            fontSize = 20.sp,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Row(modifier = Modifier.weight(1f)) {
                        // Text(text = "${data?.cumulativeDose} mSv", fontSize = 24.sp)
                        Row(
                            modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = when (dosageDtoData.value.hp10DoseValue < BigDecimal.valueOf(
                                    NfcConstant.MAX_MICRO_VALUE
                                )) {
                                    true -> dosageDtoData.value.hp10DoseValue.toString()
                                    false -> dosageDtoData.value.hp10DoseValue.divide(
                                        BigDecimal.valueOf(
                                            1000
                                        )
                                    ).toString()
                                },
                                fontSize = 20.sp,
                            )
                        }
                        Spacer(modifier = Modifier.weight(0.05f))
                        Row(modifier = Modifier.weight(1f)) {
                            Text(
                                when (dosageDtoData.value.hp10DoseValue < BigDecimal.valueOf(
                                    NfcConstant.MAX_MICRO_VALUE
                                )) {
                                    true -> MICRO_SIEVERT_SYMBOL
                                    false -> MILLI_SIEVERT_SYMBOL
                                }, fontSize = 20.sp, textAlign = TextAlign.End
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 温度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(3.0f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.temperature), fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Row(
                        modifier = Modifier.weight(1.0f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text("${data?.temperature} $DEGREE_CELSIUS")
                        Row(
                            Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                dosageDtoData.value.temperature.toString(),
                                fontSize = 20.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }

                        Spacer(modifier = Modifier.weight(0.05f))

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(DEGREE_CELSIUS_SYMBOL, fontSize = 20.sp)
                        }
                    }
                }

                // 電池電圧
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(3.0f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.battery_voltage), fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Row(
                        modifier = Modifier.weight(1.0f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text("${data?.batteryVoltage} V")
                        Row(Modifier.weight(1f)) {
                            Text(
                                dosageDtoData.value.batteryVol.toString(),
                                fontSize = 20.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.weight(0.05f))

                        Row(modifier = Modifier.weight(1f)) {
                            Text(VOLTAGE_SYMBOL, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}