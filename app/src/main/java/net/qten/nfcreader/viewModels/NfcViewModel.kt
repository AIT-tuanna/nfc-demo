package net.qten.nfcreader.viewModels

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qten.nfcreader.R
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.dto.DosageDeviceDto
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.dto.toSeparatedString
import net.qten.nfcreader.exception.NfcStatusException
import net.qten.nfcreader.pages.dataExtractMenu.createDosageDtoFromData
import net.qten.nfcreader.services.LoggerService
import net.qten.nfcreader.services.NfcReaderService
import net.qten.nfcreader.utils.Utils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.system.measureTimeMillis

class NfcViewModel : ViewModel() {
    private val _loading by lazy {
        MutableStateFlow(false)
    }

    val loading = _loading.asStateFlow()

    suspend fun readNfc(
        nfcReaderViewModel: NfcReaderViewModel,
        intent: Intent,
        context: Context,
        logger: LoggerService,
        nfcReaderService: NfcReaderService
    ) {
        viewModelScope.launch {
            if (nfcReaderViewModel.isNfcEnabled.value) {
                try {
                    _loading.emit(true)
                    var dosageDeviceDto: DosageDeviceDto?
                    val elapsedTime = measureTimeMillis {
                        dosageDeviceDto =
                            readFromIntent(intent, logger, nfcReaderViewModel, nfcReaderService)
                    }
                    if (dosageDeviceDto == null) {
                        Toast.makeText(context, "NFC Tag UUID is not 04h", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    buildSuccessTagViews(
                        elapsedTime,
                        nfcReaderViewModel,
                        context
                    )
                    writeToCsv(context, dosageDeviceDto!!, nfcReaderViewModel, logger)
                } catch (e: NfcStatusException) {
                    e.message?.let {
                        buildErrorTagViews(it, nfcReaderViewModel, context)
                    }
                } finally {
                    _loading.emit(false)
                }
            }
        }
    }

    /**
     * write data to CSV
     */
    private suspend fun writeToCsv(
        context: Context,
        data: DosageDeviceDto,
        nfcReaderViewModel: NfcReaderViewModel,
        logger: LoggerService
    ) {
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            try {
                val fileName = if (NfcConstant.ReaderMode.NFC.mode == nfcReaderViewModel.mode) {
                    NfcConstant.NFC_DATA_CSV_FILENAME.format(
                        data.deviceId.padStart(16, '0')
                    )
                } else {
                    NfcConstant.DEVICE_MEMORY_DATA_CSV_FILENAME.format(
                        data.deviceId.padStart(16, '0')
                    )
                }
                val csvFile = File(context.getExternalFilesDir(null), fileName)
                logger.logInfo("Get file ${csvFile.absolutePath} successful")

                val oldList = mutableListOf<DosageDto>()
                var headerString = NfcConstant.EMPTY_STRING
                val editIndexList = mutableMapOf<Int, DosageDto>()
                if (csvFile.exists()) {
                    // read old data from csv
                    var oldData: List<List<String>> = mutableListOf()
                    if (csvFile.exists() && csvFile.length() != 0L) {
                        val fileStream = csvFile.inputStream()
                        oldData = Utils.extractCsvData(fileStream)
                    }
                    /** Serial Number was at position 0 */
                    if (oldData.isNotEmpty()) {
                        val serialNumber = oldData[0][0]
                        oldList.addAll(oldData.drop(1).map { dataRow ->
                            createDosageDtoFromData(dataRow, serialNumber)
                        })
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        csvFile.createNewFile()
                    }
                    headerString = getHeaderString()
                }
                logger.logInfo("Read file ${csvFile.absolutePath} successful")

                withContext(Dispatchers.IO) {
                    BufferedWriter(FileWriter(csvFile, true)).use { buf ->
                        writeHeader(headerString, buf, data, nfcReaderViewModel)
                        logger.logInfo("Write csv header successful")
                        data.dosageDtoList.forEach {
                            val oldDataIndex = oldList.indexOfFirst { dto ->
                                val format = SimpleDateFormat(
                                    "yyyyMMddHHmm", Locale.getDefault(Locale.Category.FORMAT)
                                )
                                format.format(dto.absTime) == format.format(it.absTime)
                            }
                            if (oldDataIndex != -1) {
                                editIndexList[oldDataIndex] = it
                            } else {
                                buf.newLine()
                                buf.append(it.toSeparatedString())
                            }
                        }
                    }
                }

                for (index in editIndexList) {
                    updateCsvLine(csvFile, index.key + 6, index.value.toSeparatedString())
                }
                logger.logInfo("Write csv successful")
            } catch (e: IOException) {
                logger.logWarning(e.stackTraceToString())
            }
        }
    }

    /**
     * read data from NFC
     */
    private suspend fun readFromIntent(
        intent: Intent,
        logger: LoggerService,
        nfcReaderViewModel: NfcReaderViewModel,
        nfcReaderService: NfcReaderService
    ): DosageDeviceDto? {
        var tag: Tag? = null
        logger.logInfo("readFromIntent start")
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            // using this deprecated method because handy using API 29 (android 10)
            tag = withContext(this.viewModelScope.coroutineContext + Dispatchers.IO) {
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) as Tag
            }
        }
        if (tag != null) {
            logger.logInfo("techList: ${tag.techList.asList().fastJoinToString()}")
            logger.logInfo("uuid: ${tag.id[0]}")
            logger.logInfo("readFromIntent get tag successful")
            return if (0x04.toByte() != tag.id[0]) {
                null
            } else if (NfcConstant.ReaderMode.NFC.mode == nfcReaderViewModel.mode) {
                nfcReaderService.readDataFromNfcMemory(tag, logger)
            } else {
                nfcReaderService.readDataFromDeviceMemory(tag, logger)
            }
        } else {
            return null
        }
    }

    /**
     * display reading success with process time
     */
    private fun buildSuccessTagViews(
        processTime: Long, nfcReaderViewModel: NfcReaderViewModel, context: Context
    ) {
        nfcReaderViewModel.notification.value = buildString {
            append(context.getString(R.string.success_message, (processTime / 1000.0)))
        }
        nfcReaderViewModel.isError.value = false
    }

    /**
     * display reading fail with error code
     */
    private fun buildErrorTagViews(
        errorCode: String, nfcReaderViewModel: NfcReaderViewModel, context: Context
    ) {
        nfcReaderViewModel.notification.value = buildString {
            append(context.getString(R.string.error_message, errorCode))
        }
        nfcReaderViewModel.isError.value = true
    }

    /**
     * create header string for csv
     */
    private fun getHeaderString(): String {
        return listOf(
            "Absolute Time",
            "Battery Voltage [V]",
            "Temperature [℃]",
            "Accumulated Counter",
            "Hp10 Dose value (every variable time) [uSv]",
            "Hp0.07 Dose value (every variable time) [uSv]",
            "BG correction Error",
            "CPU Error",
            "Temperature Error",
            "Battery voltage Error",
            "β sensor Error",
            "γ sensor Error",
            "Unequipped",
            "Impact"
        ).joinToString()
    }

    /**
     * update 1 line of csv
     */
    private fun updateCsvLine(file: File, lineNumber: Int, newValue: String) {
        val lines = file.readLines().toMutableList()
        if (lineNumber in lines.indices) {
            lines[lineNumber] = newValue
            file.writeText(lines.joinToString("\n"))
        }
    }

    /**
     * write header to CSv
     */
    private suspend fun writeHeader(
        headerString: String,
        buf: BufferedWriter,
        data: DosageDeviceDto,
        nfcReaderViewModel: NfcReaderViewModel
    ) {
        if (headerString.isNotEmpty()) {
          withContext(Dispatchers.IO) {
              buf.append("DeviceID,${data.deviceId}")
              buf.newLine()
              buf.newLine()
              if (NfcConstant.ReaderMode.NFC.mode != nfcReaderViewModel.mode) {
                  buf.append("Hp10 BG value,${data.hp10BgValue}")
                  buf.newLine()
                  buf.append("Hp0.07 BG value,${data.hp007BgValue}")
              } else {
                  buf.newLine()
              }
              buf.newLine()
              buf.newLine()
              buf.append(headerString)
          }
        }
    }
}