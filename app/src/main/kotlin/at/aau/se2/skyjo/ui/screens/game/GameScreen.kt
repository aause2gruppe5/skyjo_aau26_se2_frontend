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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.game.model.BoardLayout
import at.aau.se2.skyjo.game.model.BoardPosition
import at.aau.se2.skyjo.game.model.BoardSlot
import at.aau.se2.skyjo.game.model.GamePhase
import at.aau.se2.skyjo.game.model.GameState
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

private enum class PendingAction { PLACE_CARD, DISCARD_AND_REVEAL }

@Composable
fun GameScreen(
    gameState: GameState?,
    onDrawFromDeck: () -> Unit = {},
    onTakeDiscardCard: () -> Unit = {},
    onReplaceDrawnCard: (BoardPosition) -> Unit = {},
    onDiscardAndReveal: (BoardPosition) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingAction by remember(gameState?.phase) { mutableStateOf<PendingAction?>(null) }
    var showResults by remember(gameState?.phase) { mutableStateOf(gameState?.phase == GamePhase.ROUND_FINISHED) }

    val currentPlayer = gameState?.players?.getOrNull(gameState.currentPlayerIndex)
    val currentPlayerName = currentPlayer?.id ?: "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray),
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Surface(color = SurfaceWhite, shadowElevation = 2.dp) {
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
                    if (gameState?.phase == GamePhase.FINAL_TURNS) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = BlueSurface,
                        ) {
                            Text(
                                text = "Final Turns: ${gameState.finalTurnsRemaining}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Player pills
                if (gameState != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gameState.players.forEachIndexed { index, player ->
                            PlayerPill(
                                name = player.id,
                                score = player.board.rawScore(),
                                isActive = index == gameState.currentPlayerIndex,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Status chips
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatChip(label = "Turn", value = currentPlayerName)
                    StatChip(label = "Deck", value = "${gameState?.drawPile?.size ?: "—"}")
                    if (pendingAction != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = PrimaryGreen,
                        ) {
                            Text(
                                text = when (pendingAction) {
                                    PendingAction.PLACE_CARD -> "Tap a card to place"
                                    PendingAction.DISCARD_AND_REVEAL -> "Tap a face-down card"
                                    null -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = SurfaceWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
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
            // ── Deck + Discard ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeckCard(
                    size = gameState?.drawPile?.size,
                    label = "Draw Deck",
                    modifier = Modifier.weight(1f),
                )
                DiscardCard(
                    value = gameState?.discardPile?.cards?.lastOrNull()?.value?.toString() ?: "—",
                    label = "Discard Pile",
                    modifier = Modifier.weight(1f),
                )
                if (gameState?.drawnCard != null) {
                    DrawnCard(
                        value = gameState.drawnCard.value.toString(),
                        label = "Drawn Card",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Player Grids ──────────────────────────────────────────────
            if (gameState != null) {
                gameState.players.forEachIndexed { index, player ->
                    val isCurrentPlayer = index == gameState.currentPlayerIndex
                    val board = player.board
                    val grid = (0 until BoardLayout.ROWS).map { row ->
                        (0 until BoardLayout.COLUMNS).map { col ->
                            board.slotAt(BoardPosition(row, col))
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = SurfaceWhite,
                        shadowElevation = if (isCurrentPlayer) 4.dp else 1.dp,
                        border = if (isCurrentPlayer)
                            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
                        else null,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (isCurrentPlayer) "${player.id}'s Grid (Your Turn)" else "${player.id}'s Grid",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentPlayer) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = GreenSurface,
                                ) {
                                    Text(
                                        text = "Score: ${board.rawScore()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            CardGrid(
                                grid = grid,
                                onCardTap = if (isCurrentPlayer && pendingAction != null) { row, col ->
                                    val pos = BoardPosition(row, col)
                                    val slot = board.slotAt(pos)
                                    when (pendingAction) {
                                        PendingAction.PLACE_CARD -> {
                                            onReplaceDrawnCard(pos)
                                            pendingAction = null
                                        }
                                        PendingAction.DISCARD_AND_REVEAL -> {
                                            if (slot is BoardSlot.Occupied && !slot.faceUp) {
                                                onDiscardAndReveal(pos)
                                                pendingAction = null
                                            }
                                        }
                                        null -> {}
                                    }
                                } else null,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Action Buttons ────────────────────────────────────────────────
        Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (gameState?.phase) {
                    GamePhase.AWAITING_DRAW, GamePhase.FINAL_TURNS -> {
                        PrimaryButton(text = "DRAW FROM DECK", onClick = onDrawFromDeck)
                        SecondaryButton(text = "Take from Discard", onClick = onTakeDiscardCard)
                    }
                    GamePhase.AWAITING_REPLACEMENT -> {
                        PrimaryButton(
                            text = if (pendingAction == PendingAction.PLACE_CARD) "← Tap a card on the grid" else "PLACE DRAWN CARD",
                            onClick = { pendingAction = PendingAction.PLACE_CARD },
                        )
                        SecondaryButton(
                            text = if (pendingAction == PendingAction.DISCARD_AND_REVEAL) "← Tap a face-down card" else "Discard & Reveal",
                            onClick = { pendingAction = PendingAction.DISCARD_AND_REVEAL },
                        )
                    }
                    GamePhase.ROUND_FINISHED -> {
                        PrimaryButton(text = "VIEW RESULTS", onClick = { showResults = true })
                    }
                    else -> {
                        // NOT_STARTED or null: show disabled placeholder
                        PrimaryButton(text = "DRAW FROM DECK", onClick = {}, enabled = false)
                    }
                }
            }
        }
    }

    // ── Round Results Dialog ──────────────────────────────────────────────
    if (showResults && gameState?.roundResult != null) {
        AlertDialog(
            onDismissRequest = { showResults = false },
            title = { Text("Round Results", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gameState.roundResult.scores
                        .sortedBy { it.finalScore }
                        .forEach { score ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val suffix = when {
                                    score.playerId == gameState.roundResult.finisherPlayerId -> " 🏁"
                                    score.finalScore != score.rawScore -> " (×2)"
                                    else -> ""
                                }
                                Text(text = "${score.playerId}$suffix", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${score.finalScore} pts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                )
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResults = false; onBack() }) {
                    Text("Back to Lobby")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResults = false }) {
                    Text("Close")
                }
            },
        )
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun PlayerPill(
    name: String,
    score: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isActive) PrimaryGreen else MaterialTheme.colorScheme.surface,
        border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, BorderColor) else null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) SurfaceWhite else MutedText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = "$score pts",
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) MintGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DeckCard(size: Int?, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(
                        brush = Brush.verticalGradient(listOf(PrimaryGreen, GreenDark)),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "?", style = MaterialTheme.typography.headlineLarge, color = MintGreen)
                    if (size != null) {
                        Text(
                            text = "$size left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MintGreen.copy(alpha = 0.8f),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedText, textAlign = TextAlign.Center)
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val numValue = value.toIntOrNull()
            val bgColor = when {
                numValue == null -> CardHiddenBg
                numValue <= 0 -> CardNegativeBg
                else -> CardPositiveBg
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(color = bgColor, shape = MaterialTheme.shapes.medium)
                    .border(width = 1.dp, color = BorderColor, shape = MaterialTheme.shapes.medium),
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
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DrawnCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val numValue = value.toIntOrNull()
            val bgColor = when {
                numValue == null -> CardHiddenBg
                numValue <= 0 -> CardNegativeBg
                else -> CardPositiveBg
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(color = bgColor, shape = MaterialTheme.shapes.medium),
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
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CardGrid(
    grid: List<List<BoardSlot>>,
    onCardTap: ((row: Int, col: Int) -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grid.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { colIndex, slot ->
                    GameCardTile(
                        slot = slot,
                        onClick = onCardTap?.let { { it(rowIndex, colIndex) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCardTile(
    slot: BoardSlot,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor, label) = when (slot) {
        is BoardSlot.Cleared -> Triple(GreenSurface, MutedText, "✓")
        is BoardSlot.Occupied -> if (!slot.faceUp) {
            Triple(CardHiddenBg, CardHiddenText, "?")
        } else {
            val numVal = slot.card.value
            val bg = if (numVal <= 0) CardNegativeBg else CardPositiveBg
            Triple(bg, MaterialTheme.colorScheme.onBackground, numVal.toString())
        }
    }

    val tappable = onClick != null && slot is BoardSlot.Occupied && slot != BoardSlot.Cleared

    Box(
        modifier = modifier
            .aspectRatio(0.65f)
            .clip(RoundedCornerShape(10.dp))
            .background(color = bgColor, shape = RoundedCornerShape(10.dp))
            .border(
                width = if (tappable) 2.dp else 1.dp,
                color = if (tappable) PrimaryGreen else if (slot is BoardSlot.Occupied && !slot.faceUp) MintGreen.copy(alpha = 0.4f) else BorderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .then(if (onClick != null && slot !is BoardSlot.Cleared) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
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
        GameScreen(gameState = null, onBack = {})
    }
}
