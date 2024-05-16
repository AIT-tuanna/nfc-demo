package net.qten.nfcreader.services

import android.icu.math.BigDecimal
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.dto.addErrorList
import java.math.RoundingMode
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DosageServiceImpl: DosageService {

    /**
     * convert data read from NFC memory to DosageDto
     */
    override fun convertDosageData(
        data: ByteArray, logger: LoggerService, byteOrder: ByteOrder
    ): DosageDto {
        val result = DosageDto()
        if (data.size >= 13) {
            result.absTime = convertAbsTime(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.ABS_TIME.start,
                    NfcConstant.DosageNfcMemoryAddress.ABS_TIME.end
                ), byteOrder
            )
            logger.logInfo(
                "absTime: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.ABS_TIME.start,
                    NfcConstant.DosageNfcMemoryAddress.ABS_TIME.end
                ).joinToString()
            )
            result.batteryVol = convertBatteryVoltage(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.BATTERY_VOL.start,
                    NfcConstant.DosageNfcMemoryAddress.BATTERY_VOL.end
                )
            )
            logger.logInfo(
                "batteryVol: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.BATTERY_VOL.start,
                    NfcConstant.DosageNfcMemoryAddress.BATTERY_VOL.end
                ).joinToString()
            )
            result.temperature = convertTemperature(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.TEMPERATURE.start,
                    NfcConstant.DosageNfcMemoryAddress.TEMPERATURE.end
                )
            )
            logger.logInfo(
                "temperature: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.TEMPERATURE.start,
                    NfcConstant.DosageNfcMemoryAddress.TEMPERATURE.end
                ).joinToString()
            )
            result.accumulatedCounter = convertAccumulatedCounter(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.ACCUMULATED_COUNTER.start,
                    NfcConstant.DosageNfcMemoryAddress.ACCUMULATED_COUNTER.end
                ), byteOrder
            )
            logger.logInfo(
                "accumulatedCounter: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.ACCUMULATED_COUNTER.start,
                    NfcConstant.DosageNfcMemoryAddress.ACCUMULATED_COUNTER.end
                ).joinToString()
            )
            result.hp10DoseValue = convertHpDosageValue(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.HP_10_DOSE_VALUE.start,
                    NfcConstant.DosageNfcMemoryAddress.HP_10_DOSE_VALUE.end
                ), byteOrder
            )
            logger.logInfo(
                "hp10DoseValue: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.HP_10_DOSE_VALUE.start,
                    NfcConstant.DosageNfcMemoryAddress.HP_10_DOSE_VALUE.end
                ).joinToString()
            )
            result.hp007DoseValue = convertHpDosageValue(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.HP_007_DOSE_VALUE.start,
                    NfcConstant.DosageNfcMemoryAddress.HP_007_DOSE_VALUE.end
                ), byteOrder
            )
            logger.logInfo(
                "hp007DoseValue: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.HP_007_DOSE_VALUE.start,
                    NfcConstant.DosageNfcMemoryAddress.HP_007_DOSE_VALUE.end
                ).joinToString()
            )
            val errorList = convertErrorValue(
                data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.ERROR.start,
                    NfcConstant.DosageNfcMemoryAddress.ERROR.end
                )
            )
            logger.logInfo(
                "errorList: " + data.copyOfRange(
                    NfcConstant.DosageNfcMemoryAddress.ERROR.start,
                    NfcConstant.DosageNfcMemoryAddress.ERROR.end
                ).joinToString()
            )
            result.addErrorList(errorList)
        }
        return result
    }

    /**
     * 機器ID
     */
    override fun convertDeviceId(data: ByteArray): String {

        return data.dropLastWhile { it == 0x00.toByte() }.toByteArray().reversedArray()
            .toString(Charsets.UTF_8).padStart(8, '0')
    }

    /**
     * Hp10 BG値 & Hp0.07 BG値
     */
    override fun convertHpBgValue(data: ByteArray, byteOrder: ByteOrder): BigDecimal {
        var returnData = BigDecimal(NfcConstant.INTEGER_ZERO)
        if (data.size >= 4) {
            val byte0 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[0] else data[3]
            val byte1 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[1] else data[2]
            val byte2 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[2] else data[1]
            val byte3 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[3] else data[0]
            val hpValue =
                (byte0.toInt() and 0xFF shl 24) or (byte1.toInt() and 0xFF shl 16) or (byte2.toInt() and 0xFF shl 8) or (byte3.toInt() and 0xFF)
            returnData = BigDecimal(hpValue.toLong()).divide(
                BigDecimal(100000),
                5,
                RoundingMode.FLOOR.ordinal
            )
        }
        return returnData
    }

    /**
     * 線量測定データ保存ページ数
     */
    override fun convertPageNumber(data: ByteArray, byteOrder: ByteOrder): UInt {
        if (data.size >= 4) {
            val byte0 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[0] else data[3]
            val byte1 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[1] else data[2]
            val byte2 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[2] else data[1]
            val byte3 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[3] else data[0]
            return 1u + (byte0.toUInt() and 0xFFu shl 24) or (byte1.toUInt() and 0xFFu shl 16) or (byte2.toUInt() and 0xFFu shl 8) or (byte3.toUInt() and 0xFFu)
        }
        return 0u
    }

    /**
     * 絶対時間
     */
    private fun convertAbsTime(data: ByteArray, byteOrder: ByteOrder): Date {
        val byte0 = (if (ByteOrder.LITTLE_ENDIAN == byteOrder) data[0] else data[3]).toUByte()
        val byte1 = (if (ByteOrder.LITTLE_ENDIAN == byteOrder) data[1] else data[2]).toUByte()
        val byte2 = (if (ByteOrder.LITTLE_ENDIAN == byteOrder) data[2] else data[1]).toUByte()
        val byte3 = (if (ByteOrder.LITTLE_ENDIAN == byteOrder) data[3] else data[0]).toUByte()
        var digit1: UInt = byte0.toUInt() and 0xFu
        val tmpMin: UInt = ((byte0.toUInt() and 0x7Fu) shr 4) * 10u + digit1
        digit1 = (byte0.toUInt() shr 7) or ((byte1.toUInt() and 7u) shl 1)
        val tmpHour: UInt = ((byte1.toUInt() and 0x18u) shr 3) * 10u + digit1
        digit1 = (((byte1.toUInt() shr 1) or ((byte2.toUInt() and 1u) shl 7)) shr 4)
        val tmpDay: UInt = ((byte2.toUInt() and 0x6u) shr 1) * 10u + digit1
        val tmpMonth: UInt = ((byte2.toUInt() shr 7) * 10u) + ((byte2.toUInt() and 0x78u) shr 3)
        val tmpYear: UInt = ((byte3.toUInt() shr 4) * 10u) + (byte3.toUInt() and 0xFu)
        val ret = tmpMonth.toString().padStart(2, '0') + "/" + tmpDay.toString()
            .padStart(2, '0') + "/20" + tmpYear.toString()
            .padStart(2, '0') + " " + tmpHour.toString().padStart(2, '0') + ":" + tmpMin.toString()
            .padStart(2, '0')
        val dateFormat = SimpleDateFormat(
            NfcConstant.DATE_CSV_FORMAT_TEMPLATE, Locale.getDefault(
                Locale.Category.FORMAT
            )
        )
        return dateFormat.parse(ret)!!
    }

    /**
     * 電池電圧
     */
    private fun convertTemperature(data: ByteArray): Int {
        var returnData = NfcConstant.INTEGER_ZERO
        if (data.isNotEmpty()) {
            returnData = (data[0].toInt() and 0xFF)
        }
        return returnData
    }

    /**
     * 温度
     */
    private fun convertBatteryVoltage(data: ByteArray): BigDecimal {
        var returnData = BigDecimal(NfcConstant.INTEGER_ZERO)
        if (data.isNotEmpty()) {
            returnData = BigDecimal(data[0].toInt() and 0xFF).divide(
                BigDecimal(10),
                1,
                RoundingMode.FLOOR.ordinal
            )
        }
        return returnData
    }

    /**
     * 積算カウンタ
     */
    private fun convertAccumulatedCounter(data: ByteArray, byteOrder: ByteOrder): UInt {
        var returnData = NfcConstant.UINT_ZERO
        if (data.size >= 2) {
            val byte0 = (if (ByteOrder.BIG_ENDIAN == byteOrder) data[0] else data[1]).toUByte()
            val byte1 = (if (ByteOrder.BIG_ENDIAN == byteOrder) data[1] else data[0]).toUByte()
            returnData = (byte0.toUInt() and 0xFFu shl 8) or (byte1.toUInt() and 0xFFu)
        }
        return returnData
    }

    /**
     * Hp10線量値 & Hp0.07線線量値
     */
    private fun convertHpDosageValue(data: ByteArray, byteOrder: ByteOrder): BigDecimal {
        var returnData = BigDecimal(NfcConstant.INTEGER_ZERO)
        if (data.size >= 2) {
            val byte0 = (if (ByteOrder.BIG_ENDIAN == byteOrder) data[0] else data[1]).toUByte()
            val byte1 = (if (ByteOrder.BIG_ENDIAN == byteOrder) data[1] else data[0]).toUByte()
            val hpValue = (getRemainingBits(byte0) and 0xFFu shl 8) or (byte1.toUInt() and 0xFFu)
            returnData = if (isMsv(byte0)) {
                BigDecimal(hpValue.toLong()).divide(
                    BigDecimal(20),
                    2,
                    RoundingMode.FLOOR.ordinal
                ).multiply(BigDecimal(1000))
            } else {
                BigDecimal(hpValue.toLong()).divide(
                    BigDecimal(20),
                    2,
                    RoundingMode.FLOOR.ordinal
                )
            }
        }

        return returnData
    }

    /**
     * エラーフラグ
     */
    private fun convertErrorValue(data: ByteArray): List<Boolean> {
        val returnData = mutableListOf<Boolean>()
        if (data.isNotEmpty()) {
            for (i in 0..7) {
                val bit = (data[0].toInt() shr (7 - i)) and 1
                returnData.add(bit != 0)
            }
        }
        return returnData
    }

    /**
     * uSvかmSvかを表す
     * @return true if mSv
     */
    private fun isMsv(data: UByte): Boolean {
        // Shift right by 7 to get the first bit and perform a bitwise AND with 1
        return ((data.toInt() shr 7) and 1) == 1
    }

    /**
     * Get the remaining 7 bits after excluding the first bit
     */
    private fun getRemainingBits(data: UByte): UInt {
        // Use 0x7F (0111 1111 in binary) to mask the first bit and keep the remaining 7 bits
        return data.toUInt() and 0x7Fu
    }
}