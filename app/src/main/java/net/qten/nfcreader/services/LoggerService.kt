package net.qten.nfcreader.services

interface LoggerService {
    /**
     * write log info
     */
    fun logInfo(message: String)

    /**
     * write log warning/error
     */
    fun logWarning(message: String)

    /**
     * Call this method when you're done with logging to release resources
     */
    fun close()
}