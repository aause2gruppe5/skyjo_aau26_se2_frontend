package at.aau.se2.skyjo.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

/**
 * Shared motion tokens so animations feel consistent and intentional across every
 * screen. Durations are in milliseconds. The spring specs give the app its playful,
 * slightly bouncy character without being distracting.
 */
object Motion {
    const val Fast = 140
    const val Normal = 280
    const val Slow = 520

    /** Playful overshoot — for press feedback, pops and reveals. */
    val bouncyFloat: AnimationSpec<Float>
        get() = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Smooth, no overshoot — for entrances and value changes. */
    val gentleFloat: AnimationSpec<Float>
        get() = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)

    val bouncyDp: AnimationSpec<Dp>
        get() = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
}

/**
 * True when the OS has animations turned off (accessibility or battery saver).
 * Decorative effects should collapse to their final state instead of animating.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f) == 0f
    }
}
