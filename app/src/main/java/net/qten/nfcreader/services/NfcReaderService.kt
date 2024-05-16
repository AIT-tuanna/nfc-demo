package net.qten.nfcreader.services

import android.nfc.Tag
import net.qten.nfcreader.dto.DosageDeviceDto

interface NfcReaderService {
    /**
     * 本体メモリ読取
     */
    suspend fun readDataFromDeviceMemory(tag: Tag, logger: LoggerService): DosageDeviceDto

    /**
     * NFCメモリ読取
     */
    suspend fun readDataFromNfcMemory(tag: Tag, logger: LoggerService): DosageDeviceDto
}