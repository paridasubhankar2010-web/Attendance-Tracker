package com.example.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.ui.theme.SubjectColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditSubjectDialog(
    subjectToEdit: Subject? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        code: String,
        threshold: Float,
        attended: Int,
        total: Int,
        colorHex: Long,
        room: String,
        scheduleDays: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(subjectToEdit?.name ?: "") }
    var code by remember { mutableStateOf(subjectToEdit?.code ?: "") }
    var threshold by remember { mutableFloatStateOf(subjectToEdit?.minThreshold ?: 75.0f) }
    var attendedText by remember { mutableStateOf(subjectToEdit?.attendedClasses?.toString() ?: "0") }
    var totalText by remember { mutableStateOf(subjectToEdit?.totalClasses?.toString() ?: "0") }
    var selectedColor by remember { mutableLongStateOf(subjectToEdit?.colorHex ?: SubjectColors.first()) }
    var room by remember { mutableStateOf(subjectToEdit?.room ?: "") }
    var nameError by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val initialSelectedDays = subjectToEdit?.scheduleDays
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf("Mon", "Wed", "Fri")
    val selectedDays = remember { mutableStateListOf<String>().apply { addAll(initialSelectedDays) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (subjectToEdit == null) "Add New Subject" else "Edit Subject",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Subject Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Operating Systems") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Subject name is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subject Code & Classroom
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Code") },
                        placeholder = { Text("CS204") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_code_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room / Hall") },
                        placeholder = { Text("Hall B") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_room_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Minimum Attendance Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance Goal / Min Threshold",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f%%", threshold),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 50f..95f,
                    steps = 8,
                    modifier = Modifier.testTag("threshold_slider")
                )

                // Quick preset buttons for 65%, 75%, 80%, 85%
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(65f, 75f, 80f, 85f).forEach { preset ->
                        FilterChip(
                            selected = (threshold == preset),
                            onClick = { threshold = preset },
                            label = { Text("${preset.toInt()}%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Initial / Current Counts
                Text(
                    text = "Current Attendance (Classes held so far)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = attendedText,
                        onValueChange = { attendedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attended_count_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Held") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_count_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule Days
                Text(
                    text = "Class Days",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysOfWeek.forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                            },
                            label = { Text(day) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Tag
                Text(
                    text = "Color Tag",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectColors.forEach { colorVal ->
                        val isCurrent = selectedColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isCurrent) 3.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorVal },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val att = attendedText.toIntOrNull() ?: 0
                    val tot = totalText.toIntOrNull() ?: 0
                    val validAtt = if (att > tot) tot else att
                    val scheduleStr = selectedDays.joinToString(", ")
                    onSave(
                        subjectToEdit?.id ?: 0L,
                        name,
                        code,
                        threshold,
                        validAtt,
                        tot,
                        selectedColor,
                        room,
                        scheduleStr,
                        ""
                    )
                },
                modifier = Modifier.testTag("save_subject_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_subject_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
