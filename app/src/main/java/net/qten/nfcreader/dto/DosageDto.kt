package net.qten.nfcreader.dto

import android.icu.math.BigDecimal
import androidx.compose.runtime.Immutable
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.constant.NfcConstant.DATE_CSV_FORMAT_TEMPLATE
import net.qten.nfcreader.utils.Utils
import net.qten.nfcreader.utils.toFormattedString
import net.qten.nfcreader.utils.toInt
import java.util.Date

@Immutable
data class DosageDto(
    var absTime: Date = Date(),
    var batteryVol: BigDecimal = BigDecimal(NfcConstant.INTEGER_ZERO),
    var temperature: Int = NfcConstant.INTEGER_ZERO,
    var accumulatedCounter: UInt = NfcConstant.UINT_ZERO,
    var hp10DoseValue: BigDecimal = BigDecimal(NfcConstant.INTEGER_ZERO),
    var hp007DoseValue: BigDecimal = BigDecimal(NfcConstant.INTEGER_ZERO),
    var bgCorrectionError: Boolean = false,
    var cpuError: Boolean = false,
    var temperatureError: Boolean = false,
    var batteryVoltageError: Boolean = false,
    var betaSensorError: Boolean = false,
    var gammaSensorError: Boolean = false,
    var unequipped: Boolean = false,
    var impact: Boolean = false,
    /** Id of the read row, format YYYYMMDDHHmmss<device_id> */
    val id: String = NfcConstant.EMPTY_STRING,
    /** Serial number */
    val serialNumber: String = NfcConstant.EMPTY_STRING
)

fun DosageDto.toSeparatedString(separator: String = ","): String {
    return listOf(
        Utils.dateToString(absTime, DATE_CSV_FORMAT_TEMPLATE),
        batteryVol,
        temperature,
        accumulatedCounter,
        hp10DoseValue.toFormattedString(),
        hp007DoseValue.toFormattedString(),
        bgCorrectionError.toInt(),
        cpuError.toInt(),
        temperatureError.toInt(),
        batteryVoltageError.toInt(),
        betaSensorError.toInt(),
        gammaSensorError.toInt(),
        unequipped.toInt(),
        impact.toInt()
    ).joinToString(separator)
}

fun DosageDto.addErrorList(errorLst: List<Boolean>) {
    this.bgCorrectionError = errorLst[0]
    this.cpuError = errorLst[1]
    this.temperatureError = errorLst[2]
    this.batteryVoltageError = errorLst[3]
    this.betaSensorError = errorLst[4]
    this.gammaSensorError = errorLst[5]
    this.unequipped = errorLst[6]
    this.impact = errorLst[7]
}