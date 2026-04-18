package at.aau.se2.skyjo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = GreenSurface,
    onPrimaryContainer = PrimaryGreen,
    secondary = BlueAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = BlueSurface,
    onSecondaryContainer = DeepBlue,
    tertiary = GoldYellow,
    onTertiary = SurfaceWhite,
    tertiaryContainer = GoldSurface,
    onTertiaryContainer = GoldDark,
    background = BackgroundGray,
    onBackground = DarkText,
    surface = SurfaceWhite,
    onSurface = DarkText,
    surfaceVariant = GreenSurface,
    onSurfaceVariant = PrimaryGreen,
    error = DangerRed,
    onError = SurfaceWhite,
    errorContainer = DangerSurface,
    onErrorContainer = DangerDark,
    outline = BorderColor,
    outlineVariant = BorderColor,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(50.dp),
)

@Composable
fun SkyjoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
