package net.qten.nfcreader.utils

import android.icu.math.BigDecimal
import android.util.Log
import net.qten.nfcreader.constant.NfcConstant
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.stream.Collectors

class Utils {
    companion object {
        fun extractCsvData(inputStream: InputStream): List<List<String>> {
            val csvData = mutableListOf<List<String>>()
            inputStream.use { stream ->
                val isr = InputStreamReader(stream, Charsets.UTF_8)
                val t = BufferedReader(isr).lines().collect(Collectors.toList())
                // Serial Number:
                csvData.add(listOf(t[0].split(",")[1]))
                // Skip 6 lines to data:
                t.stream().skip(6).forEach {
                    if (it != NfcConstant.EMPTY_STRING) {
                        val tokens = it.split(",")
                        if (tokens.isNotEmpty()) {
                            csvData.add(tokens)
                        }
                    }
                }
            }
            return csvData
        }

        /**
         * Convert the input string to date
         * @param inputString The input string to convert
         * @param dateFormat The date format string The date format string (Optional - if not specified, will
         * use the default yyyy:mm:dd HH:MM)
         * @return The parsed date if parse success, or else null
         */
        fun stringToDate(
            inputString: String, dateFormat: String = NfcConstant.DATE_CSV_FORMAT_TEMPLATE
        ): Date? {
            if (inputString.isEmpty()) {
                return null
            }

            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault(Locale.Category.FORMAT))
            return try {
                val result = sdf.parse(inputString)
                result
            } catch (e: ParseException) {
                Log.e("ERROR", e.message.toString())
                null
            }
        }

        /**
         * Convert the input date to string with format
         * @param inputDate The date to convert
         * @param dateFormat The date format string (Optional - if not specified, will
         * use the default yyyy:mm:dd HH:MM)
         * @return The converted string, or else null
         */
        fun dateToString(
            inputDate: Date, dateFormat: String = NfcConstant.DATETIME_FORMAT_YYYY_MM_DD_HH_MM
        ): String {
            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault(Locale.Category.FORMAT))

            return sdf.format(inputDate)
        }
    }
}

fun Boolean.toInt() = if (this) 1 else 0

/**
 * Extension convert to boolean (0 -> false, 1 -> true)
 */
fun String.toQtenBoolean(): Boolean {
    val intValue = this.toIntOrNull() ?: throw Error("Invalid input string: $this")
    return when (intValue) {
        0 -> false
        1 -> true
        else -> throw Error("Invalid integer value for conversion to Boolean: $intValue")
    }
}

fun BigDecimal.toFormattedString(): String {
    return this.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString()
}