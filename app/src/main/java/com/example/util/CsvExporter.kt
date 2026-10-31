package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AttendanceLog
import com.example.data.AttendanceStatus
import com.example.data.Subject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExporter {

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Generates a comprehensive CSV for the entire semester:
     * 1. Overview header
     * 2. Subject Summary table (Attended, Total, %, Threshold, Safe Bunks, Catch-up)
     * 3. Chronological Daily Attendance ledger
     */
    fun generateSemesterCsv(subjects: List<Subject>, logs: List<AttendanceLog>): String {
        val sb = StringBuilder()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val totalAttended = subjects.sumOf { it.attendedClasses }
        val totalClasses = subjects.sumOf { it.totalClasses }
        val overallPct = if (totalClasses > 0) (totalAttended.toDouble() / totalClasses) * 100.0 else 0.0

        sb.appendLine("# COLLEGE ATTENDANCE TRACKER - SEMESTER ATTENDANCE REPORT")
        sb.appendLine("# Report Generated: $todayStr")
        sb.appendLine("# Overall Attendance: ${String.format(Locale.US, "%.1f%%", overallPct)} ($totalAttended / $totalClasses classes)")
        sb.appendLine()

        // 1. Subject Summary Table
        sb.appendLine("=== SUBJECT SUMMARY ===")
        sb.appendLine("Subject Name,Course Code,Attended,Total Conducted,Attendance %,Min Goal %,Status,Safe To Bunk,Classes Needed,Room,Schedule Days")

        val sortedSubjects = subjects.sortedBy { it.name }
        val subjectMap = sortedSubjects.associateBy { it.id }

        for (sub in sortedSubjects) {
            val status = when {
                sub.isBelowThreshold -> "CRITICAL (Below Goal)"
                sub.isNearThreshold -> "WARNING (Borderline)"
                else -> "ON TRACK (Safe)"
            }
            sb.appendLine(
                listOf(
                    escapeCsv(sub.name),
                    escapeCsv(sub.code),
                    sub.attendedClasses.toString(),
                    sub.totalClasses.toString(),
                    String.format(Locale.US, "%.1f%%", sub.percentage),
                    "${sub.minThreshold.toInt()}%",
                    escapeCsv(status),
                    sub.safeToMissCount.toString(),
                    sub.classesNeededCount.toString(),
                    escapeCsv(sub.room),
                    escapeCsv(sub.scheduleDays)
                ).joinToString(",")
            )
        }
        sb.appendLine()

        // 2. Detailed Daily Logs Ledger
        sb.appendLine("=== DAILY ATTENDANCE LEDGER ===")
        sb.appendLine("Date,Day of Week,Course Code,Subject Name,Status,Notes")

        val sortedLogs = logs.sortedByDescending { it.dateEpochDay }
        for (log in sortedLogs) {
            val subject = subjectMap[log.subjectId]
            val date = LocalDate.ofEpochDay(log.dateEpochDay)
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val statusStr = when (log.status) {
                AttendanceStatus.PRESENT -> "PRESENT"
                AttendanceStatus.ABSENT -> "ABSENT"
                AttendanceStatus.CANCELLED -> "CANCELLED / HOLIDAY"
            }

            sb.appendLine(
                listOf(
                    dateStr,
                    dayName,
                    escapeCsv(subject?.code ?: ""),
                    escapeCsv(subject?.name ?: "Unknown Subject"),
                    statusStr,
                    escapeCsv(log.note)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    /**
     * Generates a focused CSV for a specific subject with summary and all logged dates.
     */
    fun generateSubjectCsv(subject: Subject, logs: List<AttendanceLog>): String {
        val sb = StringBuilder()
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        sb.appendLine("# ATTENDANCE TRACKER - SUBJECT REPORT")
        sb.appendLine("# Subject: ${subject.name} (${subject.code})")
        sb.appendLine("# Generated: $todayStr")
        sb.appendLine("# Attendance: ${String.format(Locale.US, "%.1f%%", subject.percentage)} (${subject.attendedClasses} / ${subject.totalClasses} classes)")
        sb.appendLine("# Minimum Goal: ${subject.minThreshold.toInt()}%")
        val advice = if (subject.isBelowThreshold) {
            "CRITICAL - Must attend next ${subject.classesNeededCount} consecutive classes"
        } else {
            "SAFE - Can bunk ${subject.safeToMissCount} classes safely"
        }
        sb.appendLine("# Guidance: $advice")
        if (subject.room.isNotEmpty() || subject.scheduleDays.isNotEmpty()) {
            sb.appendLine("# Room: ${subject.room} | Schedule: ${subject.scheduleDays}")
        }
        sb.appendLine()

        sb.appendLine("Date,Day of Week,Status,Notes")
        val sortedLogs = logs.sortedByDescending { it.dateEpochDay }
        for (log in sortedLogs) {
            val date = LocalDate.ofEpochDay(log.dateEpochDay)
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val statusStr = when (log.status) {
                AttendanceStatus.PRESENT -> "PRESENT"
                AttendanceStatus.ABSENT -> "ABSENT"
                AttendanceStatus.CANCELLED -> "CANCELLED / HOLIDAY"
            }

            sb.appendLine(
                listOf(
                    dateStr,
                    dayName,
                    statusStr,
                    escapeCsv(log.note)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    /**
     * Saves CSV content to an application cache file and returns a shareable FileProvider URI.
     */
    fun getShareableUri(context: Context, fileName: String, csvContent: String): Uri {
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
    }

    /**
     * Writes CSV string directly to the user-selected destination URI (via SAF CreateDocument).
     */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Launches the system Chooser to share the CSV via Email, Messaging, or Drive.
     */
    fun launchShareIntent(context: Context, fileName: String, csvContent: String, shareTitle: String) {
        val uri = getShareableUri(context, fileName, csvContent)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
            putExtra(Intent.EXTRA_TEXT, "Here is the college attendance report exported from Attendance Tracker.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Attendance CSV"))
    }
}
