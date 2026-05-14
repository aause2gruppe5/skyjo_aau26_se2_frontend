package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.ui.components.PrimaryButton
import at.aau.se2.skyjo.ui.components.SecondaryButton
import at.aau.se2.skyjo.ui.components.StatChip
import at.aau.se2.skyjo.ui.theme.BackgroundGray
import at.aau.se2.skyjo.ui.theme.BlueSurface
import at.aau.se2.skyjo.ui.theme.BorderColor
import at.aau.se2.skyjo.ui.theme.CardHiddenBg
import at.aau.se2.skyjo.ui.theme.CardHiddenText
import at.aau.se2.skyjo.ui.theme.CardNegativeBg
import at.aau.se2.skyjo.ui.theme.CardPositiveBg
import at.aau.se2.skyjo.ui.theme.GreenDark
import at.aau.se2.skyjo.ui.theme.GreenSurface
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.ui.theme.SurfaceWhite

// ── Dummy data ──────────────────────────────────────────────────────────────

private data class GamePlayer(val name: String, val score: Int, val isActive: Boolean = false)

private data class ActionCardUiModel(val label: String, val isDefense: Boolean = false)

private val dummyPlayers = listOf(
    GamePlayer("Alice", 12, isActive = true),
    GamePlayer("Bob", 8),
    GamePlayer("Charlie", 15),
)

// Grid: null = face-down, number string = revealed
private val myGrid = listOf(
    listOf(null, "3", null, "-1"),
    listOf("7", null, "11", null),
    listOf(null, "5", null, "2"),
)

private val actionCards = listOf(
    ActionCardUiModel("👁 Peek"),
    ActionCardUiModel("🔄 Trade"),
    ActionCardUiModel("🛡 Defense + Turn", isDefense = true),
    ActionCardUiModel("🃏 Draw"),
)

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun GameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayDefense: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray),
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Surface(
            color = SurfaceWhite,
            shadowElevation = 2.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(52.dp),
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
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Round 1",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedText,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }

                // Player pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dummyPlayers.forEach { player ->
                        PlayerPill(player = player, modifier = Modifier.weight(1f))
                    }
                }

                // Status chips
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatChip(label = "Turn", value = "Alice")
                    StatChip(label = "Defense", value = "Protected")
                    StatChip(label = "Target", value = "≤ 100")
                    StatChip(label = "Cards", value = "24 left")
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Action Market ─────────────────────────────────────────────
            ActionMarketSection()

            // ── Deck + Discard ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeckCard(label = "Draw Deck", modifier = Modifier.weight(1f))
                DiscardCard(value = "4", label = "Discard Pile", modifier = Modifier.weight(1f))
            }

            // ── Player Grid ───────────────────────────────────────────────
            SectionCard(title = "Your Grid", badge = "Score: 12") {
                CardGrid(cards = myGrid)
            }

            // ── Hand Action Cards ─────────────────────────────────────────
            HandActionCardsSection(cards = actionCards, onPlayDefense = onPlayDefense)

            // Bottom spacing
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Action Buttons ────────────────────────────────────────────────
        Surface(
            color = SurfaceWhite,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryButton(text = "REVEAL CARD", onClick = {})
                SecondaryButton(text = "Swap with Discard", onClick = {})
                // Timer chip
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = BlueSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "⏱  24s remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun PlayerPill(player: GamePlayer, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (player.isActive) PrimaryGreen else MaterialTheme.colorScheme.surface,
        border = if (!player.isActive)
            androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        else null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (player.isActive) SurfaceWhite else MutedText,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${player.score} pts",
                style = MaterialTheme.typography.labelMedium,
                color = if (player.isActive) MintGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActionMarketSection() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ACTION MARKET",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Central action deck
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimaryGreen, GreenDark),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = 2.dp,
                            color = MintGreen.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Action Draw Deck",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action card chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actionCards.forEach { action ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = GreenSurface,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCard(label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryGreen, GreenDark),
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "?", style = MaterialTheme.typography.headlineLarge, color = MintGreen)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DiscardCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(
                        color = CardPositiveBg,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .border(
                        width = 1.dp,
                        color = BorderColor,
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HandActionCardsSection(cards: List<ActionCardUiModel>, onPlayDefense: () -> Unit) {
    SectionCard(title = "Your Action Cards", badge = "${cards.size} cards") {
        if (cards.isEmpty()) {
            Text(
                text = "No action cards in hand",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cards.forEach { card ->
                    val cardModifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.65f)
                        .border(
                            width = 1.dp,
                            color = MintGreen.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .then(
                            if (card.isDefense) {
                                Modifier
                                    .testTag("play_defense_action_card")
                                    .clickable(onClick = onPlayDefense)
                            } else {
                                Modifier
                            },
                        )
                        .semantics {
                            contentDescription = if (card.isDefense) {
                                "Play Defense action card"
                            } else {
                                card.label
                            }
                        }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GreenSurface,
                        modifier = cardModifier,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = card.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    badge: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (badge != null) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = GreenSurface,
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CardGrid(cards: List<List<String?>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { value ->
                    GameCardTile(value = value, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GameCardTile(value: String?, modifier: Modifier = Modifier) {
    val isHidden = value == null
    val numValue = value?.toIntOrNull()
    val bgColor = when {
        isHidden -> CardHiddenBg
        numValue != null && numValue <= 0 -> CardNegativeBg
        else -> CardPositiveBg
    }
    val textColor = when {
        isHidden -> CardHiddenText
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .aspectRatio(0.65f)
            .background(color = bgColor, shape = RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (isHidden) MintGreen.copy(alpha = 0.4f) else BorderColor,
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GameScreenPreview() {
    SkyjoTheme {
        GameScreen(onBack = {})
    }
}
