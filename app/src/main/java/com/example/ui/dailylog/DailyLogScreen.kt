package com.example.ui.dailylog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.AttendanceLog
import com.example.data.AttendanceStatus
import com.example.data.Subject
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalContainer
import com.example.ui.theme.StatusSafe
import com.example.ui.theme.StatusSafeContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(
    subjects: List<Subject>,
    selectedDateEpoch: Long,
    dailyLogs: List<AttendanceLog>,
    onSelectDate: (Long) -> Unit,
    onMarkPresence: (subjectId: Long, status: AttendanceStatus) -> Unit,
    onClearPresence: (subjectId: Long) -> Unit,
    onUpdateNote: (subjectId: Long, note: String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onAddSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpoch)
    val today = LocalDate.now()
    val isToday = selectedDate == today

    val logsMap = dailyLogs.associateBy { it.subjectId }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_log_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "Daily Attendance Log",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Toggle Present or Absent for each class, or mark all in bulk",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date Navigator Card with Previous / Next and Calendar Picker Dialog
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Day Button
                        IconButton(
                            onClick = { onSelectDate(selectedDate.minusDays(1).toEpochDay()) },
                            modifier = Modifier.testTag("prev_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day"
                            )
                        }

                        // Date Title & Calendar Trigger Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDatePickerDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("calendar_picker_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isToday) "Today" else selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick date from calendar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Next Day Button
                        IconButton(
                            onClick = { onSelectDate(selectedDate.plusDays(1).toEpochDay()) },
                            modifier = Modifier.testTag("next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day"
                            )
                        }
                    }

                    // 7-day rolling date strip for quick switching
                    Spacer(modifier = Modifier.height(12.dp))
                    val startDay = selectedDate.minusDays(3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0..6) {
                            val day = startDay.plusDays(i.toLong())
                            val isSelected = day == selectedDate
                            val isCurrentToday = day == today

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isCurrentToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .clickable { onSelectDate(day.toEpochDay()) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${day.dayOfMonth}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (!isToday) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onSelectDate(today.toEpochDay()) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .testTag("jump_to_today_button")
                        ) {
                            Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jump to Today", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Bulk Action Bar & Day Summary
        val markedCount = logsMap.size
        val totalCount = subjects.size
        val presentToday = dailyLogs.count { it.status == AttendanceStatus.PRESENT }
        val absentToday = dailyLogs.count { it.status == AttendanceStatus.ABSENT }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$markedCount of $totalCount classes logged",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "✓ $presentToday Present",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StatusSafe
                            )
                            Text(
                                text = "✗ $absentToday Absent",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (absentToday > 0) StatusCritical else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (subjects.isNotEmpty()) {
                        Button(
                            onClick = onMarkAllPresent,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusSafe,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            ),
                            modifier = Modifier.testTag("mark_all_present_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mark All Present",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Subject Cards with Simple Present/Absent Switch Toggle
        if (subjects.isEmpty()) {
            item {
                EmptyAttendanceState(
                    title = "No Subjects Added",
                    message = "Add your college subjects to start logging class attendance.",
                    actionLabel = "Add Subject",
                    onAction = onAddSubjectClick
                )
            }
        } else {
            items(subjects, key = { it.id }) { subject ->
                val log = logsMap[subject.id]
                DailySubjectSwitchCard(
                    subject = subject,
                    currentStatus = log?.status,
                    currentNote = log?.note,
                    onToggle = { isPresent ->
                        val targetStatus = if (isPresent) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
                        onMarkPresence(subject.id, targetStatus)
                    },
                    onClear = { onClearPresence(subject.id) },
                    onUpdateNote = { note -> onUpdateNote(subject.id, note) }
                )
            }
        }
    }

    // Material 3 DatePickerDialog for selecting past or present dates
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            onSelectDate(pickedDate.toEpochDay())
                        }
                        showDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_date_picker_button")
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("date_picker_dialog")
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun DailySubjectSwitchCard(
    subject: Subject,
    currentStatus: AttendanceStatus?,
    currentNote: String?,
    onToggle: (Boolean) -> Unit,
    onClear: () -> Unit,
    onUpdateNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMarked = currentStatus != null && currentStatus != AttendanceStatus.CANCELLED
    val isPresent = currentStatus == AttendanceStatus.PRESENT

    val cardBorderColor by animateColorAsState(
        targetValue = when (currentStatus) {
            AttendanceStatus.PRESENT -> StatusSafe.copy(alpha = 0.5f)
            AttendanceStatus.ABSENT -> StatusCritical.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        },
        label = "cardBorderColor"
    )

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_card_${subject.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Subject header info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(subject.colorHex))
                            .align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val extra = listOfNotNull(
                            subject.code.ifEmpty { null },
                            subject.room.ifEmpty { null }
                        ).joinToString(" • ")
                        if (extra.isNotEmpty()) {
                            Text(
                                text = extra,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${subject.percentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (subject.isBelowThreshold) StatusCritical else StatusSafe
                        )
                    )
                    Text(
                        text = "Goal ${subject.minThreshold.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Switch Toggle Row (Present / Absent)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (currentStatus) {
                    AttendanceStatus.PRESENT -> StatusSafeContainer.copy(alpha = 0.6f)
                    AttendanceStatus.ABSENT -> StatusCriticalContainer.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge & Label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (currentStatus) {
                            AttendanceStatus.PRESENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StatusSafe,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Present",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusSafe
                                )
                            }
                            AttendanceStatus.ABSENT -> {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = StatusCritical,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Absent",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusCritical
                                )
                            }
                            else -> {
                                Text(
                                    text = "Not recorded yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Toggle Switch & optional reset
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMarked) {
                            IconButton(
                                onClick = onClear,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("clear_attendance_${subject.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset attendance",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Switch(
                            checked = isPresent,
                            onCheckedChange = { checked ->
                                onToggle(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusSafe,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = if (currentStatus == AttendanceStatus.ABSENT) StatusCritical else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumbContent = {
                                if (isPresent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = StatusSafe
                                    )
                                } else if (currentStatus == AttendanceStatus.ABSENT) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = StatusCritical
                                    )
                                }
                            },
                            modifier = Modifier.testTag("attendance_switch_${subject.id}")
                        )
                    }
                }
            }

            // Optional Absence Reason / Notes Section
            Spacer(modifier = Modifier.height(10.dp))

            var isEditingNote by remember(currentNote) { mutableStateOf(!currentNote.isNullOrBlank()) }
            var noteInput by remember(currentNote) { mutableStateOf(currentNote ?: "") }
            val quickReasons = listOf("Medical", "Event", "Sick", "Transit")

            if (currentNote.isNullOrBlank() && !isEditingNote) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentStatus == AttendanceStatus.ABSENT) "Add reason for absence:" else "Optional note / reason:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "+ Add Reason / Note",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isEditingNote = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("add_note_button_${subject.id}")
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reason / Note",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!currentNote.isNullOrBlank()) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            noteInput = ""
                                            isEditingNote = false
                                            onUpdateNote("")
                                        }
                                        .padding(4.dp)
                                        .testTag("clear_note_button_${subject.id}")
                                )
                            } else {
                                Text(
                                    text = "Hide",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { isEditingNote = false }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = {
                            noteInput = it
                            onUpdateNote(it)
                        },
                        placeholder = {
                            Text(
                                "e.g. Medical, College event, Sick leave...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendance_note_input_${subject.id}")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick suggestion chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickReasons.forEach { reason ->
                            SuggestionChip(
                                onClick = {
                                    noteInput = reason
                                    onUpdateNote(reason)
                                },
                                label = { Text(reason, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("chip_${reason.lowercase()}_${subject.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
