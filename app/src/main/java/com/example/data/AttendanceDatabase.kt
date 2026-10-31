package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttendanceConverters {
    @TypeConverter
    fun fromStatus(status: AttendanceStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): AttendanceStatus = runCatching {
        AttendanceStatus.valueOf(value)
    }.getOrDefault(AttendanceStatus.PRESENT)
}

@Database(
    entities = [Subject::class, AttendanceLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AttendanceConverters::class)
abstract class AttendanceDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getDatabase(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_tracker.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial realistic college subjects so first-time users immediately see the dashboard
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        seedInitialData(database.attendanceDao())
                    }
                }
            }

            private suspend fun seedInitialData(dao: AttendanceDao) {
                val now = System.currentTimeMillis()
                val todayEpoch = java.time.LocalDate.now().toEpochDay()

                val sampleSubjects = listOf(
                    Subject(
                        id = 1,
                        name = "Data Structures & Algorithms",
                        code = "CS201",
                        minThreshold = 75.0f,
                        attendedClasses = 19,
                        totalClasses = 22,
                        colorHex = 0xFF2563EB, // Sapphire Blue
                        room = "Room 302",
                        scheduleDays = "Mon, Wed, Fri",
                        createdAt = now
                    ),
                    Subject(
                        id = 2,
                        name = "Operating Systems",
                        code = "CS204",
                        minThreshold = 75.0f,
                        attendedClasses = 13,
                        totalClasses = 20, // 65% - below threshold! Triggers low attendance alert
                        colorHex = 0xFFEF4444, // Red
                        room = "Lecture Hall B",
                        scheduleDays = "Tue, Thu",
                        createdAt = now
                    ),
                    Subject(
                        id = 3,
                        name = "Database Management Systems",
                        code = "CS206",
                        minThreshold = 80.0f,
                        attendedClasses = 17,
                        totalClasses = 21, // ~81% - close to 80% threshold
                        colorHex = 0xFFF59E0B, // Amber
                        room = "Lab 4",
                        scheduleDays = "Mon, Wed",
                        createdAt = now
                    ),
                    Subject(
                        id = 4,
                        name = "Discrete Mathematics",
                        code = "MA202",
                        minThreshold = 75.0f,
                        attendedClasses = 24,
                        totalClasses = 25, // 96% - Safe!
                        colorHex = 0xFF10B981, // Emerald Green
                        room = "Seminar Hall 1",
                        scheduleDays = "Mon, Tue, Thu",
                        createdAt = now
                    )
                )

                for (s in sampleSubjects) {
                    dao.insertSubject(s)
                }

                // Add sample log for today
                dao.insertOrUpdateLog(
                    AttendanceLog(
                        subjectId = 1,
                        dateEpochDay = todayEpoch,
                        status = AttendanceStatus.PRESENT,
                        note = "Graph Traversal Lecture"
                    )
                )
            }
        }
    }
}
