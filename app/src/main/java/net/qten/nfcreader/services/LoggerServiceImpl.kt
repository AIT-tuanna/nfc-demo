package net.qten.nfcreader.services

import android.content.Context
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class LoggerServiceImpl(context: Context) : LoggerService {
    private val logger: Logger = Logger.getLogger(LoggerService::class.java.name)
    private val fileHandler: FileHandler

    init {
        val logDirectory = context.getExternalFilesDir(null)
        fileHandler = FileHandler("${logDirectory?.path}/NFCReaderLog.log", true)
        fileHandler.formatter = SimpleFormatter()
        logger.addHandler(fileHandler)
    }

    /**
     * write log info
     */
    override fun logInfo(message: String) {
        logger.info(message)
    }

    /**
     * write log warning/error
     */
    override fun logWarning(message: String) {
        logger.warning(message)
    }

    /**
     * Call this method when you're done with logging to release resources
     */

    override fun close() {
        fileHandler.close()
    }
}