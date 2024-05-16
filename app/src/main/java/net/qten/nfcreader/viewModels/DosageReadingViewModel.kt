package net.qten.nfcreader.viewModels

import android.icu.math.BigDecimal
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.utils.Utils
import net.qten.nfcreader.utils.toQtenBoolean
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Use to communicate between DosageDto list screen
 * and DosageDto detail screen
 */
class DosageReadingViewModel : ViewModel() {
    private val _isFromDosageDetail = MutableStateFlow(false)
    val isFromMenu = _isFromDosageDetail.asStateFlow()

    private val _dosages by lazy {
        MutableStateFlow<List<DosageDto>>(mutableListOf())
    }
    val dosages = _dosages.asStateFlow()

    private val _dosage by lazy {
        MutableStateFlow<DosageDto?>(null)
    }
    val dosage = _dosage.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    var firstVisibleIndex = 0
    var firstVisibleIndexOffset = 0

    var firstVisibleIndexNfc = 0
    var firstVisibleIndexOffsetNfc = 0

    /**
     * Find dosage item by device id
     * @param id Row data id
     * @return Dosage if exists, or else null
     */
    suspend fun findById(id: String) {
        _dosage.emit(_dosages.value.find {
            it.id.compareTo(id) == 0
        })
    }

    suspend fun setIsFromMenu(value: Boolean) {
        _isFromDosageDetail.emit(value)
    }

    /**
     * Process CSV asynchronously
     * @param dir Path of files
     * @param mode ScreenMode
     * @see NfcConstant.ReaderMode.DEVICE_MEMORY
     * @see NfcConstant.ReaderMode.NFC
     */
    suspend fun processCsv(dir: Path?, mode: String?) {
        viewModelScope.launch {
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                Log.i("CSV Processing", "External storage is not mounted.")
                return@launch
            }

            _loading.emit(true)
            val filenameRegex = when (mode) {
                NfcConstant.ReaderMode.DEVICE_MEMORY.toString() -> NfcConstant.DEVICE_MEMORY_DATA_CSV_FILENAME_REGEX
                else -> NfcConstant.NFC_DATA_CSV_FILENAME_REGEX
            }

            try {
                dir?.let {
                    _dosages.emit(readCsv(it, filenameRegex))
                } ?: Log.i("CSV Processing", "Directory path is null.")
            } catch (e: IOException) {
                Log.i("CSV Processing Error", e.message.toString())
            } finally {
                _loading.emit(false)
            }
        }
    }

    /**
     * Read csv
     * @param dir Path of the file(s)
     * @param filename Csv file name
     * @return Data read from csv
     */
    private suspend fun readCsv(dir: Path, filename: String): List<DosageDto> {
        val regex = filename.toRegex()

        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                val files = getMatchingFiles(dir, regex)
                // Ensure the sequence operations are performed within the IO context
                readDataFromFiles(files).asSequence()
                    .distinctBy { it.id }
                    .sortedWith(compareByDescending<DosageDto> { it.absTime }.thenBy { it.serialNumber })
                    .toList()
            } catch (e: IOException) {
                Log.e("readCsv", "Error reading CSV files: ${e.message}")
                emptyList()
            }
        }
    }

    private suspend fun getMatchingFiles(dir: Path, regex: Regex): List<File> {
        Log.i("getMatchingFiles", "Start ${System.currentTimeMillis()}")
        return withContext(Dispatchers.IO) {
            try {
                Files.newDirectoryStream(dir) { path -> regex.matches(path.fileName.toString()) }
                    .use { stream ->
                        stream.asSequence().filter { Files.isRegularFile(it) }.map { it.toFile() }
                            .toList()
                    }
            } catch (e: IOException) {
                Log.e("getMatchingFiles", "Error accessing directory: ${e.message}")
                emptyList()
            } finally {
                Log.i("getMatchingFiles", "End ${System.currentTimeMillis()}")
            }
        }
    }

    private suspend fun readDataFromFiles(files: List<File>): List<DosageDto> {
        Log.i("readDataFromFiles", "Start ${System.currentTimeMillis()}")
        val result = mutableListOf<DosageDto>()
        coroutineScope {
            files.map { file ->
                async(Dispatchers.IO) {
                    file.bufferedReader(Charsets.UTF_8).use { reader ->
                        val header = reader.readLine().split(",")[1]
                        reader.lineSequence().drop(6).filter { it.isNotBlank() }
                            .map { it.split(",") }.filter { it.isNotEmpty() }.forEach { dataRow ->
                                val dosageDto = createDosageDtoFromData(dataRow, header)
                                synchronized(result) {
                                    result.add(dosageDto)
                                }
                            }
                    }
                }
            }.awaitAll()
        }
        Log.i(
            "readDataFromFiles",
            "Finished ${System.currentTimeMillis()}, total ${result.size} items"
        )
        return result
    }

    private fun createDosageDtoFromData(
        dataRow: List<String>, serialNumber: String = NfcConstant.EMPTY_STRING
    ): DosageDto {
        val id = Utils.dateToString(
            Utils.stringToDate(dataRow[0])!!, ID_DATE_FORMAT_YYYY_MM_DD_HH_MM_SS
        ) + serialNumber

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
            bgCorrectionError = dataRow[6].toQtenBoolean(),
            cpuError = dataRow[7].toQtenBoolean(),
            temperatureError = dataRow[8].toQtenBoolean(),
            batteryVoltageError = dataRow[9].toQtenBoolean(),
            betaSensorError = dataRow[10].toQtenBoolean(),
            gammaSensorError = dataRow[11].toQtenBoolean(),
            unequipped = dataRow[12].toQtenBoolean(),
            impact = dataRow[13].toQtenBoolean()
        )
    }

    companion object {
        /** Dosage ID's date format */
        const val ID_DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = "YYYYMMDDHHmmss"
    }
}