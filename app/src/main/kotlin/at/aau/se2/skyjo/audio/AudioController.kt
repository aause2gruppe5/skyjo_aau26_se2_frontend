package at.aau.se2.skyjo.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import at.aau.se2.skyjo.R
import at.aau.se2.skyjo.settings.SettingsRepository
import at.aau.se2.skyjo.ui.navigation.AppDestination

/** One-shot sound effects, gated by [SettingsRepository.soundEnabled]. */
enum class SoundEffect(@RawRes val resId: Int) {
    VICTORY(R.raw.sfx_victory),
    DEFEAT(R.raw.sfx_defeat),
    ROUND_COMPLETE(R.raw.sfx_round_complete),
}

/**
 * Plays per-screen looping background music and one-shot sound effects.
 *
 * Background music is gated by [SettingsRepository.musicEnabled] and sound effects by
 * [SettingsRepository.soundEnabled]; the controller reads the flow values at play time so a
 * toggle change takes effect immediately (see [refreshMusic]).
 *
 * Activity-scoped: call [pause]/[resume] from the host lifecycle and [release] on destroy.
 */
class AudioController(
    context: Context,
    private val settings: SettingsRepository,
) {
    private val appContext = context.applicationContext

    private var bgmPlayer: MediaPlayer? = null
    private var currentTrack: Int? = null
    private var paused = false

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loadedSfx: Map<SoundEffect, Int> =
        SoundEffect.entries.associateWith { soundPool.load(appContext, it.resId, 1) }

    /** Switches background music to the track for [destination], or stops it when none maps. */
    fun playForDestination(destination: AppDestination?) {
        val track = destination?.let(::trackFor)
        if (track == null) {
            stopBgm()
            currentTrack = null
            return
        }
        if (track == currentTrack && bgmPlayer != null) {
            refreshMusic()
            return
        }
        currentTrack = track
        startBgm(track)
    }

    /** Re-evaluates the music toggle for the current track (start if enabled, stop if not). */
    fun refreshMusic() {
        val track = currentTrack ?: return
        if (settings.musicEnabled.value && !paused) {
            if (bgmPlayer == null) startBgm(track)
        } else {
            stopBgm()
        }
    }

    fun playSfx(effect: SoundEffect) {
        if (!settings.soundEnabled.value) return
        val soundId = loadedSfx[effect] ?: return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun pause() {
        paused = true
        bgmPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun resume() {
        paused = false
        if (settings.musicEnabled.value) {
            currentTrack?.let { track ->
                if (bgmPlayer == null) startBgm(track) else bgmPlayer?.start()
            }
        }
    }

    fun release() {
        stopBgm()
        soundPool.release()
    }

    private fun startBgm(@RawRes track: Int) {
        stopBgm()
        if (!settings.musicEnabled.value || paused) return
        bgmPlayer = MediaPlayer.create(appContext, track)?.apply {
            isLooping = true
            start()
        }
    }

    private fun stopBgm() {
        bgmPlayer?.release()
        bgmPlayer = null
    }

    private fun trackFor(destination: AppDestination): Int? = when (destination) {
        AppDestination.Auth -> R.raw.bgm_auth
        AppDestination.Start -> R.raw.bgm_home
        AppDestination.Lobby -> R.raw.bgm_lobby
        AppDestination.Game -> R.raw.bgm_game
        AppDestination.Friends -> R.raw.bgm_friends
        AppDestination.Leaderboard -> R.raw.bgm_leaderboard
        AppDestination.Settings -> R.raw.bgm_settings
        AppDestination.Rules -> R.raw.bgm_settings
        AppDestination.GameOver -> null
    }
}

/**
 * Provides the active [AudioController] to the composable tree. Defaults to `null` so previews
 * and tests render without a controller; call sites use `LocalAudio.current?.playSfx(...)`.
 */
val LocalAudio = androidx.compose.runtime.staticCompositionLocalOf<AudioController?> { null }
