package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Subject
import com.example.data.UserSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Attendance Tracker", appName)
  }

  @Test
  fun `verify attendance calculations and thresholds`() {
    // Subject with 19 attended out of 20 total = 95%, threshold 75%
    val safeSubject = Subject(
      name = "Discrete Math",
      attendedClasses = 19,
      totalClasses = 20,
      minThreshold = 75.0f
    )
    assertEquals(95.0, safeSubject.percentage, 0.01)
    assertEquals(false, safeSubject.isBelowThreshold)
    // How many can miss: (19) / (20 + x) >= 0.75 -> 20 + x <= 25.33 -> x = 5
    assertEquals(5, safeSubject.safeToMissCount)

    // Subject below threshold: 12 attended out of 20 = 60%, threshold 75%
    val criticalSubject = Subject(
      name = "Operating Systems",
      attendedClasses = 12,
      totalClasses = 20,
      minThreshold = 75.0f
    )
    assertEquals(60.0, criticalSubject.percentage, 0.01)
    assertEquals(true, criticalSubject.isBelowThreshold)
    assertEquals(12, criticalSubject.classesNeededCount)
  }

  @Test
  fun `verify user login profile saving and logout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = UserSessionManager(context)

    // Initially logged out
    sessionManager.logout()
    assertFalse(sessionManager.userProfile.value.isLoggedIn)

    // Save profile
    sessionManager.saveProfile(
      name = "Jane Doe",
      institution = "MIT",
      preparingForCompetitive = true,
      targetExam = "GATE"
    )

    val profile = sessionManager.userProfile.value
    assertTrue(profile.isLoggedIn)
    assertEquals("Jane Doe", profile.name)
    assertEquals("MIT", profile.institution)
    assertTrue(profile.preparingForCompetitive)
    assertEquals("GATE", profile.targetExam)

    // Logout
    sessionManager.logout()
    assertFalse(sessionManager.userProfile.value.isLoggedIn)
  }
}
