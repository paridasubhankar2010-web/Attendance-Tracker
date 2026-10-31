package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    CANCELLED // Doesn't affect total or attended
}

@Entity(
    tableName = "attendance_logs",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("subjectId"),
        Index(value = ["subjectId", "dateEpochDay"], unique = true)
    ]
)
data class AttendanceLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dateEpochDay: Long,
    val status: AttendanceStatus,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
