package net.qten.nfcreader.viewModels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import net.qten.nfcreader.constant.NfcConstant

data class NfcReaderViewModel(
    var isNfcEnabled: MutableState<Boolean> = mutableStateOf(false),
    var mode: Int = NfcConstant.ReaderMode.NFC.mode,
    var notification: MutableState<String> = mutableStateOf(NfcConstant.EMPTY_STRING),
    var isError: MutableState<Boolean> = mutableStateOf(false),
)
