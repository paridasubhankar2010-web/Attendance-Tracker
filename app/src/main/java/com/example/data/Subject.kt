package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.ceil
import kotlin.math.floor

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val minThreshold: Float = 75.0f,
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0,
    val colorHex: Long = 0xFF2563EB,
    val room: String = "",
    val scheduleDays: String = "Mon, Wed, Fri",
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val percentage: Double
        get() = if (totalClasses <= 0) 0.0 else (attendedClasses.toDouble() / totalClasses) * 100.0

    val isBelowThreshold: Boolean
        get() = totalClasses > 0 && percentage < minThreshold

    val isNearThreshold: Boolean
        get() = totalClasses > 0 && percentage >= minThreshold && percentage < (minThreshold + 5.0f)

    /**
     * Number of future classes the student can miss (attend 0 out of X)
     * while keeping attendance >= minThreshold.
     */
    val safeToMissCount: Int
        get() {
            if (totalClasses <= 0 || percentage < minThreshold) return 0
            val t = minThreshold / 100.0
            if (t <= 0.0) return 99
            val maxTotal = attendedClasses / t
            val canMiss = floor(maxTotal - totalClasses).toInt()
            return if (canMiss > 0) canMiss else 0
        }

    /**
     * Number of consecutive future classes the student must attend
     * to reach minThreshold.
     */
    val classesNeededCount: Int
        get() {
            if (totalClasses <= 0 || percentage >= minThreshold) return 0
            val t = minThreshold / 100.0
            if (t >= 1.0) return 99
            val needed = ceil((t * totalClasses - attendedClasses) / (1.0 - t)).toInt()
            return if (needed > 0) needed else 0
        }
}
