package at.aau.se2.skyjo.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import at.aau.se2.skyjo.ui.theme.Motion
import at.aau.se2.skyjo.ui.theme.rememberReducedMotion

/**
 * A number that rolls up/down to [value] instead of snapping — used for score
 * reveals. Settles on the exact [value] (and shows it immediately under reduced
 * motion), so assertions on the final number stay valid.
 */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    suffix: String = "",
) {
    val reduced = rememberReducedMotion()
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = Motion.Slow),
        label = "animatedCounter",
    )
    Text(
        text = "${if (reduced) value else animated}$suffix",
        modifier = modifier,
        style = style,
        color = color,
    )
}
