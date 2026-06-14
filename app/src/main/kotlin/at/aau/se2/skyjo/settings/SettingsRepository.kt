package at.aau.se2.skyjo.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's audio and haptic preferences in the shared [PREFS_NAME] store and
 * exposes them as observable [StateFlow]s.
 *
 * Defaults: music off, sound effects on, haptic on.
 *
 * A single process-wide instance is shared via [getInstance] so the settings screen, the
 * audio controller and the haptic controller all read from the same source of truth.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _musicEnabled = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, DEFAULT_MUSIC))
    val musicEnabled: StateFlow<Boolean> = _musicEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, DEFAULT_SOUND))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, DEFAULT_HAPTIC))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    fun setMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC, enabled).apply()
        _musicEnabled.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _hapticEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "skyjo_prefs"
        private const val KEY_MUSIC = "settings_music_enabled"
        private const val KEY_SOUND = "settings_sound_enabled"
        private const val KEY_HAPTIC = "settings_haptic_enabled"

        const val DEFAULT_MUSIC = false
        const val DEFAULT_SOUND = true
        const val DEFAULT_HAPTIC = true

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}
