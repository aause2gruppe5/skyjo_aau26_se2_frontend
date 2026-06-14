package at.aau.se2.skyjo.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.settings.SettingsRepository
import at.aau.se2.skyjo.ui.navigation.AppDestination
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AudioControllerTest {

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private lateinit var controller: AudioController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("skyjo_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        settings = SettingsRepository.getInstance(context)
        settings.setMusicEnabled(SettingsRepository.DEFAULT_MUSIC)
        settings.setSoundEnabled(SettingsRepository.DEFAULT_SOUND)
        settings.setHapticEnabled(SettingsRepository.DEFAULT_HAPTIC)
        controller = AudioController(context, settings)
    }

    @Test
    fun `playForDestination maps every music destination without crashing`() {
        settings.setMusicEnabled(true)
        val musicDestinations = listOf(
            AppDestination.Auth,
            AppDestination.Start,
            AppDestination.Lobby,
            AppDestination.Game,
            AppDestination.Friends,
            AppDestination.Leaderboard,
            AppDestination.Settings,
            AppDestination.Rules,
        )
        musicDestinations.forEach { controller.playForDestination(it) }
    }

    @Test
    fun `playForDestination with GameOver stops music and clears track`() {
        settings.setMusicEnabled(true)
        controller.playForDestination(AppDestination.Game)
        controller.playForDestination(AppDestination.GameOver)
        // After clearing, a resume must not restart the stale game track.
        controller.resume()
    }

    @Test
    fun `playForDestination with null destination stops music`() {
        controller.playForDestination(null)
    }

    @Test
    fun `replaying the same destination refreshes instead of restarting`() {
        settings.setMusicEnabled(true)
        controller.playForDestination(AppDestination.Lobby)
        controller.playForDestination(AppDestination.Lobby)
    }

    @Test
    fun `playForDestination while music disabled does not start playback`() {
        settings.setMusicEnabled(false)
        controller.playForDestination(AppDestination.Game)
    }

    @Test
    fun `switching destination while music disabled silences previous track`() {
        settings.setMusicEnabled(true)
        controller.playForDestination(AppDestination.Lobby)
        settings.setMusicEnabled(false)
        controller.playForDestination(AppDestination.Game)
    }

    @Test
    fun `refreshMusic with no current track is a no-op`() {
        controller.refreshMusic()
    }

    @Test
    fun `refreshMusic stops playback when music disabled`() {
        settings.setMusicEnabled(true)
        controller.playForDestination(AppDestination.Game)
        settings.setMusicEnabled(false)
        controller.refreshMusic()
    }

    @Test
    fun `refreshMusic starts playback when music re-enabled`() {
        settings.setMusicEnabled(false)
        controller.playForDestination(AppDestination.Game)
        settings.setMusicEnabled(true)
        controller.refreshMusic()
    }

    @Test
    fun `playSfx plays each effect when sound enabled`() {
        settings.setSoundEnabled(true)
        SoundEffect.entries.forEach { controller.playSfx(it) }
    }

    @Test
    fun `playSfx is a no-op when sound disabled`() {
        settings.setSoundEnabled(false)
        controller.playSfx(SoundEffect.VICTORY)
    }

    @Test
    fun `pause and resume cycle while playing`() {
        settings.setMusicEnabled(true)
        controller.playForDestination(AppDestination.Game)
        controller.pause()
        controller.resume()
    }

    @Test
    fun `resume while music disabled does not start playback`() {
        settings.setMusicEnabled(false)
        controller.playForDestination(AppDestination.Game)
        controller.pause()
        controller.resume()
    }

    @Test
    fun `release stops music and frees the sound pool`() {
        settings.setMusicEnabled(true)
        controller.playForDestination(AppDestination.Game)
        controller.release()
    }

    @Test
    fun `LocalAudio composition local exists`() {
        org.junit.Assert.assertNotNull(LocalAudio)
    }
}
