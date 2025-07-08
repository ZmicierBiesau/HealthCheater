package com.maxvale.healthcheater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthConnectClient = HealthConnectClient.getOrCreate(this)

        setContent {
            MaterialTheme {
                val permissions = remember {
                    setOf(HealthPermission.getWritePermission(StepsRecord::class))
                }

                val scope = rememberCoroutineScope()
                var permissionGranted by remember { mutableStateOf(false) }

                // Launcher для запроса разрешений
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { resultMap ->
                    permissionGranted = resultMap.values.all { it }
                }

                LaunchedEffect(Unit) {
                    val granted = healthConnectClient.permissionController.getGrantedPermissions()
                    if (!granted.containsAll(permissions)) {
                        // Запускаем системный запрос разрешений
                        permissionLauncher.launch(permissions.toTypedArray())
                    } else {
                        permissionGranted = true
                    }
                }

                // Показываем экран только если разрешения выданы
//                if (permissionGranted) {
                    StepInsertScreen(healthConnectClient)
//                }
            }
        }
    }
}
