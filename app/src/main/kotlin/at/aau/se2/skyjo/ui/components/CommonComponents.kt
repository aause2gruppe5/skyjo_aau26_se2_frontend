package at.aau.se2.skyjo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.ui.theme.GoldSurface
import at.aau.se2.skyjo.ui.theme.GoldYellow
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.OnlineGreen
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme

// ── Buttons ────────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

// ── Cards ──────────────────────────────────────────────────────────────────

@Composable
fun SkyjoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

// Legacy alias so existing screens don't break
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = SkyjoCard(modifier = modifier, content = content)

// ── Labels ─────────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(
    eyebrow: String? = null,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Chips ──────────────────────────────────────────────────────────────────

@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun BadgeChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = GoldSurface,
    contentColor: Color = GoldYellow,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ── Avatar ─────────────────────────────────────────────────────────────────

@Composable
fun AvatarBadge(
    initial: Char,
    size: Int = 40,
    showOnlineIndicator: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            modifier = Modifier.size(size.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial.uppercaseChar().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (showOnlineIndicator) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(OnlineGreen, CircleShape)
                    .align(Alignment.BottomEnd),
            )
        }
    }
}

// ── Player rows ────────────────────────────────────────────────────────────

@Composable
fun PlayerRow(
    name: String,
    status: String,
    isHost: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBadge(initial = name.first(), size = 40)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isHost) "$name  👑" else name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = if (status == "Ready") MintGreen else MaterialTheme.colorScheme.outline,
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = if (status == "Ready") PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// ── Responsive helpers ─────────────────────────────────────────────────────

@Composable
fun screenHorizontalPadding(): Dp =
    if (LocalConfiguration.current.screenWidthDp < 400) 14.dp else 20.dp

// ── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    SkyjoTheme {
        PrimaryButton(text = "START GAME", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayerRowPreview() {
    SkyjoTheme {
        PlayerRow(name = "Alice", status = "Ready", isHost = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun StatChipPreview() {
    SkyjoTheme {
        StatChip(label = "Turn", value = "Alice")
    }
}
