package at.aau.se2.skyjo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import at.aau.se2.skyjo.ui.theme.Motion
import at.aau.se2.skyjo.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay

/**
 * Springy scale-down while pressed. Pass the same [interactionSource] the clickable
 * uses. Purely a visual transform, so it never changes the composable tree.
 */
@Composable
fun Modifier.bouncyPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) pressedScale else 1f,
        animationSpec = Motion.bouncyFloat,
        label = "bouncyPress",
    )
    return this.scale(scale)
}

/**
 * Fade + rise entrance on first composition, with an optional stagger [delayMillis].
 * Ends fully visible (alpha 1, no offset), and no-ops under reduced motion, so it is
 * safe for content that tests assert on.
 */
@Composable
fun Modifier.entrance(delayMillis: Int = 0): Modifier {
    if (rememberReducedMotion()) return this
    val progress = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(1f, animationSpec = tween(Motion.Normal))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 36f
    }
}
