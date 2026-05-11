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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.TotalScore
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
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.ui.theme.SurfaceWhite

private val actionCards = listOf("👁 Peek", "🔄 Trade", "⚡ Double", "🃏 Draw")

private const val PHASE_AWAITING_DRAW = "AWAITING_DRAW"
private const val PHASE_AWAITING_REPLACEMENT = "AWAITING_REPLACEMENT"
private const val PHASE_ROUND_FINISHED = "ROUND_FINISHED"
private const val PHASE_FINAL_TURNS = "FINAL_TURNS"

@Composable
fun GameScreen(
    gameState: GameUpdateMessage? = null,
    myPlayerId: String? = null,
    isMyTurn: Boolean = false,
    onDrawFromDeck: () -> Unit = {},
    onDrawFromDiscard: () -> Unit = {},
    onReplaceCard: (row: Int, col: Int) -> Unit = { _, _ -> },
    onDiscardAndReveal: (row: Int, col: Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tracks which action mode is selected in AWAITING_REPLACEMENT
    var pendingAction by remember { mutableStateOf<String?>(null) }

    // Reset pending action when phase changes
    val currentPhase = gameState?.phase
    val myBoard = gameState?.players?.find { it.playerId == myPlayerId }?.board
    val myScore = gameState?.totalScores?.find { it.playerId == myPlayerId }?.totalScore ?: 0
    val currentPlayerNickname = gameState?.players
        ?.find { it.playerId == gameState.currentPlayerId }?.nickname ?: ""
    val discardCard = gameState?.discardTopCard
    val drawnCard = gameState?.drawnCard

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
                        text = if (gameState != null) "Round ${gameState.roundNumber}" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedText,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }

                // Player score pills
                if (gameState != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        gameState.totalScores.forEach { score ->
                            PlayerPill(
                                score = score,
                                isActive = score.playerId == gameState.currentPlayerId,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Status chips
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatChip(
                            label = "Turn",
                            value = if (isMyTurn) "You" else currentPlayerNickname,
                        )
                        StatChip(label = "Phase", value = currentPhase ?: "")
                        if (gameState.gameOver) {
                            StatChip(label = "Status", value = "Game Over")
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
            if (gameState == null) {
                // Loading / connecting state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Connecting...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MutedText,
                    )
                }
            } else {
                // ── Action Market ─────────────────────────────────────────
                ActionMarketSection()

                // ── Deck + Discard ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DeckCard(
                        label = "Draw Pile",
                        clickable = isMyTurn && currentPhase == PHASE_AWAITING_DRAW,
                        onClick = {
                            pendingAction = null
                            onDrawFromDeck()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    DiscardCard(
                        card = discardCard,
                        label = "Discard Pile",
                        clickable = isMyTurn && currentPhase == PHASE_AWAITING_DRAW && discardCard != null,
                        onClick = {
                            pendingAction = null
                            onDrawFromDiscard()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // ── Drawn Card ────────────────────────────────────────────
                if (drawnCard != null && currentPhase == PHASE_AWAITING_REPLACEMENT && isMyTurn) {
                    DrawnCardSection(card = drawnCard)
                }

                // ── Player Grid ───────────────────────────────────────────
                if (myBoard != null) {
                    SectionCard(
                        title = "Your Grid",
                        badge = "Score: $myScore",
                    ) {
                        CardGrid(
                            board = myBoard,
                            selectable = isMyTurn && currentPhase == PHASE_AWAITING_REPLACEMENT && pendingAction != null,
                            onCardClick = { row, col ->
                                when (pendingAction) {
                                    "REPLACE" -> {
                                        onReplaceCard(row, col)
                                        pendingAction = null
                                    }
                                    "DISCARD_AND_REVEAL" -> {
                                        onDiscardAndReveal(row, col)
                                        pendingAction = null
                                    }
                                }
                            },
                        )
                    }
                }

                // ── Other Players ─────────────────────────────────────────
                val others = gameState.players.filter { it.playerId != myPlayerId }
                if (others.isNotEmpty()) {
                    SectionCard(title = "Other Players") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            others.forEach { player ->
                                OtherPlayerRow(
                                    player = player,
                                    totalScore = gameState.totalScores
                                        .find { it.playerId == player.playerId }?.totalScore ?: 0,
                                    isCurrentPlayer = player.playerId == gameState.currentPlayerId,
                                )
                            }
                        }
                    }
                }

                // ── Hand Action Cards ─────────────────────────────────────
                HandActionCardsSection(cards = actionCards)

                // Game over banner
                if (gameState.gameOver) {
                    val winner = gameState.totalScores.minByOrNull { it.totalScore }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = PrimaryGreen,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "GAME OVER",
                                style = MaterialTheme.typography.titleLarge,
                                color = SurfaceWhite,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            if (winner != null) {
                                Text(
                                    text = "Winner: ${winner.nickname} (${winner.totalScore} pts)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MintGreen,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Bottom Action Bar ─────────────────────────────────────────────
        if (gameState != null) {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when {
                        gameState.gameOver -> {
                            Text(
                                text = "Game finished!",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        currentPhase == PHASE_ROUND_FINISHED -> {
                            Text(
                                text = "Round finished – next round starting…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        !isMyTurn -> {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = BlueSurface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Waiting for $currentPlayerNickname…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                        }

                        currentPhase == PHASE_AWAITING_DRAW || currentPhase == PHASE_FINAL_TURNS -> {
                            PrimaryButton(
                                text = "DRAW FROM DECK",
                                onClick = {
                                    pendingAction = null
                                    onDrawFromDeck()
                                },
                            )
                            SecondaryButton(
                                text = if (discardCard != null)
                                    "Draw from Discard (${discardCard.value})"
                                else "Draw from Discard",
                                onClick = {
                                    pendingAction = null
                                    onDrawFromDiscard()
                                },
                            )
                        }

                        currentPhase == PHASE_AWAITING_REPLACEMENT -> {
                            Text(
                                text = "Choose an action, then tap a card on your grid:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText,
                            )
                            PrimaryButton(
                                text = if (pendingAction == "REPLACE") "✓ Replace Mode — tap a card" else "REPLACE CARD",
                                onClick = { pendingAction = "REPLACE" },
                            )
                            SecondaryButton(
                                text = if (pendingAction == "DISCARD_AND_REVEAL") "✓ Reveal Mode — tap a card" else "Discard & Reveal",
                                onClick = { pendingAction = "DISCARD_AND_REVEAL" },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun PlayerPill(
    score: TotalScore,
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
                text = score.nickname,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) SurfaceWhite else MutedText,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${score.totalScore} pts",
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) MintGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DeckCard(
    label: String,
    clickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = if (clickable) 6.dp else 2.dp,
        modifier = modifier.then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
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
                            colors = if (clickable) listOf(MintGreen, PrimaryGreen)
                            else listOf(PrimaryGreen, GreenDark),
                        ),
                        shape = MaterialTheme.shapes.medium,
                    )
                    .then(
                        if (clickable) Modifier.border(2.dp, MintGreen, MaterialTheme.shapes.medium)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (clickable) "TAP" else "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SurfaceWhite,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (clickable) PrimaryGreen else MutedText,
                fontWeight = if (clickable) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DiscardCard(
    card: Card?,
    label: String,
    clickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = if (clickable) 6.dp else 2.dp,
        modifier = modifier.then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
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
                        color = if (card != null) cardColor(card.value) else CardHiddenBg,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .border(
                        width = if (clickable) 2.dp else 1.dp,
                        color = if (clickable) PrimaryGreen else BorderColor,
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (card != null) cardDisplayValue(card) else "—",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (clickable) PrimaryGreen else MutedText,
                fontWeight = if (clickable) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DrawnCardSection(card: Card) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "DRAWN CARD",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .background(
                        color = cardColor(card.value),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(2.dp, PrimaryGreen, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cardDisplayValue(card),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                )
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
                        color = at.aau.se2.skyjo.ui.theme.GreenSurface,
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
private fun CardGrid(
    board: List<List<BoardSlot>>,
    selectable: Boolean,
    onCardClick: (row: Int, col: Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        board.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { colIndex, slot ->
                    BoardSlotTile(
                        slot = slot,
                        selectable = selectable && slot.type == "OCCUPIED",
                        onClick = { onCardClick(rowIndex, colIndex) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardSlotTile(
    slot: BoardSlot,
    selectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (slot.type) {
        "CLEARED" -> {
            Box(
                modifier = modifier
                    .aspectRatio(0.65f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Transparent),
            )
        }
        else -> {
            val isFaceUp = slot.faceUp == true
            val cardValue = if (isFaceUp) slot.card?.value else null
            val bgColor = when {
                !isFaceUp -> CardHiddenBg
                cardValue != null && cardValue <= 0 -> CardNegativeBg
                else -> CardPositiveBg
            }
            val displayText = when {
                !isFaceUp -> "?"
                slot.card?.type == "ACTION" -> "A"
                cardValue != null -> cardValue.toString()
                else -> "?"
            }

            Box(
                modifier = modifier
                    .aspectRatio(0.65f)
                    .background(color = bgColor, shape = RoundedCornerShape(10.dp))
                    .border(
                        width = if (selectable) 2.dp else 1.dp,
                        color = if (selectable) PrimaryGreen
                        else if (!isFaceUp) MintGreen.copy(alpha = 0.4f)
                        else BorderColor,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .then(if (selectable) Modifier.clickable(onClick = onClick) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (!isFaceUp) CardHiddenText else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actionCards.forEach { action ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = at.aau.se2.skyjo.ui.theme.GreenSurface,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = action,
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
private fun HandActionCardsSection(cards: List<String>) {
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
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = at.aau.se2.skyjo.ui.theme.GreenSurface,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.65f)
                            .border(
                                width = 1.dp,
                                color = MintGreen.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                            ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = card,
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
private fun OtherPlayerRow(
    player: GamePlayerState,
    totalScore: Int,
    isCurrentPlayer: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrentPlayer) at.aau.se2.skyjo.ui.theme.GreenSurface
        else MaterialTheme.colorScheme.surface,
        border = if (isCurrentPlayer)
            androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
        else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = player.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentPlayer) PrimaryGreen else MutedText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isCurrentPlayer) MintGreen else MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.CircleShape,
                    )
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = player.nickname,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$totalScore pts",
                style = MaterialTheme.typography.labelLarge,
                color = if (isCurrentPlayer) PrimaryGreen else MutedText,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun cardColor(value: Int): Color = when {
    value <= 0 -> CardNegativeBg
    else -> CardPositiveBg
}

private fun cardDisplayValue(card: Card): String =
    if (card.type == "ACTION") "A" else card.value.toString()

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GameScreenPreview() {
    SkyjoTheme {
        GameScreen(
            gameState = GameUpdateMessage(
                phase = PHASE_AWAITING_DRAW,
                currentPlayerId = "player1",
                roundNumber = 1,
                gameOver = false,
                totalScores = listOf(
                    TotalScore("player1", "Alice", 12),
                    TotalScore("player2", "Bob", 8),
                ),
                players = listOf(
                    GamePlayerState(
                        playerId = "player1",
                        nickname = "Alice",
                        board = List(3) {
                            List(4) {
                                BoardSlot(type = "OCCUPIED", faceUp = false)
                            }
                        }
                    )
                ),
                discardTopCard = Card(id = 1, value = 4, type = "NUMBER"),
            ),
            myPlayerId = "player1",
            isMyTurn = true,
            onBack = {},
        )
    }
}
