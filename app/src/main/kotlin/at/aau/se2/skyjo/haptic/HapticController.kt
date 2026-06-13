package at.aau.se2.skyjo.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import at.aau.se2.skyjo.settings.SettingsRepository

/**
 * Fires short device vibrations for UI taps and in-game events.
 *
 * Every call is gated by [SettingsRepository.hapticEnabled]; when haptics are disabled the
 * controller is a no-op. Requires the `VIBRATE` permission (declared in the manifest).
 */
class HapticController(
    context: Context,
    private val settings: SettingsRepository,
) {
    private val vibrator: Vibrator? = resolveVibrator(context.applicationContext)

    /** Light tick for tappable UI elements (buttons, switches, nav tabs). */
    fun tick() = vibrate(TICK_MS)

    /** Stronger pulse for meaningful in-game events (draw, replace, reveal, round/game end). */
    fun event() = vibrate(EVENT_MS)

    private fun vibrate(durationMs: Long) {
        if (!settings.hapticEnabled.value) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private companion object {
        const val TICK_MS = 15L
        const val EVENT_MS = 35L
    }
}

/**
 * Provides the active [HapticController] to the composable tree. Defaults to `null` so previews
 * and tests render without a controller; call sites use `LocalHaptic.current?.tick()`.
 */
val LocalHaptic = staticCompositionLocalOf<HapticController?> { null }
