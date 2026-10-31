package com.example.ui.subjects

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.EmptyAttendanceState
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSafe

@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    searchQuery: String,
    filterTab: Int,
    onSearchChange: (String) -> Unit,
    onFilterChange: (Int) -> Unit,
    onSelectSubject: (Subject) -> Unit,
    onQuickAdd: (subjectId: Long, isPresent: Boolean) -> Unit,
    onAddSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSubjects = subjects.filter { subject ->
        val matchesQuery = subject.name.contains(searchQuery, ignoreCase = true) ||
                subject.code.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterTab) {
            1 -> subject.isBelowThreshold // At Risk
            2 -> !subject.isBelowThreshold // Safe / On Track
            else -> true
        }
        matchesQuery && matchesFilter
    }

    val atRiskCount = subjects.count { it.isBelowThreshold }
    val safeCount = subjects.count { !it.isBelowThreshold }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("subjects_screen"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Subject Management",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track thresholds, simulate attendance, and manage criteria",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search subjects or course code...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.clickable { onSearchChange("") }
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_subject_input")
                )
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTab == 0,
                        onClick = { onFilterChange(0) },
                        label = { Text("All (${subjects.size})") },
                        modifier = Modifier.testTag("filter_all_chip")
                    )
                    FilterChip(
                        selected = filterTab == 1,
                        onClick = { onFilterChange(1) },
                        label = { Text("At Risk ($atRiskCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("filter_risk_chip")
                    )
                    FilterChip(
                        selected = filterTab == 2,
                        onClick = { onFilterChange(2) },
                        label = { Text("On Track ($safeCount)") },
                        modifier = Modifier.testTag("filter_safe_chip")
                    )
                }
            }

            // Subject Cards
            if (filteredSubjects.isEmpty()) {
                item {
                    EmptyAttendanceState(
                        title = if (searchQuery.isNotEmpty()) "No Matching Subjects" else "No Subjects in this Category",
                        message = if (searchQuery.isNotEmpty()) "Try searching with a different keyword." else "Add a subject or switch filters.",
                        actionLabel = if (subjects.isEmpty()) "Add Subject" else null,
                        onAction = if (subjects.isEmpty()) onAddSubjectClick else null
                    )
                }
            } else {
                items(filteredSubjects, key = { it.id }) { subject ->
                    SubjectListItemCard(
                        subject = subject,
                        onClick = { onSelectSubject(subject) },
                        onQuickPresent = { onQuickAdd(subject.id, true) },
                        onQuickAbsent = { onQuickAdd(subject.id, false) }
                    )
                }
            }
        }

        // Add Subject Floating Action Button
        FloatingActionButton(
            onClick = onAddSubjectClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
                .testTag("add_subject_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
        }
    }
}

@Composable
fun SubjectListItemCard(
    subject: Subject,
    onClick: () -> Unit,
    onQuickPresent: () -> Unit,
    onQuickAbsent: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("subject_card_${subject.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(subject.colorHex))
                            .align(Alignment.CenterVertically)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (subject.code.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = subject.code,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (subject.room.isNotEmpty()) {
                                Text(
                                    text = subject.room,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AttendancePercentageCircle(
                    percentage = subject.percentage,
                    minThreshold = subject.minThreshold,
                    size = 58.dp,
                    strokeWidth = 6.dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Threshold details and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    isBelowThreshold = subject.isBelowThreshold,
                    isNearThreshold = subject.isNearThreshold
                )

                Text(
                    text = "Goal: ${subject.minThreshold.toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bunk advice
            BunkAdviceChip(
                canMissCount = subject.safeToMissCount,
                needAttendCount = subject.classesNeededCount,
                isBelowThreshold = subject.isBelowThreshold,
                minThreshold = subject.minThreshold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${subject.attendedClasses} / ${subject.totalClasses} classes attended",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onQuickAbsent,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.testTag("subject_quick_absent_${subject.id}")
                    ) {
                        Text("Absent", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onQuickPresent,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusSafe,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.testTag("subject_quick_present_${subject.id}")
                    ) {
                        Text("Present", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
