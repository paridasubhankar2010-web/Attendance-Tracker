package com.example.ui.export

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Subject
import com.example.util.CsvExporter
import kotlinx.coroutines.launch

@Composable
fun ExportAttendanceDialog(
    initialSubject: Subject?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onGenerateCsv: suspend (Subject?) -> Pair<String, String>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0 = Entire Semester, 1 = Specific Subject
    var exportScope by remember {
        mutableIntStateOf(if (initialSubject != null) 1 else 0)
    }
    var selectedSubject by remember {
        mutableStateOf(initialSubject ?: subjects.firstOrNull())
    }
    var isSubjectDropdownOpen by remember { mutableStateOf(false) }

    var cachedFileName by remember { mutableStateOf("semester_attendance.csv") }
    var cachedCsvContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Recompute CSV whenever scope or selected subject changes
    LaunchedEffect(exportScope, selectedSubject) {
        isLoading = true
        val target = if (exportScope == 1) selectedSubject else null
        val (fileName, content) = onGenerateCsv(target)
        cachedFileName = fileName
        cachedCsvContent = content
        isLoading = false
    }

    // Storage Access Framework launcher to save CSV to Downloads / Drive
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = CsvExporter.writeToUri(context, uri, cachedCsvContent)
            if (success) {
                Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            } else {
                Toast.makeText(context, "Failed to write CSV file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Export Attendance CSV",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Generate and share spreadsheet backups for your records or academic advisors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scope Selector
                Text(
                    text = "Export Scope",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exportScope == 0,
                        onClick = { exportScope = 0 },
                        label = { Text("Entire Semester") },
                        modifier = Modifier.testTag("export_scope_semester_chip")
                    )
                    FilterChip(
                        selected = exportScope == 1,
                        onClick = { exportScope = 1 },
                        label = { Text("Single Subject") },
                        modifier = Modifier.testTag("export_scope_subject_chip")
                    )
                }

                // Subject picker if Single Subject is selected
                if (exportScope == 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Subject",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSubjectDropdownOpen = true }
                                .padding(12.dp)
                                .testTag("select_subject_dropdown_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectedSubject != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(selectedSubject!!.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedSubject!!.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                    } else {
                                        Text(text = "No subjects available")
                                    }
                                }
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = isSubjectDropdownOpen,
                            onDismissRequest = { isSubjectDropdownOpen = false }
                        ) {
                            subjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(sub.colorHex))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${sub.name} (${sub.code})")
                                        }
                                    },
                                    onClick = {
                                        selectedSubject = sub
                                        isSubjectDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "File: $cachedFileName",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (exportScope == 0) {
                                "Includes summary table of all ${subjects.size} subjects and complete daily attendance log ledger."
                            } else {
                                "Includes ${selectedSubject?.name ?: "Subject"} metrics and complete date history."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share with Advisor button
                FilledTonalButton(
                    onClick = {
                        val title = if (exportScope == 0) {
                            "Semester Attendance Report"
                        } else {
                            "${selectedSubject?.name ?: "Subject"} Attendance Report"
                        }
                        CsvExporter.launchShareIntent(
                            context = context,
                            fileName = cachedFileName,
                            csvContent = cachedCsvContent,
                            shareTitle = title
                        )
                    },
                    enabled = !isLoading && cachedCsvContent.isNotEmpty(),
                    modifier = Modifier.testTag("share_csv_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }

                // Save to Device button
                Button(
                    onClick = {
                        createDocLauncher.launch(cachedFileName)
                    },
                    enabled = !isLoading && cachedCsvContent.isNotEmpty(),
                    modifier = Modifier.testTag("save_csv_device_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_export_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
