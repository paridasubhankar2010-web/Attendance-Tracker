package com.example.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.ui.components.AttendancePercentageCircle
import com.example.ui.components.BunkAdviceChip
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusSafe
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailSheet(
    subject: Subject,
    onDismiss: () -> Unit,
    onEdit: (Subject) -> Unit,
    onDelete: (Subject) -> Unit,
    onExportSubject: (Subject) -> Unit,
    onUpdateCounts: (subjectId: Long, attended: Int, total: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Simulator states: future attended, future missed
    var simFutureAttended by remember { mutableIntStateOf(0) }
    var simFutureMissed by remember { mutableIntStateOf(0) }

    val simulatedAttended = subject.attendedClasses + simFutureAttended
    val simulatedTotal = subject.totalClasses + simFutureAttended + simFutureMissed
    val simulatedPercentage = if (simulatedTotal > 0) {
        (simulatedAttended.toDouble() / simulatedTotal) * 100.0
    } else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier.testTag("subject_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(subject.colorHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (subject.code.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = subject.code,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_detail_sheet_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = subject.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subject.room.isNotEmpty() || subject.scheduleDays.isNotEmpty()) {
                val details = listOfNotNull(
                    subject.room.ifEmpty { null },
                    subject.scheduleDays.ifEmpty { null }
                ).joinToString(" • ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Percentage & Statistics Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendancePercentageCircle(
                        percentage = subject.percentage,
                        minThreshold = subject.minThreshold,
                        size = 84.dp,
                        strokeWidth = 8.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        StatusBadge(
                            isBelowThreshold = subject.isBelowThreshold,
                            isNearThreshold = subject.isNearThreshold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Attended: ${subject.attendedClasses} / ${subject.totalClasses} classes",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Required Goal: ${subject.minThreshold.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Low attendance alert banner or Bunk Advice
            if (subject.isBelowThreshold) {
                Surface(
                    color = StatusCriticalContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = StatusCritical,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Attendance Alert!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusCritical
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your attendance is ${(subject.minThreshold - subject.percentage).toInt()}% below the minimum ${subject.minThreshold.toInt()}% requirement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👉 You must attend the next ${subject.classesNeededCount} consecutive classes to get back on track.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusCritical
                            )
                        }
                    }
                }
            } else {
                BunkAdviceChip(
                    canMissCount = subject.safeToMissCount,
                    needAttendCount = subject.classesNeededCount,
                    isBelowThreshold = false,
                    minThreshold = subject.minThreshold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive "What-If" Simulator
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔮 Attendance Simulator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Project your percentage before skipping or attending upcoming classes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated result
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Projected Attendance:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%% (%d/%d)", simulatedPercentage, simulatedAttended, simulatedTotal),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (simulatedPercentage < subject.minThreshold) StatusCritical else StatusSafe
                                )
                            )
                        }
                        if (simFutureAttended > 0 || simFutureMissed > 0) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        simFutureAttended = 0
                                        simFutureMissed = 0
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Future Attended Slider
                    Text(
                        text = "Attend next $simFutureAttended classes",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Slider(
                        value = simFutureAttended.toFloat(),
                        onValueChange = { simFutureAttended = it.toInt() },
                        valueRange = 0f..20f,
                        steps = 19
                    )

                    // Future Missed Slider
                    Text(
                        text = "Miss/Bunk next $simFutureMissed classes",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Slider(
                        value = simFutureMissed.toFloat(),
                        onValueChange = { simFutureMissed = it.toInt() },
                        valueRange = 0f..20f,
                        steps = 19
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Direct Counter Adjuster
            Text(
                text = "Manual Counter Adjustment",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Attended classes counter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Attended", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${subject.attendedClasses}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    if (subject.attendedClasses > 0) {
                                        onUpdateCounts(
                                            subject.id,
                                            subject.attendedClasses - 1,
                                            subject.totalClasses
                                        )
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease attended")
                            }
                            IconButton(
                                onClick = {
                                    val newAtt = subject.attendedClasses + 1
                                    val newTot = if (newAtt > subject.totalClasses) newAtt else subject.totalClasses
                                    onUpdateCounts(subject.id, newAtt, newTot)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase attended")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Total classes counter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Total Held", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${subject.totalClasses}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    if (subject.totalClasses > 0 && subject.totalClasses > subject.attendedClasses) {
                                        onUpdateCounts(
                                            subject.id,
                                            subject.attendedClasses,
                                            subject.totalClasses - 1
                                        )
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease total")
                            }
                            IconButton(
                                onClick = {
                                    onUpdateCounts(
                                        subject.id,
                                        subject.attendedClasses,
                                        subject.totalClasses + 1
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase total")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export Subject CSV Button
            OutlinedButton(
                onClick = { onExportSubject(subject) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_subject_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Subject Attendance (CSV)")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Edit & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { onEdit(subject) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_subject_button")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Info")
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delete_subject_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete '${subject.name}'?") },
            text = { Text("All logged attendance records for this subject will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(subject)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
