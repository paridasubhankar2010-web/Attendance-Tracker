package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AttendanceViewModel
import com.example.ui.dailylog.DailyLogScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.export.ExportAttendanceDialog
import com.example.ui.login.LoginScreen
import com.example.ui.profile.UserProfileDialog
import com.example.ui.subjects.AddEditSubjectDialog
import com.example.ui.subjects.SubjectDetailSheet
import com.example.ui.subjects.SubjectsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusCritical

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AttendanceTrackerApp()
            }
        }
    }
}

@Composable
fun AttendanceTrackerApp(
    viewModel: AttendanceViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val subjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val stats by viewModel.overallStats.collectAsStateWithLifecycle()
    val selectedDateEpoch by viewModel.selectedDateEpoch.collectAsStateWithLifecycle()
    val dailyLogs by viewModel.dailyLogs.collectAsStateWithLifecycle()
    val selectedSubjectForDetail by viewModel.selectedSubjectForDetail.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditSubjectOpen.collectAsStateWithLifecycle()
    val subjectToEdit by viewModel.subjectToEdit.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterTab by viewModel.filterTab.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsStateWithLifecycle()
    val exportTargetSubject by viewModel.exportTargetSubject.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isProfileDialogOpen by viewModel.isProfileDialogOpen.collectAsStateWithLifecycle()

    // If user is not logged in, present the Login/Onboarding Screen
    if (!userProfile.isLoggedIn) {
        LoginScreen(
            onLogin = { name, inst, prep, exam ->
                viewModel.loginUser(name, inst, prep, exam)
            }
        )
        return
    }

    // Handle back button on sub-screens
    BackHandler(enabled = isProfileDialogOpen || isExportDialogOpen || selectedSubjectForDetail != null || selectedTab != 0) {
        if (isProfileDialogOpen) {
            viewModel.closeProfileDialog()
        } else if (isExportDialogOpen) {
            viewModel.closeExportDialog()
        } else if (selectedSubjectForDetail != null) {
            viewModel.openSubjectDetail(null)
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    // Show Snackbars for feedback
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                // Dashboard Tab
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        if (stats.atRiskCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = StatusCritical) {
                                    Text("${stats.atRiskCount}")
                                }
                            }) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                    contentDescription = "Dashboard"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        }
                    },
                    label = { Text("Dashboard", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_dashboard")
                )

                // Daily Log Tab
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.CalendarToday else Icons.Outlined.CalendarToday,
                            contentDescription = "Daily Log"
                        )
                    },
                    label = { Text("Daily Log", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_daily_log")
                )

                // Subjects Tab
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.School else Icons.Outlined.School,
                            contentDescription = "Subjects"
                        )
                    },
                    label = { Text("Subjects", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_subjects")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        stats = stats,
                        subjects = subjects,
                        userProfile = userProfile,
                        onSelectSubject = { viewModel.openSubjectDetail(it) },
                        onQuickAdd = { id, isPresent -> viewModel.quickAddClass(id, isPresent) },
                        onAddSubjectClick = { viewModel.openAddSubjectDialog() },
                        onExportClick = { viewModel.openExportDialog(null) },
                        onProfileClick = { viewModel.openProfileDialog() }
                    )
                    1 -> DailyLogScreen(
                        subjects = subjects,
                        selectedDateEpoch = selectedDateEpoch,
                        dailyLogs = dailyLogs,
                        onSelectDate = { viewModel.onSelectDate(it) },
                        onMarkPresence = { id, status -> viewModel.markDailyPresence(id, status) },
                        onClearPresence = { id -> viewModel.clearDailyPresence(id) },
                        onUpdateNote = { id, note -> viewModel.updateDailyPresenceNote(id, note) },
                        onMarkAllPresent = { viewModel.markAllPresentForDate() },
                        onAddSubjectClick = { viewModel.openAddSubjectDialog() }
                    )
                    2 -> SubjectsScreen(
                        subjects = subjects,
                        searchQuery = searchQuery,
                        filterTab = filterTab,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setFilterTab(it) },
                        onSelectSubject = { viewModel.openSubjectDetail(it) },
                        onQuickAdd = { id, isPresent -> viewModel.quickAddClass(id, isPresent) },
                        onAddSubjectClick = { viewModel.openAddSubjectDialog() }
                    )
                }
            }
        }
    }

    // Subject Detail Bottom Sheet with "What-If" Simulator
    if (selectedSubjectForDetail != null) {
        SubjectDetailSheet(
            subject = selectedSubjectForDetail!!,
            onDismiss = { viewModel.openSubjectDetail(null) },
            onEdit = { subject ->
                viewModel.openSubjectDetail(null)
                viewModel.openEditSubjectDialog(subject)
            },
            onDelete = { subject -> viewModel.deleteSubject(subject) },
            onExportSubject = { subject ->
                viewModel.openExportDialog(subject)
            },
            onUpdateCounts = { id, attended, total ->
                viewModel.updateManualAttendance(id, attended, total)
            }
        )
    }

    // Add or Edit Subject Dialog
    if (isAddEditOpen) {
        AddEditSubjectDialog(
            subjectToEdit = subjectToEdit,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { id, name, code, threshold, attended, total, colorHex, room, scheduleDays, notes ->
                viewModel.saveSubject(
                    id = id,
                    name = name,
                    code = code,
                    minThreshold = threshold,
                    attendedClasses = attended,
                    totalClasses = total,
                    colorHex = colorHex,
                    room = room,
                    scheduleDays = scheduleDays,
                    notes = notes
                )
            }
        )
    }

    // Export Attendance CSV Dialog
    if (isExportDialogOpen) {
        ExportAttendanceDialog(
            initialSubject = exportTargetSubject,
            subjects = subjects,
            onDismiss = { viewModel.closeExportDialog() },
            onGenerateCsv = { targetSub -> viewModel.generateExportData(targetSub) }
        )
    }

    // Student Profile Dialog
    if (isProfileDialogOpen) {
        UserProfileDialog(
            userProfile = userProfile,
            onDismiss = { viewModel.closeProfileDialog() },
            onUpdateProfile = { name, inst, prep, exam ->
                viewModel.loginUser(name, inst, prep, exam)
            },
            onLogout = { viewModel.logoutUser() }
        )
    }
}
