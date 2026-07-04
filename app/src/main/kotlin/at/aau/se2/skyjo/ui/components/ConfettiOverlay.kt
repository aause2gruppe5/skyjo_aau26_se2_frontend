package at.aau.se2.skyjo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import at.aau.se2.skyjo.ui.theme.BlueAccent
import at.aau.se2.skyjo.ui.theme.DangerRed
import at.aau.se2.skyjo.ui.theme.GoldYellow
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.PrimaryGreenMid
import at.aau.se2.skyjo.ui.theme.rememberReducedMotion
import kotlin.random.Random

private data class ConfettiPiece(
    val xFraction: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val drift: Float,
    val delay: Float,
)

/**
 * A one-shot, purely decorative in-screen confetti burst that rains down over the
 * area it fills. Draw it on top of the content you want to celebrate. No-ops when
 * [show] is false or the OS has animations disabled.
 */
@Composable
fun ConfettiOverlay(
    show: Boolean,
    modifier: Modifier = Modifier,
    pieceCount: Int = 90,
) {
    if (!show || rememberReducedMotion()) return

    val colors = listOf(PrimaryGreenMid, MintGreen, BlueAccent, GoldYellow, DangerRed)
    val pieces = remember(show) {
        List(pieceCount) {
            ConfettiPiece(
                xFraction = Random.nextFloat(),
                color = colors[Random.nextInt(colors.size)],
                size = 8f + Random.nextFloat() * 10f,
                rotation = Random.nextFloat() * 360f,
                drift = (Random.nextFloat() - 0.5f) * 0.3f,
                delay = Random.nextFloat() * 0.25f,
            )
        }
    }
    val progress = remember(show) { Animatable(0f) }
    LaunchedEffect(show) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1800))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        pieces.forEach { p ->
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            val x = (p.xFraction + p.drift * local) * size.width
            val y = (-0.1f + local * 1.2f) * size.height
            val alpha = (1f - local).coerceIn(0f, 1f)
            rotate(degrees = p.rotation + local * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                    size = Size(p.size, p.size * 0.6f),
                )
            }
        }
    }
}
