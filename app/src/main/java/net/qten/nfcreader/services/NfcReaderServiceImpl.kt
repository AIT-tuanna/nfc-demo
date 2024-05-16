package net.qten.nfcreader.services

import android.content.Context
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcA
import android.os.Environment
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.dto.DosageDeviceDto
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.dto.toSeparatedString
import net.qten.nfcreader.exception.NfcStatusException
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteOrder
import kotlin.random.Random
import kotlin.random.nextUInt

class NfcReaderServiceImpl(context: Context) : NfcReaderService {
    private val dosageService = DosageServiceImpl()
    private var context: Context

    init {
        this.context = context
    }

    /**
     * 本体メモリ読取
     */
    override suspend fun readDataFromDeviceMemory(tag: Tag, logger: LoggerService): DosageDeviceDto {
        logger.logInfo("readDataFromDeviceMemory")
        val nfcA = NfcA.get(tag) ?: throw NfcStatusException(NfcConstant.EMPTY_STRING)
        try {
            // Connect to the tag
            nfcA.connect()
            logger.logInfo("readDataFromDeviceMemory connected")
            val deviceId = getDeviceIdFromMcu(logger, nfcA)
            logger.logInfo("deviceId: $deviceId")

            // 線量測定データ保存ページ数
            val pageNumberMcuCommand = createMcuCommand(createScdForPageNumberCommand())
            val pageNumber = dosageService.convertPageNumber(
                readFromDevice(
                    pageNumberMcuCommand, logger, nfcA
                )
            )
            logger.logInfo("pageNumber: $pageNumber")
            // 線量測定データ読み出し : Hp10 BG値
            val hp10BgValueMcuCommand = createMcuCommand(createScdForHp10BgCommand())
            val hp10BgValue = dosageService.convertHpBgValue(
                readFromDevice(
                    hp10BgValueMcuCommand, logger, nfcA
                )
            )
            logger.logInfo("hp10BgValue: $hp10BgValue")
            // 線量測定データ読み出し : Hp0.07 BG値
            val hp007BgValueMcuCommand = createMcuCommand(createScdForHp007BgCommand())
            val hp007BgValue = dosageService.convertHpBgValue(
                readFromDevice(
                    hp007BgValueMcuCommand, logger, nfcA
                )
            )
            logger.logInfo("hp007BgValue: $hp007BgValue")
            // 線量測定データ転送開始 : トレンドデータ
            var dosageByteArray = ByteArray(0)
            for (pageNo in 0u until pageNumber) {
                for (dataNo in 0u until 16u) {
                    val dosageDataMcuCommand =
                        createMcuCommand(createScdForDosageValueCommand(pageNo, dataNo))
                    dosageByteArray += readFromDevice(
                        dosageDataMcuCommand, logger, nfcA
                    )
                }
            }

            val dosageDtoList = mutableListOf<DosageDto>()
            var address = 0

            while ((address + NfcConstant.NUMBER_OF_BYTES_PER_DOSAGE_DATA) <= dosageByteArray.size) {
                // convert byte array to dosage data, each data has 13 bytes
                val response = dosageByteArray.copyOfRange(
                    address, address + NfcConstant.NUMBER_OF_BYTES_PER_DOSAGE_DATA
                )
                if (ByteArray(NfcConstant.NUMBER_OF_BYTES_PER_DOSAGE_DATA) { 0xFF.toByte() }
                        .contentEquals(response)) {
                    break
                }
                logger.logInfo("rawData: ${dosageByteArray.joinToString()}")
                val dosage = dosageService.convertDosageData(response, logger)
                logger.logInfo("formattedData: ${dosage.toSeparatedString()}")
                dosageDtoList.add(dosage)
                address += NfcConstant.NUMBER_OF_BYTES_PER_DOSAGE_DATA
            }

            return DosageDeviceDto(
                deviceId = deviceId,
                hp10BgValue = hp10BgValue,
                hp007BgValue = hp007BgValue,
                dosageDtoList = dosageDtoList
            )
        } catch (e: IOException) {
            // Handle communication error
            logger.logWarning(e.stackTraceToString())
            throw NfcStatusException(NfcConstant.EMPTY_STRING)
        } finally {
            // Ensure the connection is closed
            if (nfcA.isConnected) {
                nfcA.close()
            }
        }
    }

    /**
     * NFCメモリ読取
     */
    override suspend fun readDataFromNfcMemory(tag: Tag, logger: LoggerService): DosageDeviceDto {
        val nfcA: NfcA = NfcA.get(tag)
        logger.logInfo("readDataFromNfcMemory get tag successful")
        val dosageDtoList = mutableListOf<DosageDto>()
        try {
            nfcA.connect()
            logger.logInfo("readDataFromNfcMemory connect successful")
            dosageDtoList.addAll(
                readDosageFromNfcMemory(
                    nfcA,
                    0x00.toByte(),
                    NfcConstant.NfcMemoryAddress.SECTOR_0.start,
                    NfcConstant.NfcMemoryAddress.SECTOR_0.end,
                    logger
                )
            )
            dosageDtoList.addAll(
                readDosageFromNfcMemory(
                    nfcA,
                    0x01.toByte(),
                    NfcConstant.NfcMemoryAddress.SECTOR_1.start,
                    NfcConstant.NfcMemoryAddress.SECTOR_1.end,
                    logger
                )
            )

            val deviceId = getDeviceIdFromMcu(logger, nfcA)

            return DosageDeviceDto(dosageDtoList = dosageDtoList, deviceId = deviceId)
        } catch (e: IOException) {
            // Handle the IO exception
            logger.logWarning(e.stackTraceToString())
            throw NfcStatusException(NfcConstant.EMPTY_STRING)
        } finally {
            if (nfcA.isConnected) {
                nfcA.close()
            }
        }
    }

    /**
     * 機器ID
     */
    private fun getDeviceIdFromMcu(logger: LoggerService, nfcA: NfcA): String {
        // 機器ID
        val deviceIdMcuCommand = createMcuCommand(createScdForDeviceIdCommand())
        return dosageService.convertDeviceId(
            readFromDevice(
                deviceIdMcuCommand, logger, nfcA
            )
        )
    }

    /**
     * command for 機器ID
     */
    private fun createScdForDeviceIdCommand(): ByteArray {
        return byteArrayOf(
            NfcConstant.APDUDataFormat.DEVICE_ID.offset2.toByte(),
            NfcConstant.APDUDataFormat.DEVICE_ID.offset1.toByte(),
            NfcConstant.APDUDataFormat.DEVICE_ID.category.toByte(),
            *getAddressHeaderByteArray(),
            0x00u.toByte(),
            0x00u.toByte(),
            0x04u.toByte(),
            0x00u.toByte()
        )
    }

    /**
     * command for 線量測定データ保存ページ数
     */
    private fun createScdForPageNumberCommand(): ByteArray {
        return byteArrayOf(
            NfcConstant.APDUDataFormat.PAGE_NUMBER.offset2.toByte(),
            NfcConstant.APDUDataFormat.PAGE_NUMBER.offset1.toByte(),
            NfcConstant.APDUDataFormat.PAGE_NUMBER.category.toByte(),
            *getAddressHeaderByteArray(),
            0x00u.toByte(),
            0x00u.toByte(),
            0x04u.toByte(),
            0x00u.toByte()
        )
    }

    /**
     * command for 線量測定データ読み出し : Hp10 BG値
     */
    private fun createScdForHp10BgCommand(): ByteArray {
        return byteArrayOf(
            NfcConstant.APDUDataFormat.HP_10_BG_VALUE.offset2.toByte(),
            NfcConstant.APDUDataFormat.HP_10_BG_VALUE.offset1.toByte(),
            NfcConstant.APDUDataFormat.HP_10_BG_VALUE.category.toByte(),
            *getAddressHeaderByteArray(),
            0x00u.toByte(),
            0x00u.toByte(),
            0x04u.toByte(),
            0x00u.toByte()
        )
    }

    /**
     * command for 線量測定データ読み出し : Hp0.07 BG値
     */
    private fun createScdForHp007BgCommand(): ByteArray {
        return byteArrayOf(
            NfcConstant.APDUDataFormat.HP_007_BG_VALUE.offset2.toByte(),
            NfcConstant.APDUDataFormat.HP_007_BG_VALUE.offset1.toByte(),
            NfcConstant.APDUDataFormat.HP_007_BG_VALUE.category.toByte(),
            *getAddressHeaderByteArray(),
            0x00u.toByte(),
            0x00u.toByte(),
            0x04u.toByte(),
            0x00u.toByte()
        )
    }

    /**
     * command for 線量測定データ転送開始 : トレンドデータ
     */
    private fun createScdForDosageValueCommand(pageNumber: UInt, dataNo: UInt): ByteArray {
        // 上位12bit：指定ページデータ(0d0~0d1014)
        // 下位4bit：データ番号(0d0~0d15)
        val reserve = ((pageNumber shl 4) or (dataNo and 0x0Fu))
        return byteArrayOf(
            NfcConstant.APDUDataFormat.DOSAGE_DATA.offset2.toByte(),
            NfcConstant.APDUDataFormat.DOSAGE_DATA.offset1.toByte(),
            NfcConstant.APDUDataFormat.DOSAGE_DATA.category.toByte(),
            *getAddressHeaderByteArray(),
            (reserve and 0xFFu).toByte(),
            (reserve shr 8).toByte(),
            0x20u.toByte(),
            0x00u.toByte()
        )
    }

    /**
     * create MCU command
     */
    private fun createMcuCommand(scd: ByteArray): ByteArray {
        val ccd = createCcdData()
        return byteArrayOf(
            0x00u.toByte(),
            0x01u.toByte(),
            0x00u.toByte(),
            0x00u.toByte(),
            *computeCRC32MPEG2(ccd).toByteArrayLittleEndian(),
            *computeCRC32MPEG2(scd).toByteArrayLittleEndian(),
            *ccd,
            *scd
        )
    }

    /**
     * create Common Command Data
     */
    private fun createCcdData(): ByteArray {
        val random = Random.nextUInt(from = 0x0000u, until = 0x10000u)
        return byteArrayOf(
            0x00u.toByte(),
            0x40u.toByte(),
            0x00u.toByte(),
            0x08u.toByte(),
            0x0Cu.toByte(),
            0x00u.toByte(),
            (random and 0xFFu).toByte(),
            (random shr 8).toByte()
        )
    }

    /**
     * create byte array for Address Header: 0xF2FFFF0000
     */
    private fun getAddressHeaderByteArray(): ByteArray {
        return byteArrayOf(
            0xF2u.toByte(),
            0xFFu.toByte(),
            0xFFu.toByte(),
            0x00u.toByte(),
            0x00u.toByte()
        )
    }

    /**
     * convert data to CRC32 by MPEG2
     */
    private fun computeCRC32MPEG2(input: ByteArray): UInt {
        var crc = 0xFFFFFFFFu
        for (byte in input) {
            var temp = (byte.toUInt() shl 24) and 0xFF000000u
            for (i in 0 until 8) {
                crc = if ((crc xor temp) and 0x80000000u != 0u) {
                    (crc shl 1) xor 0x04C11DB7u
                } else {
                    crc shl 1
                }
                temp = temp shl 1
            }
        }
        return crc
    }

    /**
     * Read data from MCU
     */
    private fun readFromDevice(
        mcuReadCommand: ByteArray, logger: LoggerService, nfcA: NfcA
    ): ByteArray {
        var message = NfcConstant.EMPTY_STRING
        for (j in 0 until 4) {
            try {
                selectSector(nfcA, logger, 0x00.toByte())

                // Delay a bit for the tag to respond in time
                Thread.sleep(100)
                var startTime = System.currentTimeMillis()
                // Check NC_REG:PTHRU_ON, NC_REG:TRANSFER_DIR and NS_REG: Memory access
                checkSessionByteBeforeWrite(nfcA, logger, startTime)

                // Delay a bit for the tag to respond in time
                Thread.sleep(100)
                try {
                    // Write command to 240 ~ 255
                    val writeCommand = byteArrayOf(
                        NfcConstant.NFC_FAST_WRITE_COMMAND.toByte(),
                        0xF0.toByte(),
                        0xFF.toByte(),
                        *mcuReadCommand,
                        *ByteArray(32) { 0 }
                    )
                    logger.logInfo("write command : ${writeCommand.joinToString()}")
                    val writeResponse = nfcA.transceive(writeCommand)
                    logger.logInfo("write command response: ${writeResponse.joinToString()}")
                } catch (e: TagLostException) {
                    // It's normal for the second part of the command to cause a TagLostException
                    // You can safely continue to communicate with the tag after this exception
                    logger.logWarning(e.stackTraceToString())
                }
                logger.logInfo("write command successful")

                if (nfcA.isConnected) {
                    nfcA.close()
                }
                // Delay a bit for the tag to respond in time
                Thread.sleep(100)
                nfcA.connect()
                selectSector(nfcA, logger, 0x00.toByte())
                startTime = System.currentTimeMillis()
                // Check NS_REG: Memory access and NS_REG: data is ready to be read by NFC
                checkSessionByteBeforeRead(nfcA, logger, startTime)

                // Delay a bit for the tag to respond in time
                Thread.sleep(100)
                // Read data from 240 ~ 255
                val readCommand = byteArrayOf(
                    NfcConstant.NFC_FAST_READ_COMMAND.toByte(), 0xF0.toByte(), 0xFF.toByte()
                )
                val readResponse = nfcA.transceive(readCommand)
                logger.logInfo("read data response: ${readResponse.joinToString()}")

                val responseLength =
                    (readResponse[0x0011].toInt() and 0xFF shl 8) or (readResponse[0x0010].toInt() and 0xFF)
                logger.logInfo("responseLength: $responseLength")
                checkCRC32(readResponse, logger, responseLength)

                // check status code
                val statusCode = readResponse.copyOfRange(0x000C, 0x000E)
                val statusType = (statusCode[1].toInt() shr 6) and 0b11
                if (statusType != 0b00 && statusType != 0b01) {
                    logger.logInfo("status code: ${statusCode.joinToString()}")
                    // neu status code khac success hoac warning thi luu lai message
                    val errorType =
                        ((statusCode[1].toInt() and 0b00000011) shl 2) or ((statusCode[0].toInt() shr 6) and 0b11)
                    val errorCode = statusCode[0].toInt() and 0b00111111
                    message = "0b${errorType.toString(2).padStart(4, '0')} - 0b${
                        errorCode.toString(2).padStart(6, '0')
                    }"
                    logger.logInfo("error code: $message")
                    continue
                }
                logger.logInfo(
                    "read data successful: ${
                        readResponse.copyOfRange(
                            0x0014,
                            0x0014 + responseLength
                        ).joinToString()
                    }"
                )

                return readResponse.copyOfRange(0x0014, 0x0014 + responseLength)
            } catch (e: IOException) {
                // Handle the IO exception
                logger.logWarning(e.stackTraceToString())
            }
        }
        // neu sau khi gui 4 lan van chua thanh cong thi se throw loi
        throw NfcStatusException(message)
    }

    /**
     * Check sum CRC32MPEG2 for ccd and scd
     */
    private fun checkCRC32(
        readResponse: ByteArray,
        logger: LoggerService,
        responseLength: Int
    ) {
        val ccd = readResponse.copyOfRange(0x000C, 0x0014)
        val scd = readResponse.copyOfRange(0x0014, 0x0014 + responseLength)
        val crcCcd = readResponse.copyOfRange(0x0004, 0x008)
        val crcScd = readResponse.copyOfRange(0x0008, 0x00C)
        if (computeCRC32MPEG2(ccd) != convertToUInt(crcCcd)) {
            logger.logInfo("check ccd crc32 error: ${ccd.joinToString()} , ${crcCcd.joinToString()}")
            //                    throw NfcStatusException(NfcConstant.EMPTY_STRING)
        }
        if (computeCRC32MPEG2(scd) != convertToUInt(crcScd)) {
            logger.logInfo("check scd crc32 error: ${scd.joinToString()} , ${crcScd.joinToString()}")
            //                    throw NfcStatusException(NfcConstant.EMPTY_STRING)
        }
    }

    /**
     * Check NS_REG: Memory access is locked to the NFC interface
     * and NS_REG: data is ready in SRAM buffer to be read by NFC
     */
    private fun checkSessionByteBeforeRead(
        nfcA: NfcA,
        logger: LoggerService,
        startTime: Long
    ) {
        do {
            try {
                // Get NS_REG for read command
                val sessionCommand2 = byteArrayOf(
                    NfcConstant.NFC_READ_COMMAND.toByte(), 236.toByte()
                )
                val sessionResponse2 = nfcA.transceive(sessionCommand2)
                logger.logInfo("Get NS_REG successful:${sessionResponse2.joinToString()}")
                // Check NS_REG: Memory access is locked to the NFC interface
                // and NS_REG: data is ready in SRAM buffer to be read by NFC
                if (isPassThNfcReadReady(sessionResponse2)
                    && isPassThNfcLocked(sessionResponse2)
                ) {
                    logger.logInfo("Check NS_REG successful")
                    break
                }
            } catch (e: TagLostException) {
                // It's normal for the second part of the command to cause a TagLostException
                // You can safely continue to communicate with the tag after this exception
                logger.logWarning(e.stackTraceToString())
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                throw NfcStatusException("time out")
            }
            // Delay a bit for the tag to respond in time
            Thread.sleep(10)
        } while (true)
    }

    /**
     * Check NC_REG:PTHRU_ON, NC_REG:TRANSFER_DIR from NFC to I2C interface
     * and NS_REG: Memory access is locked to the NFC interface
     */
    private fun checkSessionByteBeforeWrite(
        nfcA: NfcA,
        logger: LoggerService,
        startTime: Long
    ) {
        do {
            try {
                // Get NC_REG and NS_REG for write command
                val getSessionCommand1 = byteArrayOf(
                    NfcConstant.NFC_READ_COMMAND.toByte(), 236.toByte()
                )
                val sessionResponse1 = nfcA.transceive(getSessionCommand1)
                logger.logInfo("Get NC_REG and NS_REG successful:${sessionResponse1.joinToString()}")
                // Check NC_REG:PTHRU_ON, NC_REG:TRANSFER_DIR from NFC to I2C interface
                // and NS_REG: Memory access is locked to the NFC interface
                if (isPassThroughEnabled(sessionResponse1)
                    && isPassThTransferDir(sessionResponse1)
                    && isPassThNfcLocked(sessionResponse1)
                ) {
                    logger.logInfo("Check NC_REG and NS_REG successful")
                    break
                }
            } catch (e: TagLostException) {
                // It's normal for the second part of the command to cause a TagLostException
                // You can safely continue to communicate with the tag after this exception
                logger.logWarning(e.stackTraceToString())
            }
            if (System.currentTimeMillis() - startTime > 5000) {
                throw NfcStatusException("time out")
            }
            // Delay a bit for the tag to respond in time
            Thread.sleep(10)
        } while (true)
    }

    /**
     * Read data from NFC Tag using NFC-A
     */
    private fun readDosageFromNfcMemory(
        nfcA: NfcA, sector: Byte, startAddress: Int, endAddress: Int, logger: LoggerService
    ): MutableList<DosageDto> {
        for (j in 0 until 4) {
            val dosageDtoList = mutableListOf<DosageDto>()
            try {
                selectSector(nfcA, logger, sector)

                var address = startAddress
                do {
                    // Command to read from block $address to $address + 4
                    val command = byteArrayOf(
                        NfcConstant.NFC_READ_COMMAND.toByte(), address.toByte()
                    )
                    val response = nfcA.transceive(command)
                    writeRawData(
                        context, response, "$sector-0x${address.toByte().toHexString()}-${
                            (address + 3).toByte().toHexString()
                        }.dat", logger
                    )
                    logger.logInfo("rawData: ${response.joinToString()}")
                    val dosage = dosageService.convertDosageData(response, logger)
                    logger.logInfo("formattedData: ${dosage.toSeparatedString()}")
                    dosageDtoList.add(dosage)
                    address += 4
                } while (address <= endAddress)

                return dosageDtoList
            } catch (e: IOException) {
                // Handle the IO exception
                logger.logWarning(e.stackTraceToString())
            }
        }
        throw NfcStatusException(NfcConstant.EMPTY_STRING)
    }

    private fun selectSector(
        nfcA: NfcA,
        logger: LoggerService,
        sector: Byte
    ) {
        // First part of the SECTOR SELECT command
        val sectorSelectCmdPacket1 =
            byteArrayOf(NfcConstant.NFC_SECTOR_SELECT_COMMAND.toByte(), 0xFF.toByte())
        // Send the first part of the command
        val response1 = nfcA.transceive(sectorSelectCmdPacket1)
        logger.logInfo("selectSector first command successful:${response1.joinToString()}")
        // Delay a bit for the tag to respond in time
        Thread.sleep(100)

        // Second part of the SECTOR SELECT command, with the sector number
        val sectorSelectCmdPacket2 =
            byteArrayOf(sector, 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        // Send the second part of the command
        try {
            val response2 = nfcA.transceive(sectorSelectCmdPacket2)
            logger.logInfo("selectSector second command successful:${response2.joinToString()}")
        } catch (e: TagLostException) {
            // It's normal for the second part of the command to cause a TagLostException
            // You can safely continue to communicate with the tag after this exception
            logger.logWarning(e.stackTraceToString())
        }
    }

    /**
     * convert byte array to hexadecimal format 0x00
     */
    private fun ByteArray.toHexString(): String {
        return "0x" + joinToString("") { "%02x".format(it) }
    }

    /**
     * convert byte to hexadecimal format 0x00
     */
    private fun Byte.toHexString(): String {
        return String.format("%02x", this)
    }

    /**
     * export raw data for each read command
     */
    private fun writeRawData(
        context: Context, data: ByteArray, fileName: String, logger: LoggerService
    ) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            try {
                val csvFile = File(context.getExternalFilesDir(null), fileName)
                logger.logInfo("Get file ${csvFile.absolutePath} successful")

                if (!csvFile.exists()) {
                    csvFile.createNewFile()
                }
                logger.logInfo("Read file ${csvFile.absolutePath} successful")

                BufferedWriter(FileWriter(csvFile, true)).use { buf ->
                    logger.logInfo("Write csv header successful")
                    buf.append(data.toHexString())
                    buf.newLine()
                }
                logger.logInfo("Write dat file fileName successful")
            } catch (e: IOException) {
                logger.logWarning(e.stackTraceToString())
            }
        }
    }

    /**
     * convert byte array to unsigned int
     */
    private fun convertToUInt(
        data: ByteArray, byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN
    ): UInt {
        if (data.size >= 4) {
            val byte0 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[0] else data[3]
            val byte1 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[1] else data[2]
            val byte2 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[2] else data[1]
            val byte3 = if (ByteOrder.BIG_ENDIAN == byteOrder) data[3] else data[0]
            return (byte0.toUInt() and 0xFFu shl 24) or (byte1.toUInt() and 0xFFu shl 16) or (byte2.toUInt() and 0xFFu shl 8) or (byte3.toUInt() and 0xFFu)
        }
        return 0u
    }

    private fun UInt.toByteArrayLittleEndian(): ByteArray {
        return byteArrayOf(
            (this and 0xFFu).toByte(),
            (this shr 8 and 0xFFu).toByte(),
            (this shr 16 and 0xFFu).toByte(),
            (this shr 24 and 0xFFu).toByte()
        )
    }

    private fun isPassThroughEnabled(data: ByteArray): Boolean {
        return data.isNotEmpty() && ((data[0].toInt() shr 6 and 1) == 1)
    }

    private fun isPassThTransferDir(data: ByteArray): Boolean {
        return data.isNotEmpty() && ((data[0].toInt() and 1) == 1)
    }

    private fun isPassThNfcLocked(data: ByteArray): Boolean {
        return data.size > 7 && ((data[6].toInt() shr 5 and 1) == 1)
    }

    private fun isPassThNfcReadReady(data: ByteArray): Boolean {
        return data.size > 7 && ((data[6].toInt() shr 3 and 1) == 1)
    }
}