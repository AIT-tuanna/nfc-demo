package net.qten.nfcreader.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.qten.nfcreader.R
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.dto.DosageDto
import net.qten.nfcreader.viewModels.DosageReadingViewModel

@Composable
fun ScrollContent(
    navController: NavHostController,
    mode: String,
    items: List<DosageDto>,
    viewModel: DosageReadingViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readerMode = NfcConstant.ReaderMode.DEVICE_MEMORY.toString()

    // Define lazyListState based on the mode
    val lazyListState = rememberLazyListState(
        if (mode == readerMode) viewModel.firstVisibleIndex else viewModel.firstVisibleIndexNfc,
        if (mode == readerMode) viewModel.firstVisibleIndexOffset else viewModel.firstVisibleIndexOffsetNfc
    )

    // Define colors outside LazyColumn to avoid recomputation
    val whiteColor = remember { ContextCompat.getColor(context, R.color.white) }
    val oddItemBgColor = remember { ContextCompat.getColor(context, R.color.item_odd_bg) }

    LazyColumn(
        modifier = Modifier
            .verticalScrollBar(lazyListState)
            .fillMaxSize(),
        state = lazyListState
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            val backgroundColor = if (index % 2 == 0) whiteColor else oddItemBgColor

                val route = remember {
                    "${NfcConstant.Route.DATA_LIST_MENU.routeName}?mode=$mode&id=${item.id}"
                }

                DosageDataItem(
                    onClick = {
                        scope.launch {
                            viewModel.updateListState(lazyListState, mode)
                        }
                        navController.navigate(route)
                    },
                    backgroundColor = backgroundColor,
                    item = item
                )
        }
    }
}

// Extension function to update the list state in the ViewModel
fun DosageReadingViewModel.updateListState(
    listState: LazyListState,
    mode: String
) {
    val readerMode = NfcConstant.ReaderMode.DEVICE_MEMORY.toString()
    if (mode == readerMode) {
        firstVisibleIndex = listState.firstVisibleItemIndex
        firstVisibleIndexOffset = listState.firstVisibleItemScrollOffset
    } else {
        firstVisibleIndexNfc = listState.firstVisibleItemIndex
        firstVisibleIndexOffsetNfc = listState.firstVisibleItemScrollOffset
    }
}

@Composable
fun Modifier.verticalScrollBar(
    state: LazyListState, width: Dp = 6.dp
): Modifier {
    val scrollbarWidthPx = with(LocalDensity.current) { width.toPx() }
    val totalItemsCount = remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
    val firstVisibleItemIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val firstVisibleItemScrollOffset =
        remember { derivedStateOf { state.firstVisibleItemScrollOffset } }

    return this.then(Modifier.drawWithContent {
        drawContent()

        val elementHeight = size.height / totalItemsCount.value
        val scrollbarHeight = elementHeight * 16
        val scrollbarOffsetY =
            firstVisibleItemIndex * elementHeight + firstVisibleItemScrollOffset.value / 4

        drawRoundRect(
            color = Color.DarkGray,
            topLeft = Offset(size.width - scrollbarWidthPx, scrollbarOffsetY),
            size = Size(scrollbarWidthPx, scrollbarHeight),
            cornerRadius = CornerRadius(8f)
        )
    })
}