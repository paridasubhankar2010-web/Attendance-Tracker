package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "",
    val institution: String = "",
    val preparingForCompetitive: Boolean = false,
    val targetExam: String = "",
    val isLoggedIn: Boolean = false
)

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val institution = prefs.getString(KEY_INSTITUTION, "") ?: ""
        val preparing = prefs.getBoolean(KEY_PREPARING, false)
        val exam = prefs.getString(KEY_TARGET_EXAM, "") ?: ""
        return UserProfile(
            name = name,
            institution = institution,
            preparingForCompetitive = preparing,
            targetExam = exam,
            isLoggedIn = isLoggedIn
        )
    }

    fun saveProfile(
        name: String,
        institution: String,
        preparingForCompetitive: Boolean,
        targetExam: String = ""
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_NAME, name.trim())
            .putString(KEY_INSTITUTION, institution.trim())
            .putBoolean(KEY_PREPARING, preparingForCompetitive)
            .putString(KEY_TARGET_EXAM, targetExam.trim())
            .apply()

        _userProfile.value = UserProfile(
            name = name.trim(),
            institution = institution.trim(),
            preparingForCompetitive = preparingForCompetitive,
            targetExam = targetExam.trim(),
            isLoggedIn = true
        )
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
        _userProfile.value = _userProfile.value.copy(isLoggedIn = false)
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_NAME = "user_name"
        private const val KEY_INSTITUTION = "user_institution"
        private const val KEY_PREPARING = "user_preparing_competitive"
        private const val KEY_TARGET_EXAM = "user_target_exam"
    }
}
