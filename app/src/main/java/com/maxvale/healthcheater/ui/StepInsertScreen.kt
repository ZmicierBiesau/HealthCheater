package com.maxvale.healthcheater

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

@Composable
fun StepInsertScreen(healthConnectClient: HealthConnectClient) {
    val context = LocalContext.current
    var steps by remember { mutableStateOf("1000") }
    val now = remember { LocalDateTime.now() }
    var startTime by remember { mutableStateOf(now.minusHours(1)) }
    var endTime by remember { mutableStateOf(now) }
    var result by remember { mutableStateOf<String?>(null) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Install Health Connect before cheating",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
                .padding(all = 16.dp))

        Text("Add steps", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = steps,
            onValueChange = { steps = it },
            label = { Text("Amount of steps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(onClick = {
            showDateTimePicker(context) { pickedDateTime ->
                startTime = pickedDateTime
            }
        }) {
            Text("Pick Start Time: ${startTime?.format(formatter) ?: "Not selected"}")
        }

        Button(onClick = {
            showDateTimePicker(context) { pickedDateTime ->
                endTime = pickedDateTime
            }
        }) {
            Text("Pick End Time: ${endTime?.format(formatter) ?: "Not selected"}")
        }

        Button(onClick = {
            val permission = setOf(HealthPermission.getWritePermission(StepsRecord::class))

            val count = steps.toLongOrNull()
            if (count == null || count <= 0) {
                result = "Enter >0 steps"
                return@Button
            }

            val scope = CoroutineScope(Dispatchers.Main)

            scope.launch {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                if (!granted.containsAll(permission)) {
                    result = "No access for health app"
                    return@launch
                }

                try {
                    val start = ZonedDateTime.of(startTime, ZoneId.systemDefault()).toInstant()
                    val end = ZonedDateTime.of(endTime, ZoneId.systemDefault()).toInstant()
                    val offset = ZoneOffset.systemDefault().rules.getOffset(start)
                    val metadata = Metadata.activelyRecorded(
                        device = Device(type = Device.TYPE_RING)
                    )

                    val session = ExerciseSessionRecord(
                        metadata        = metadata,
                        startTime       = start,
                        startZoneOffset = offset,
                        endTime         = end,
                        endZoneOffset   = offset,
                        exerciseType    = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
                    )

                    val distanceRecord = DistanceRecord(
                        metadata        = metadata,
                        startTime       = start,
                        startZoneOffset = offset,
                        endTime         = end,
                        endZoneOffset   = offset,
                        distance        = Length.meters(steps.toDouble() * 0.9)
                    )


                    val stepsRecord = StepsRecord(
                        count = steps.toLong(),
                        startTime = start,
                        endTime = end,
                        startZoneOffset = offset,
                        endZoneOffset = offset,
                        metadata = metadata,
                    )
                    healthConnectClient.insertRecords(listOf(stepsRecord))
                    result = "Successfuly added $count steps"
                } catch (e: Exception) {
                    result = "Error: ${e.message}"
                }
            }
        }) {
            Text("Add steps")
        }

        result?.let {
            Text(it, color = Color.Blue)
        }

        Button(
            onClick = {
                try {
                    // Intent to open Health Connect settings
                    val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case where Health Connect is not installed or settings are unavailable
                    e.printStackTrace()
                    Toast.makeText(context, "Health Connect is not available", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("Open Health Connect Settings")
        }
    }
}

fun showDateTimePicker(context: Context, onDateTimeSelected: (LocalDateTime) -> Unit) {
    val now = LocalDateTime.now()
    val calendar = Calendar.getInstance()
    calendar.time = Date.from(now.atZone(ZoneId.systemDefault()).toInstant())

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selectedDateTime = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                    onDateTimeSelected(selectedDateTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
