package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Subject Operations ---
    @Query("SELECT * FROM subjects WHERE archived = 0 ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("UPDATE subjects SET attendedClasses = :attended, totalClasses = :total WHERE id = :subjectId")
    suspend fun updateSubjectCounts(subjectId: Long, attended: Int, total: Int)

    // --- Attendance Log Operations ---
    @Query("SELECT * FROM attendance_logs WHERE dateEpochDay = :dateEpochDay")
    fun getLogsForDate(dateEpochDay: Long): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId ORDER BY dateEpochDay DESC")
    fun getLogsForSubject(subjectId: Long): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs ORDER BY dateEpochDay DESC")
    fun getAllLogs(): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs ORDER BY dateEpochDay DESC")
    suspend fun getAllLogsList(): List<AttendanceLog>

    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId ORDER BY dateEpochDay DESC")
    suspend fun getLogsForSubjectList(subjectId: Long): List<AttendanceLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: AttendanceLog): Long

    @Query("SELECT * FROM attendance_logs WHERE subjectId = :subjectId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getLogForSubjectAndDate(subjectId: Long, dateEpochDay: Long): AttendanceLog?

    @Query("DELETE FROM attendance_logs WHERE subjectId = :subjectId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteLogForSubjectAndDate(subjectId: Long, dateEpochDay: Long)

    @Delete
    suspend fun deleteLog(log: AttendanceLog)
}
