package at.aau.se2.skyjo.ui.screens.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.se2.skyjo.ui.components.AvatarBadge
import at.aau.se2.skyjo.ui.components.BadgeChip
import at.aau.se2.skyjo.ui.components.PrimaryButton
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.components.SkyjoDrawerScaffold
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.GoldSurface
import at.aau.se2.skyjo.ui.theme.GoldYellow
import at.aau.se2.skyjo.ui.theme.GreenDark
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.OnlineGreen
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.ui.theme.SurfaceWhite

private val onlinePlayers = listOf('K', 'M', 'R', 'T')

@Composable
fun StartScreen(
    onPlayClicked: (playerName: String) -> Unit,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var playerName by remember { mutableStateOf("") }

    SkyjoDrawerScaffold(
        currentDestination = AppDestination.Start,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Hero Card ────────────────────────────────────────────────
            SkyjoCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Gradient header area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(PrimaryGreen, GreenDark),
                                ),
                                shape = MaterialTheme.shapes.medium,
                            )
                            .padding(20.dp),
                    ) {
                        Column {
                            // Eyebrow badge
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MintGreen.copy(alpha = 0.25f),
                            ) {
                                Text(
                                    text = "ACTION MODE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MintGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PLAY",
                                style = MaterialTheme.typography.displayLarge,
                                color = SurfaceWhite,
                                lineHeight = 52.sp,
                            )
                            Text(
                                text = "Challenge your friends\nin the ultimate card game",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SurfaceWhite.copy(alpha = 0.75f),
                            )
                        }
                        // Online players row – bottom right
                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            onlinePlayers.take(3).forEachIndexed { index, initial ->
                                AvatarBadge(
                                    initial = initial,
                                    size = 28,
                                    backgroundColor = MintGreen,
                                    textColor = PrimaryGreen,
                                    modifier = if (index > 0) Modifier.offset(x = (-6).dp) else Modifier,
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1,284 online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintGreen,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player name input
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // CTA button
                    PrimaryButton(
                        text = "START NEW SESSION",
                        onClick = { if (playerName.isNotBlank()) onPlayClicked(playerName.trim()) },
                        enabled = playerName.isNotBlank(),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            // ── Feature Cards Row ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FeatureCard(
                    emoji = "👥",
                    title = "Friends",
                    subtitle = "3 online",
                    onClick = { onNavigate(AppDestination.Friends) },
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
            }

            // ── Profile / Stats Card ─────────────────────────────────────
            SkyjoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarBadge(
                        initial = if (playerName.isNotBlank()) playerName.first() else 'A',
                        size = 52,
                        showOnlineIndicator = true,
                        backgroundColor = MintGreen,
                        textColor = PrimaryGreen,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (playerName.isNotBlank()) playerName else "AcePlayer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BadgeChip(text = "Pro Tier")
                        }
                        Text(
                            text = "Level 42",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    StatItem(value = "127", label = "Total Wins")
                    VerticalDivider()
                    StatItem(value = "18", label = "Avg Score")
                    VerticalDivider()
                    StatItem(value = "57%", label = "Win Rate")
                }
            }

            // Bottom spacing for floating nav
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryGreen,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun StartScreenPreview() {
    SkyjoTheme {
        StartScreen(onPlayClicked = {}, onNavigate = {})
    }
}
