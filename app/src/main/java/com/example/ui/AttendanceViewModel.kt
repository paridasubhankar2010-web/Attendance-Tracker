package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceLog
import com.example.data.AttendanceRepository
import com.example.data.AttendanceStatus
import com.example.data.Subject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OverallAttendanceStats(
    val totalAttended: Int = 0,
    val totalClasses: Int = 0,
    val overallPercentage: Double = 0.0,
    val safeCount: Int = 0,
    val atRiskCount: Int = 0,
    val nearRiskCount: Int = 0,
    val atRiskSubjects: List<Subject> = emptyList(),
    val bestSubject: Subject? = null,
    val worstSubject: Subject? = null
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository
    private val sessionManager = com.example.data.UserSessionManager(application)
    val userProfile: StateFlow<com.example.data.UserProfile> = sessionManager.userProfile

    private val _isProfileDialogOpen = MutableStateFlow(false)
    val isProfileDialogOpen: StateFlow<Boolean> = _isProfileDialogOpen.asStateFlow()

    init {
        val database = AttendanceDatabase.getDatabase(application)
        repository = AttendanceRepository(database.attendanceDao())
    }

    fun loginUser(name: String, institution: String, preparingForCompetitive: Boolean, targetExam: String = "") {
        sessionManager.saveProfile(name, institution, preparingForCompetitive, targetExam)
        _userMessage.value = "Welcome, ${name.trim()}!"
    }

    fun logoutUser() {
        sessionManager.logout()
        _isProfileDialogOpen.value = false
        _userMessage.value = "Logged out"
    }

    fun openProfileDialog() {
        _isProfileDialogOpen.value = true
    }

    fun closeProfileDialog() {
        _isProfileDialogOpen.value = false
    }

    val allSubjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDateEpoch = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedDateEpoch: StateFlow<Long> = _selectedDateEpoch.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyLogs: StateFlow<List<AttendanceLog>> = _selectedDateEpoch
        .flatMapLatest { epoch -> repository.getLogsForDate(epoch) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val overallStats: StateFlow<OverallAttendanceStats> = allSubjects
        .combine(_selectedDateEpoch) { subjects, _ ->
            if (subjects.isEmpty()) {
                OverallAttendanceStats()
            } else {
                val totalAttended = subjects.sumOf { it.attendedClasses }
                val totalClasses = subjects.sumOf { it.totalClasses }
                val percentage = if (totalClasses > 0) {
                    (totalAttended.toDouble() / totalClasses) * 100.0
                } else 0.0

                val atRisk = subjects.filter { it.isBelowThreshold }
                val nearRisk = subjects.filter { it.isNearThreshold }
                val safe = subjects.filter { !it.isBelowThreshold && !it.isNearThreshold }

                val nonZeroSubjects = subjects.filter { it.totalClasses > 0 }
                val best = nonZeroSubjects.maxByOrNull { it.percentage }
                val worst = nonZeroSubjects.minByOrNull { it.percentage }

                OverallAttendanceStats(
                    totalAttended = totalAttended,
                    totalClasses = totalClasses,
                    overallPercentage = percentage,
                    safeCount = safe.size,
                    atRiskCount = atRisk.size,
                    nearRiskCount = nearRisk.size,
                    atRiskSubjects = atRisk,
                    bestSubject = best,
                    worstSubject = worst
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OverallAttendanceStats()
        )

    // UI State for Dialogs and Sheet
    private val _selectedSubjectForDetail = MutableStateFlow<Subject?>(null)
    val selectedSubjectForDetail: StateFlow<Subject?> = _selectedSubjectForDetail.asStateFlow()

    private val _isAddEditSubjectOpen = MutableStateFlow(false)
    val isAddEditSubjectOpen: StateFlow<Boolean> = _isAddEditSubjectOpen.asStateFlow()

    private val _subjectToEdit = MutableStateFlow<Subject?>(null)
    val subjectToEdit: StateFlow<Subject?> = _subjectToEdit.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterTab = MutableStateFlow(0) // 0 = All, 1 = At Risk, 2 = Safe
    val filterTab: StateFlow<Int> = _filterTab.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun onSelectDate(epochDay: Long) {
        _selectedDateEpoch.value = epochDay
    }

    fun markDailyPresence(subjectId: Long, status: AttendanceStatus, note: String = "") {
        viewModelScope.launch {
            repository.markAttendance(subjectId, _selectedDateEpoch.value, status, note)
            val subject = repository.getSubjectById(subjectId)
            val name = subject?.name ?: "Subject"
            _userMessage.value = when (status) {
                AttendanceStatus.PRESENT -> "Marked Present for $name"
                AttendanceStatus.ABSENT -> "Marked Absent for $name"
                AttendanceStatus.CANCELLED -> "Marked Cancelled for $name"
            }
        }
    }

    fun clearDailyPresence(subjectId: Long) {
        viewModelScope.launch {
            repository.clearAttendanceForDate(subjectId, _selectedDateEpoch.value)
            _userMessage.value = "Attendance record cleared"
        }
    }

    fun updateDailyPresenceNote(subjectId: Long, note: String) {
        viewModelScope.launch {
            repository.updateLogNote(subjectId, _selectedDateEpoch.value, note)
            if (note.isNotBlank()) {
                val subject = repository.getSubjectById(subjectId)
                val name = subject?.name ?: "Subject"
                _userMessage.value = "Saved note for $name"
            }
        }
    }

    fun markAllPresentForDate() {
        val currentSubjects = allSubjects.value
        if (currentSubjects.isEmpty()) return
        viewModelScope.launch {
            repository.markAllPresentForDate(_selectedDateEpoch.value, currentSubjects)
            _userMessage.value = "Marked all ${currentSubjects.size} subjects Present"
        }
    }

    fun quickAddClass(subjectId: Long, isPresent: Boolean) {
        viewModelScope.launch {
            repository.quickAddClass(subjectId, isPresent)
            val action = if (isPresent) "Present (+1)" else "Absent (+1)"
            _userMessage.value = "Logged $action"
        }
    }

    fun quickRemoveClass(subjectId: Long, wasPresent: Boolean) {
        viewModelScope.launch {
            repository.quickRemoveClass(subjectId, wasPresent)
            _userMessage.value = "Undone 1 class"
        }
    }

    fun updateManualAttendance(subjectId: Long, attended: Int, total: Int) {
        viewModelScope.launch {
            repository.setManualCounts(subjectId, attended, total)
            _userMessage.value = "Attendance counts updated"
            // Refresh selected subject if in detail
            val updated = repository.getSubjectById(subjectId)
            _selectedSubjectForDetail.value = updated
        }
    }

    fun saveSubject(
        id: Long = 0,
        name: String,
        code: String,
        minThreshold: Float,
        attendedClasses: Int,
        totalClasses: Int,
        colorHex: Long,
        room: String,
        scheduleDays: String,
        notes: String
    ) {
        viewModelScope.launch {
            val subject = Subject(
                id = id,
                name = name.trim(),
                code = code.trim().uppercase(),
                minThreshold = minThreshold,
                attendedClasses = attendedClasses,
                totalClasses = totalClasses,
                colorHex = colorHex,
                room = room.trim(),
                scheduleDays = scheduleDays.trim(),
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.insertSubject(subject)
                _userMessage.value = "Added subject '${subject.name}'"
            } else {
                repository.updateSubject(subject)
                _userMessage.value = "Updated '${subject.name}'"
                _selectedSubjectForDetail.value = subject
            }
            closeAddEditDialog()
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _selectedSubjectForDetail.value = null
            _userMessage.value = "Deleted subject '${subject.name}'"
        }
    }

    fun openSubjectDetail(subject: Subject?) {
        _selectedSubjectForDetail.value = subject
    }

    fun openAddSubjectDialog() {
        _subjectToEdit.value = null
        _isAddEditSubjectOpen.value = true
    }

    fun openEditSubjectDialog(subject: Subject) {
        _subjectToEdit.value = subject
        _isAddEditSubjectOpen.value = true
    }

    fun closeAddEditDialog() {
        _isAddEditSubjectOpen.value = false
        _subjectToEdit.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterTab(tab: Int) {
        _filterTab.value = tab
    }

    // Export State
    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    private val _exportTargetSubject = MutableStateFlow<Subject?>(null)
    val exportTargetSubject: StateFlow<Subject?> = _exportTargetSubject.asStateFlow()

    fun openExportDialog(subject: Subject? = null) {
        _exportTargetSubject.value = subject
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
        _exportTargetSubject.value = null
    }

    suspend fun generateExportData(targetSubject: Subject?): Pair<String, String> {
        val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        return if (targetSubject != null) {
            val logs = repository.getLogsForSubjectList(targetSubject.id)
            val code = targetSubject.code.ifEmpty { "subject" }.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "attendance_${code}_$todayStr.csv"
            val content = com.example.util.CsvExporter.generateSubjectCsv(targetSubject, logs)
            Pair(fileName, content)
        } else {
            val subjects = allSubjects.value
            val logs = repository.getAllLogsList()
            val fileName = "semester_attendance_$todayStr.csv"
            val content = com.example.util.CsvExporter.generateSemesterCsv(subjects, logs)
            Pair(fileName, content)
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }
}
