package net.qten.nfcreader.dto

import android.icu.math.BigDecimal
import net.qten.nfcreader.constant.NfcConstant

data class DosageDeviceDto(
    val deviceId: String = NfcConstant.EMPTY_STRING,
    val hp10BgValue: BigDecimal = BigDecimal(NfcConstant.INTEGER_ZERO),
    val hp007BgValue: BigDecimal = BigDecimal(NfcConstant.INTEGER_ZERO),
    val dosageDtoList: MutableList<DosageDto> = mutableListOf()
)
