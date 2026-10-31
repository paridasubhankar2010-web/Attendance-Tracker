package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class AttendanceRepository(private val dao: AttendanceDao) {

    val allSubjects: Flow<List<Subject>> = dao.getAllSubjects()

    fun getLogsForDate(dateEpochDay: Long): Flow<List<AttendanceLog>> =
        dao.getLogsForDate(dateEpochDay)

    fun getLogsForSubject(subjectId: Long): Flow<List<AttendanceLog>> =
        dao.getLogsForSubject(subjectId)

    suspend fun getSubjectById(id: Long): Subject? = dao.getSubjectById(id)

    suspend fun getAllLogsList(): List<AttendanceLog> = dao.getAllLogsList()

    suspend fun getLogsForSubjectList(subjectId: Long): List<AttendanceLog> = dao.getLogsForSubjectList(subjectId)

    suspend fun insertSubject(subject: Subject): Long = dao.insertSubject(subject)

    suspend fun updateSubject(subject: Subject) = dao.updateSubject(subject)

    suspend fun deleteSubject(subject: Subject) = dao.deleteSubject(subject)

    /**
     * Mark attendance for a subject on a specific date.
     * Properly updates both the attendance_log and the cumulative attended/total counters in the subject.
     */
    suspend fun markAttendance(
        subjectId: Long,
        dateEpochDay: Long,
        newStatus: AttendanceStatus,
        note: String = ""
    ) {
        val subject = dao.getSubjectById(subjectId) ?: return
        val existingLog = dao.getLogForSubjectAndDate(subjectId, dateEpochDay)

        var newAttended = subject.attendedClasses
        var newTotal = subject.totalClasses

        if (existingLog == null) {
            when (newStatus) {
                AttendanceStatus.PRESENT -> {
                    newAttended += 1
                    newTotal += 1
                }
                AttendanceStatus.ABSENT -> {
                    newTotal += 1
                }
                AttendanceStatus.CANCELLED -> {
                    // Class cancelled, doesn't increment total or attended
                }
            }
        } else {
            val oldStatus = existingLog.status
            if (oldStatus == newStatus) {
                // If status is same, check if note changed
                if (existingLog.note != note) {
                    dao.insertOrUpdateLog(
                        existingLog.copy(
                            note = note,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                return
            }
            // Revert old effect
            when (oldStatus) {
                AttendanceStatus.PRESENT -> {
                    newAttended -= 1
                    newTotal -= 1
                }
                AttendanceStatus.ABSENT -> {
                    newTotal -= 1
                }
                AttendanceStatus.CANCELLED -> {
                    // Nothing to revert
                }
            }
            // Apply new effect
            when (newStatus) {
                AttendanceStatus.PRESENT -> {
                    newAttended += 1
                    newTotal += 1
                }
                AttendanceStatus.ABSENT -> {
                    newTotal += 1
                }
                AttendanceStatus.CANCELLED -> {
                    // No count addition
                }
            }
        }

        newAttended = max(0, newAttended)
        newTotal = max(0, newTotal)
        if (newAttended > newTotal) newAttended = newTotal

        dao.updateSubjectCounts(subjectId, newAttended, newTotal)
        dao.insertOrUpdateLog(
            AttendanceLog(
                id = existingLog?.id ?: 0,
                subjectId = subjectId,
                dateEpochDay = dateEpochDay,
                status = newStatus,
                note = note,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Clear the log for a given date and adjust the subject count.
     */
    suspend fun clearAttendanceForDate(subjectId: Long, dateEpochDay: Long) {
        val subject = dao.getSubjectById(subjectId) ?: return
        val existingLog = dao.getLogForSubjectAndDate(subjectId, dateEpochDay) ?: return

        var newAttended = subject.attendedClasses
        var newTotal = subject.totalClasses

        when (existingLog.status) {
            AttendanceStatus.PRESENT -> {
                newAttended -= 1
                newTotal -= 1
            }
            AttendanceStatus.ABSENT -> {
                newTotal -= 1
            }
            AttendanceStatus.CANCELLED -> {
                // no change
            }
        }

        newAttended = max(0, newAttended)
        newTotal = max(0, newTotal)
        dao.updateSubjectCounts(subjectId, newAttended, newTotal)
        dao.deleteLogForSubjectAndDate(subjectId, dateEpochDay)
    }

    /**
     * Update or attach a note to an attendance record for a specific date
     */
    suspend fun updateLogNote(subjectId: Long, dateEpochDay: Long, note: String) {
        val existingLog = dao.getLogForSubjectAndDate(subjectId, dateEpochDay)
        if (existingLog != null) {
            dao.insertOrUpdateLog(
                existingLog.copy(
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            // Default to ABSENT when student enters an absence reason before toggling
            markAttendance(subjectId, dateEpochDay, AttendanceStatus.ABSENT, note)
        }
    }

    /**
     * Quick 1-tap increment on Dashboard (+1 Present or +1 Absent)
     */
    suspend fun quickAddClass(subjectId: Long, isPresent: Boolean) {
        val subject = dao.getSubjectById(subjectId) ?: return
        val newAttended = if (isPresent) subject.attendedClasses + 1 else subject.attendedClasses
        val newTotal = subject.totalClasses + 1
        dao.updateSubjectCounts(subjectId, newAttended, newTotal)
    }

    /**
     * Quick undo/decrement
     */
    suspend fun quickRemoveClass(subjectId: Long, wasPresent: Boolean) {
        val subject = dao.getSubjectById(subjectId) ?: return
        if (subject.totalClasses <= 0) return
        val newAttended = if (wasPresent) max(0, subject.attendedClasses - 1) else subject.attendedClasses
        val newTotal = max(0, subject.totalClasses - 1)
        val clampedAttended = if (newAttended > newTotal) newTotal else newAttended
        dao.updateSubjectCounts(subjectId, clampedAttended, newTotal)
    }

    /**
     * Set explicit counts from manual editor
     */
    suspend fun setManualCounts(subjectId: Long, attended: Int, total: Int) {
        val clampedTotal = max(0, total)
        val clampedAttended = max(0, minOf(attended, clampedTotal))
        dao.updateSubjectCounts(subjectId, clampedAttended, clampedTotal)
    }

    /**
     * Bulk mark all provided subjects as Present for a specific date
     */
    suspend fun markAllPresentForDate(dateEpochDay: Long, subjects: List<Subject>) {
        for (subject in subjects) {
            markAttendance(subject.id, dateEpochDay, AttendanceStatus.PRESENT)
        }
    }
}
