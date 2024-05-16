package net.qten.nfcreader.pages.dosageReadingMenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.qten.nfcreader.R
import net.qten.nfcreader.component.Button
import net.qten.nfcreader.component.QtenScreenTitle

@Composable
fun DosageReadingMenu(onNavigateToScanMenu: () -> Unit, onNavigateToList: () -> Unit) {
    Scaffold(
        Modifier.fillMaxHeight()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QtenScreenTitle(text = stringResource(id = R.string.menu_main_title))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Button(text = stringResource(id = R.string.button_scan_title),
                    fontSize = 20,
                    onClick = { onNavigateToScanMenu() })
                Spacer(modifier = Modifier.height(30.dp))
                Button(text = stringResource(id = R.string.button_display_title),
                    fontSize = 20,
                    onClick = { onNavigateToList() })
            }
        }
    }
}