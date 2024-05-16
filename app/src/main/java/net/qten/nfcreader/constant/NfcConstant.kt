package net.qten.nfcreader.constant

object NfcConstant {
    const val EMPTY_STRING = ""
    const val UINT_ZERO = 0u
    const val INTEGER_ZERO = 0
    /** csv file name: NFCTrendData_xxxx.csv */
    const val NFC_DATA_CSV_FILENAME = "NFCTrendData_%s.csv"
    /** csv file name: MeasData_xxxx.csv */
    const val DEVICE_MEMORY_DATA_CSV_FILENAME = "MeasData_%s.csv"
    /** csv file name regex: NFCTrendData_*.csv */
    const val NFC_DATA_CSV_FILENAME_REGEX = "NFCTrendData_.*\\.csv"
    /** csv file name regex: MeasData_*.csv */
    const val DEVICE_MEMORY_DATA_CSV_FILENAME_REGEX = "MeasData_.*\\.csv"

    /** NFC read command : 0x30 */
    const val NFC_READ_COMMAND = 0x30
    /** NFC fast read command : 0x3A */
    const val NFC_FAST_READ_COMMAND = 0x3A
    /** NFC fast write command : 0xA6 */
    const val NFC_FAST_WRITE_COMMAND = 0xA6
    /** NFC read command : 0xC2 */
    const val NFC_SECTOR_SELECT_COMMAND = 0xC2
    /** CSV datetime format: MM/dd/yyyy HH:mm */
    const val DATE_CSV_FORMAT_TEMPLATE = "MM/dd/yyyy HH:mm"
    /** Number of bytes per dosage data : 13 */
    const val NUMBER_OF_BYTES_PER_DOSAGE_DATA = 13

    /** yyyy/MM/dd HH:mm */
    const val DATETIME_FORMAT_YYYY_MM_DD_HH_MM = "yyyy/MM/dd HH:mm"

    /** dd/MM/yyyy HH:mm */
    const val DATETIME_FORMAT_DD_MM_YYYY_HH_MM = "dd/MM/yyyy HH:mm"

    /** 1000 */
    const val MAX_MICRO_VALUE = 1000L

    /** Reader mode */
    enum class ReaderMode(val mode: Int) {
        /** 本体メモリ */
        DEVICE_MEMORY(0),
        /** NFCメモリ */
        NFC(1);
    }

    /** Address of dosage data in NFC memory */
    enum class DosageNfcMemoryAddress(val start: Int, val end: Int) {
        /** 絶対時間 */
        ABS_TIME(0, 4),
        /** 電池電圧 */
        BATTERY_VOL(4, 5),
        /** 温度 */
        TEMPERATURE(5, 6),
        /** 積算カウンタ */
        ACCUMULATED_COUNTER(6, 8),
        /**  Hp10線量値 */
        HP_10_DOSE_VALUE(8, 10),
        /**  Hp0.07線量値 */
        HP_007_DOSE_VALUE(10, 12),
        /** エラー */
        ERROR(12, 13);
    }

    /** sector address */
    enum class NfcMemoryAddress(val start: Int, val end: Int) {
        /** sector 0 : (0x04, 0xDC)*/
        SECTOR_0(0x04, 0xDC),
        /** sector 1 : (0x00, 0xFC)*/
        SECTOR_1(0x00, 0xFC);
    }

    /** APDU dosage data format : vi endian la little endian nen gia tri offset phai dao nguoc so voi TL*/
    enum class APDUDataFormat(val length: Int, val category: UInt, val offset1: UInt, val offset2: UInt) {
        /** 機器ID : (4, 0x00, 0x010C) */
        DEVICE_ID(4, 0x00u, 0x01u, 0x0Cu),
        /** 線量測定データ保存ページ数 : (4, 0x05, 0x001C) */
        PAGE_NUMBER(4, 0x05u, 0x00u, 0x1Cu),
        /** Hp10 BG値 : (4, 0x05, 0x007C) */
        HP_10_BG_VALUE(4, 0x05u, 0x00u, 0x7Cu),
        /** Hp0.07 BG値 : (4, 0x05, 0x009C) */
        HP_007_BG_VALUE(4, 0x05u, 0x00u, 0x9Cu),
        /** トレンドデータ : (32, 0x05, 0x00BC) */
        DOSAGE_DATA(32, 0x05u, 0x00u, 0xBCu)
    }

    /** Application Routes */
    enum class Route(val routeName: String) {
        HOME_MENU("home_menu"),
        DATA_READING_MENU("data_reading_menu"),
        NFC_READER("nfc_reader"),
        LIST("list"),
        DATA_LIST_MENU("data")
    }
}