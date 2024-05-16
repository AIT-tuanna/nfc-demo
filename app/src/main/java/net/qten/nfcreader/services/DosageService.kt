package net.qten.nfcreader.services

import android.icu.math.BigDecimal
import net.qten.nfcreader.dto.DosageDto
import java.nio.ByteOrder

interface DosageService {
    /**
     * convert data read from NFC to DosageDto
     */
    fun convertDosageData(data: ByteArray, logger: LoggerService, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): DosageDto
    /**
     * 機器ID
     */
    fun convertDeviceId(data: ByteArray): String
    /**
     * Hp10 BG値 & Hp0.07 BG値
     */
    fun convertHpBgValue(data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): BigDecimal
    /**
     * 線量測定データ保存ページ数
     */
    fun convertPageNumber(data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN): UInt
}