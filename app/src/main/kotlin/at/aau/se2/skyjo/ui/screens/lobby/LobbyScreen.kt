package at.aau.se2.skyjo.ui.screens.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.ui.components.AvatarBadge
import at.aau.se2.skyjo.ui.components.PrimaryButton
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.theme.BackgroundGray
import at.aau.se2.skyjo.ui.theme.BorderColor
import at.aau.se2.skyjo.ui.theme.GreenSurface
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.ui.theme.SurfaceWhite

@Composable
fun LobbyScreen(
    players: List<LobbyPlayer> = emptyList(),
    maxPlayers: Int = 6,
    isHost: Boolean = false,
    joinCode: String? = null,
    errorMessage: String? = null,
    onStartGame: (maxRounds: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedRounds by remember { mutableIntStateOf(3) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray),
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────
        Surface(
            color = SurfaceWhite,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryGreen,
                    )
                }
                Text(
                    text = "SKYJO ACTION",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────
            Column {
                Text(
                    text = "CURRENT LOBBY",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (players.isEmpty()) "Waiting for\nPlayers" else "Players Ready",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                joinCode?.takeIf { it.isNotBlank() }?.let { code ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Join Code: $code",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
                errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ── Player count bar ─────────────────────────────────────────
            SkyjoCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Players",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${players.size} / $maxPlayers",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(GreenSurface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = if (maxPlayers > 0) players.size.toFloat() / maxPlayers else 0f)
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(PrimaryGreen),
                        )
                    }
                }
            }

            // ── Player Slots ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                players.forEach { player ->
                    FilledPlayerSlot(player = player)
                }
                // Empty slots up to maxPlayers (show at most 2 empty slots)
                val emptySlots = (maxPlayers - players.size).coerceIn(0, 2)
                repeat(emptySlots) {
                    EmptyPlayerSlot()
                }
            }

            // ── Match Rules ──────────────────────────────────────────────
            SkyjoCard {
                Column {
                    Text(
                        text = "Match Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Number of Rounds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { rounds ->
                            ToggleChip(
                                text = rounds.toString(),
                                selected = selectedRounds == rounds,
                                onClick = { if (isHost) selectedRounds = rounds },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (!isHost) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Only the host can change rules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Bottom Start Button ──────────────────────────────────────────
        Surface(
            color = SurfaceWhite,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = "START GAME",
                    onClick = { onStartGame(selectedRounds) },
                    enabled = isHost && players.size >= 2,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isHost) "Need at least 2 players to start" else "Waiting for host to start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FilledPlayerSlot(player: LobbyPlayer) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBadge(
                initial = player.nickname.firstOrNull() ?: '?',
                size = 44,
                showOnlineIndicator = true,
                backgroundColor = MintGreen,
                textColor = PrimaryGreen,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (player.isHost) "Host" else "Player",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MintGreen,
            ) {
                Text(
                    text = "Ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyPlayerSlot() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(
                width = 1.5.dp,
                color = BorderColor,
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Empty slot",
                tint = MutedText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Waiting for player...",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
            )
        }
    }
}

@Composable
private fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) PrimaryGreen else MaterialTheme.colorScheme.surface,
        border = if (!selected)
            androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        else null,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) SurfaceWhite else MutedText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LobbyScreenPreview() {
    SkyjoTheme {
        LobbyScreen(
            players = listOf(
                LobbyPlayer("Alice", isHost = true),
                LobbyPlayer("Bob", isHost = false),
            ),
            maxPlayers = 6,
            isHost = true,
            onStartGame = {},
            onBack = {},
        )
    }
}
