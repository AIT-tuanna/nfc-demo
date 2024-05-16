package net.qten.nfcreader

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.qten.nfcreader.constant.NfcConstant
import net.qten.nfcreader.services.LoggerServiceImpl
import net.qten.nfcreader.services.NfcReaderServiceImpl
import net.qten.nfcreader.theme.NfcReaderTheme
import net.qten.nfcreader.viewModels.NfcReaderViewModel
import net.qten.nfcreader.viewModels.NfcViewModel

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
    }

    private val nfcReaderViewModel = NfcReaderViewModel()
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var myTag: Tag
    private lateinit var nfcReaderService: NfcReaderServiceImpl
    private lateinit var logger: LoggerServiceImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAllPermissionsGranted()) {
            requestPermissions()
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show()
        }

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_MUTABLE
        )

        val detectedTag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        detectedTag.addCategory(Intent.CATEGORY_DEFAULT)

        logger = LoggerServiceImpl(this.applicationContext)
        nfcReaderService = NfcReaderServiceImpl(this.applicationContext)

        setContent {
            val navController = rememberNavController()
            NfcReaderTheme {
                NfcReaderApp(navController, nfcReaderViewModel, {
                    nfcAdapter?.enableForegroundDispatch(
                        this, pendingIntent, null, null
                    )
                }, {
                    nfcAdapter?.disableForegroundDispatch(this)
                })
            }

            // Check if there's a navigation request
            intent.getStringExtra("navigateTo")?.let { destination ->
                if (destination == NfcConstant.Route.DATA_READING_MENU.routeName) {
                    navController.navigate(destination)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val viewModel: NfcViewModel by viewModels()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.readNfc(
                    nfcReaderViewModel,
                    intent,
                    this@MainActivity,
                    logger,
                    nfcReaderService
                )
            }
        }

    }

    override fun onPause() {
        super.onPause()
        logger.logInfo("Application pause")
        logger.close()
        if (nfcReaderViewModel.isNfcEnabled.value) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
    }

    override fun onResume() {
        super.onResume()
        logger.logInfo("Application resume")
        logger = LoggerServiceImpl(this.applicationContext)
        if (nfcReaderViewModel.isNfcEnabled.value) {
            nfcAdapter?.enableForegroundDispatch(
                this, pendingIntent, null, null
            )
        }
    }

    private fun isAllPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun requestPermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissionsToRequest.toTypedArray(), REQUEST_CODE
            )
        }
    }

    private fun isPermissionGranted(ctx: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                ctx, permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }
}