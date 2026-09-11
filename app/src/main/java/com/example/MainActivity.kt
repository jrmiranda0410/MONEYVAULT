package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.MoneyVaultDatabase
import com.example.data.repository.MoneyVaultRepository
import com.example.notification.NotificationHelper
import com.example.ui.MoneyVaultApp
import com.example.ui.theme.MoneyVaultTheme
import com.example.ui.viewmodel.LockViewModel
import com.example.ui.viewmodel.MoneyVaultViewModel
import com.example.ui.viewmodel.MoneyVaultViewModelFactory

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        val db = MoneyVaultDatabase.getDatabase(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)
        MoneyVaultRepository(db, notificationHelper)
    }

    private val factory by lazy {
        MoneyVaultViewModelFactory(repository)
    }

    private val lockViewModel: LockViewModel by viewModels { factory }
    private val vaultViewModel: MoneyVaultViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userSettings by vaultViewModel.userSettings.collectAsStateWithLifecycle()
            val themeMode = userSettings?.themeMode ?: "SYSTEM"

            // Handle Android 13+ Notification permission
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* permission result */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MoneyVaultTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MoneyVaultApp(
                        lockViewModel = lockViewModel,
                        vaultViewModel = vaultViewModel
                    )
                }
            }
        }
    }
}
