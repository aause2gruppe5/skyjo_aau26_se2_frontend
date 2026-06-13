package at.aau.se2.skyjo.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("skyjo_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        repository = SettingsRepository.getInstance(context)
        // Reset to defaults so a shared singleton from another test doesn't leak state.
        repository.setMusicEnabled(SettingsRepository.DEFAULT_MUSIC)
        repository.setSoundEnabled(SettingsRepository.DEFAULT_SOUND)
        repository.setHapticEnabled(SettingsRepository.DEFAULT_HAPTIC)
    }

    @Test
    fun `defaults are music off sound on haptic on`() {
        assertFalse(repository.musicEnabled.value)
        assertTrue(repository.soundEnabled.value)
        assertTrue(repository.hapticEnabled.value)
    }

    @Test
    fun `setMusicEnabled updates the flow`() {
        repository.setMusicEnabled(true)
        assertTrue(repository.musicEnabled.value)
    }

    @Test
    fun `setSoundEnabled updates the flow`() {
        repository.setSoundEnabled(false)
        assertFalse(repository.soundEnabled.value)
    }

    @Test
    fun `setHapticEnabled updates the flow`() {
        repository.setHapticEnabled(false)
        assertFalse(repository.hapticEnabled.value)
    }

    @Test
    fun `toggling a setting back and forth ends on the last value`() {
        repository.setMusicEnabled(true)
        repository.setMusicEnabled(false)
        repository.setMusicEnabled(true)
        assertTrue(repository.musicEnabled.value)
    }
}
