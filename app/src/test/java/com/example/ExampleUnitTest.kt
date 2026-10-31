package com.example

import com.example.data.AttendanceLog
import com.example.data.AttendanceStatus
import com.example.data.Subject
import com.example.util.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {

  @Test
  fun testGenerateSemesterCsv() {
    val subjects = listOf(
      Subject(
        id = 1,
        name = "Computer Networks",
        code = "CS301",
        minThreshold = 75.0f,
        attendedClasses = 15,
        totalClasses = 20,
        room = "Hall A"
      )
    )

    val logs = listOf(
      AttendanceLog(
        id = 1,
        subjectId = 1,
        dateEpochDay = LocalDate.of(2026, 10, 31).toEpochDay(),
        status = AttendanceStatus.PRESENT,
        note = "Socket Programming"
      )
    )

    val csv = CsvExporter.generateSemesterCsv(subjects, logs)

    assertTrue(csv.contains("=== SUBJECT SUMMARY ==="))
    assertTrue(csv.contains("Computer Networks"))
    assertTrue(csv.contains("CS301"))
    assertTrue(csv.contains("75.0%"))
    assertTrue(csv.contains("=== DAILY ATTENDANCE LEDGER ==="))
    assertTrue(csv.contains("2026-10-31"))
    assertTrue(csv.contains("PRESENT"))
    assertTrue(csv.contains("Socket Programming"))
  }

  @Test
  fun testGenerateSubjectCsv() {
    val subject = Subject(
      id = 2,
      name = "Operating Systems",
      code = "CS204",
      minThreshold = 75.0f,
      attendedClasses = 12,
      totalClasses = 20
    )

    val logs = listOf(
      AttendanceLog(
        id = 1,
        subjectId = 2,
        dateEpochDay = LocalDate.of(2026, 10, 30).toEpochDay(),
        status = AttendanceStatus.ABSENT,
        note = "Sick leave"
      )
    )

    val csv = CsvExporter.generateSubjectCsv(subject, logs)

    assertTrue(csv.contains("Operating Systems (CS204)"))
    assertTrue(csv.contains("60.0%"))
    assertTrue(csv.contains("2026-10-30"))
    assertTrue(csv.contains("ABSENT"))
    assertTrue(csv.contains("Sick leave"))
  }
}
