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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.RoundResult
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

private sealed class SwapSelectionState {
    data class AwaitingFirst(val cardIndex: Int) : SwapSelectionState()
    data class AwaitingSecond(
        val cardIndex: Int,
        val player1Id: String,
        val player1Row: Int,
        val player1Col: Int,
    ) : SwapSelectionState()
}

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
    onDrawFromActionDeck: () -> Unit = {},
    onDrawVisibleActionCard: (actionCardIndex: Int) -> Unit = {},
    onReplaceCard: (row: Int, col: Int) -> Unit = { _, _ -> },
    onDiscardAndReveal: (row: Int, col: Int) -> Unit = { _, _ -> },
    onPlayPlayerSwapCard: (actionCardIndex: Int, p1Id: String, p1Row: Int, p1Col: Int, p2Id: String, p2Row: Int, p2Col: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onPlayActionCard: (actionCardIndex: Int) -> Unit = {},
    onDiscardActionCard: (actionCardIndex: Int) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var swapState by remember(isMyTurn, gameState?.roundNumber) { mutableStateOf<SwapSelectionState?>(null) }

    val currentPhase = gameState?.phase
    val myPlayer = gameState?.players?.find { it.playerId == myPlayerId }
    val myBoard = myPlayer?.board
    val myActionCards = myPlayer?.actionCards.orEmpty()
    val myVisibleScore = myBoard
        ?.flatten()
        ?.filter { it.type == "OCCUPIED" && it.faceUp == true }
        ?.mapNotNull { it.card?.value }
        ?.sum() ?: 0
    val currentPlayerNickname = gameState?.players
        ?.find { it.playerId == gameState.currentPlayerId }?.nickname ?: ""
    val discardCard = gameState?.discardTopCard
    val drawnCard = gameState?.drawnCard

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray),
    ) {
        GameScreenHeader(
            gameState = gameState,
            isMyTurn = isMyTurn,
            currentPlayerNickname = currentPlayerNickname,
            currentPhase = currentPhase,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (gameState == null) {
                ConnectingPlaceholder()
            } else {
                GameContent(
                    gameState = gameState,
                    myPlayerId = myPlayerId,
                    myBoard = myBoard,
                    myVisibleScore = myVisibleScore,
                    discardCard = discardCard,
                    drawnCard = drawnCard,
                    isMyTurn = isMyTurn,
                    currentPhase = currentPhase,
                    pendingAction = pendingAction,
                    swapState = swapState,
                    onPendingActionChange = { pendingAction = it },
                    onSwapStateChange = { swapState = it },
                    onDrawFromDeck = onDrawFromDeck,
                    onDrawFromDiscard = onDrawFromDiscard,
                    onDrawFromActionDeck = onDrawFromActionDeck,
                    onDrawVisibleActionCard = onDrawVisibleActionCard,
                    onReplaceCard = onReplaceCard,
                    onDiscardAndReveal = onDiscardAndReveal,
                    myActionCards = myActionCards,
                    onPlayPlayerSwapCard = onPlayPlayerSwapCard,
                    onPlayActionCard = onPlayActionCard,
                    onDiscardActionCard = onDiscardActionCard,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (gameState != null) {
            GameScreenActionBar(
                gameState = gameState,
                currentPhase = currentPhase,
                isMyTurn = isMyTurn,
                currentPlayerNickname = currentPlayerNickname,
                discardCard = discardCard,
                pendingAction = pendingAction,
                onPendingActionChange = { pendingAction = it },
                onDrawFromDeck = onDrawFromDeck,
                onDrawFromDiscard = onDrawFromDiscard,
            )
        }
    }
}

// ── Top-level section composables ────────────────────────────────────────────

@Composable
private fun GameScreenHeader(
    gameState: GameUpdateMessage?,
    isMyTurn: Boolean,
    currentPlayerNickname: String,
    currentPhase: String?,
    onBack: () -> Unit,
) {
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
}

@Composable
private fun ConnectingPlaceholder() {
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
}

@Composable
private fun GameContent(
    gameState: GameUpdateMessage,
    myPlayerId: String?,
    myBoard: List<List<BoardSlot>>?,
    myVisibleScore: Int,
    discardCard: Card?,
    drawnCard: Card?,
    isMyTurn: Boolean,
    currentPhase: String?,
    pendingAction: String?,
    swapState: SwapSelectionState?,
    onPendingActionChange: (String?) -> Unit,
    onSwapStateChange: (SwapSelectionState?) -> Unit,
    onDrawFromDeck: () -> Unit,
    onDrawFromDiscard: () -> Unit,
    onDrawFromActionDeck: () -> Unit,
    onDrawVisibleActionCard: (actionCardIndex: Int) -> Unit,
    onReplaceCard: (row: Int, col: Int) -> Unit,
    onDiscardAndReveal: (row: Int, col: Int) -> Unit,
    myActionCards: List<ActionCard>,
    onPlayPlayerSwapCard: (Int, String, Int, Int, String, Int, Int) -> Unit,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
) {
    val isDrawPhase = currentPhase == PHASE_AWAITING_DRAW
    val isReplacementPhase = currentPhase == PHASE_AWAITING_REPLACEMENT

    ActionMarketSection(
        cards = gameState.visibleActionCards,
        actionDrawPileCount = gameState.actionDrawPileCount,
        clickable = isMyTurn && isDrawPhase,
        onDrawFromActionDeck = onDrawFromActionDeck,
        onDrawVisibleActionCard = onDrawVisibleActionCard,
    )

    DrawPileRow(
        isMyTurn = isMyTurn,
        isDrawPhase = isDrawPhase,
        discardCard = discardCard,
        onPendingActionChange = onPendingActionChange,
        onDrawFromDeck = onDrawFromDeck,
        onDrawFromDiscard = onDrawFromDiscard,
    )

    if (drawnCard != null && isReplacementPhase && isMyTurn) {
        DrawnCardSection(card = drawnCard)
    }

    if (myBoard != null) {
        PlayerGridSection(
            myBoard = myBoard,
            myVisibleScore = myVisibleScore,
            isMyTurn = isMyTurn,
            isReplacementPhase = isReplacementPhase,
            pendingAction = pendingAction,
            swapState = swapState,
            myPlayerId = myPlayerId ?: "",
            onPendingActionChange = onPendingActionChange,
            onReplaceCard = onReplaceCard,
            onDiscardAndReveal = onDiscardAndReveal,
            onSwapSlotSelected = { row, col ->
                if (myPlayerId != null) {
                    when (val state = swapState) {
                        is SwapSelectionState.AwaitingFirst -> {
                            onSwapStateChange(SwapSelectionState.AwaitingSecond(state.cardIndex, myPlayerId, row, col))
                        }
                        is SwapSelectionState.AwaitingSecond -> {
                            if (myPlayerId != state.player1Id) {
                                onPlayPlayerSwapCard(
                                    state.cardIndex,
                                    state.player1Id,
                                    state.player1Row,
                                    state.player1Col,
                                    myPlayerId,
                                    row,
                                    col,
                                )
                                onSwapStateChange(null)
                            }
                        }
                        null -> Unit
                    }
                }
            },
        )
    }

    val others = gameState.players.filter { it.playerId != myPlayerId }
    if (others.isNotEmpty()) {
        OtherPlayersSection(
            players = others,
            totalScores = gameState.totalScores,
            currentPlayerId = gameState.currentPlayerId,
            disconnectedPlayers = gameState.disconnectedPlayers,
            swapState = swapState,
            onSlotSelected = { playerId, row, col ->
                when (val state = swapState) {
                    is SwapSelectionState.AwaitingFirst -> {
                        onSwapStateChange(SwapSelectionState.AwaitingSecond(state.cardIndex, playerId, row, col))
                    }
                    is SwapSelectionState.AwaitingSecond -> {
                        if (playerId != state.player1Id) {
                            onPlayPlayerSwapCard(
                                state.cardIndex,
                                state.player1Id,
                                state.player1Row,
                                state.player1Col,
                                playerId,
                                row,
                                col,
                            )
                            onSwapStateChange(null)
                        }
                    }
                    null -> Unit
                }
            },
        )
    }

    HandActionCardsSection(
        cards = myActionCards,
        canUseCards = isMyTurn && isDrawPhase,
        swapState = swapState,
        onPlayActionCard = { index ->
            if (myActionCards.getOrNull(index)?.kind == "PLAYER_SWAP") {
                onSwapStateChange(SwapSelectionState.AwaitingFirst(index))
            } else {
                onPlayActionCard(index)
            }
        },
        onDiscardActionCard = { index ->
            onDiscardActionCard(index)
            onSwapStateChange(null)
        },
    )

    if (gameState.roundResult != null) {
        RoundResultSection(
            roundResult = gameState.roundResult,
            players = gameState.players,
            roundNumber = gameState.roundNumber,
        )
    }

    if (gameState.gameOver) {
        GameOverBanner(totalScores = gameState.totalScores)
    }
}

@Composable
private fun DrawPileRow(
    isMyTurn: Boolean,
    isDrawPhase: Boolean,
    discardCard: Card?,
    onPendingActionChange: (String?) -> Unit,
    onDrawFromDeck: () -> Unit,
    onDrawFromDiscard: () -> Unit,
) {
    val deckClickable = isMyTurn && isDrawPhase
    val discardClickable = isMyTurn && isDrawPhase && discardCard != null
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeckCard(
            label = "Draw Pile",
            clickable = deckClickable,
            onClick = {
                onPendingActionChange(null)
                onDrawFromDeck()
            },
            modifier = Modifier.weight(1f),
        )
        DiscardCard(
            card = discardCard,
            label = "Discard Pile",
            clickable = discardClickable,
            onClick = {
                onPendingActionChange(null)
                onDrawFromDiscard()
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlayerGridSection(
    myBoard: List<List<BoardSlot>>,
    myVisibleScore: Int,
    isMyTurn: Boolean,
    isReplacementPhase: Boolean,
    pendingAction: String?,
    swapState: SwapSelectionState?,
    myPlayerId: String,
    onPendingActionChange: (String?) -> Unit,
    onReplaceCard: (row: Int, col: Int) -> Unit,
    onDiscardAndReveal: (row: Int, col: Int) -> Unit,
    onSwapSlotSelected: (row: Int, col: Int) -> Unit,
) {
    val canSelectForSwap = when (val state = swapState) {
        is SwapSelectionState.AwaitingFirst -> myPlayerId.isNotBlank()
        is SwapSelectionState.AwaitingSecond -> myPlayerId.isNotBlank() && state.player1Id != myPlayerId
        null -> false
    }

    SectionCard(
        title = "Your Grid",
        badge = "Visible: $myVisibleScore",
    ) {
        CardGrid(
            board = myBoard,
            selectable = canSelectForSwap || (isMyTurn && isReplacementPhase && pendingAction != null),
            onCardClick = { row, col ->
                if (canSelectForSwap) {
                    onSwapSlotSelected(row, col)
                } else {
                    when (pendingAction) {
                        "REPLACE" -> {
                            onReplaceCard(row, col)
                            onPendingActionChange(null)
                        }
                        "DISCARD_AND_REVEAL" -> {
                            onDiscardAndReveal(row, col)
                            onPendingActionChange(null)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun OtherPlayersSection(
    players: List<GamePlayerState>,
    totalScores: List<TotalScore>,
    currentPlayerId: String?,
    disconnectedPlayers: List<String>,
    swapState: SwapSelectionState?,
    onSlotSelected: (playerId: String, row: Int, col: Int) -> Unit,
) {
    SectionCard(title = "Other Players") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            players.forEach { player ->
                OtherPlayerRow(
                    player = player,
                    totalScore = totalScores.find { it.playerId == player.playerId }?.totalScore ?: 0,
                    isCurrentPlayer = player.playerId == currentPlayerId,
                    isDisconnected = player.nickname in disconnectedPlayers,
                    swapState = swapState,
                    onSlotSelected = { row, col -> onSlotSelected(player.playerId, row, col) },
                )
            }
        }
    }
}

@Composable
private fun GameOverBanner(totalScores: List<TotalScore>) {
    val winner = totalScores.minByOrNull { it.totalScore }
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

@Composable
private fun GameScreenActionBar(
    gameState: GameUpdateMessage,
    currentPhase: String?,
    isMyTurn: Boolean,
    currentPlayerNickname: String,
    discardCard: Card?,
    pendingAction: String?,
    onPendingActionChange: (String?) -> Unit,
    onDrawFromDeck: () -> Unit,
    onDrawFromDiscard: () -> Unit,
) {
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
                            onPendingActionChange(null)
                            onDrawFromDeck()
                        },
                    )
                    SecondaryButton(
                        text = if (discardCard != null)
                            "Draw from Discard (${discardCard.value})"
                        else "Draw from Discard",
                        onClick = {
                            onPendingActionChange(null)
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
                        onClick = { onPendingActionChange("REPLACE") },
                    )
                    SecondaryButton(
                        text = if (pendingAction == "DISCARD_AND_REVEAL") "✓ Reveal Mode — tap a card" else "Discard & Reveal",
                        onClick = { onPendingActionChange("DISCARD_AND_REVEAL") },
                    )
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
private fun RoundResultSection(
    roundResult: RoundResult,
    players: List<GamePlayerState>,
    roundNumber: Int,
) {
    val finisherNickname = players.find { it.playerId == roundResult.finisherPlayerId }?.nickname
        ?: roundResult.finisherPlayerId

    SectionCard(title = "Round $roundNumber Results") {
        if (finisherNickname.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = at.aau.se2.skyjo.ui.theme.GreenSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "$finisherNickname finished the round",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Player",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Round",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp),
            )
            Text(
                text = "Final",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        roundResult.scores.forEach { score ->
            val nickname = players.find { it.playerId == score.playerId }?.nickname
                ?: score.playerId
            val isFinisher = score.playerId == roundResult.finisherPlayerId

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isFinisher) "$nickname ★" else nickname,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isFinisher) FontWeight.Bold else FontWeight.Normal,
                    color = if (isFinisher) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${score.rawScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
                Text(
                    text = "${score.finalScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (score.finalScore <= 0) at.aau.se2.skyjo.ui.theme.CardNegativeBg
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionMarketSection(
    cards: List<ActionCard>,
    actionDrawPileCount: Int,
    clickable: Boolean = false,
    onDrawFromActionDeck: () -> Unit = {},
    onDrawVisibleActionCard: (actionCardIndex: Int) -> Unit = {},
) {
    val deckClickable = clickable && actionDrawPileCount > 0

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
                                colors = if (deckClickable) listOf(MintGreen, PrimaryGreen)
                                else listOf(PrimaryGreen, GreenDark),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = 2.dp,
                            color = if (deckClickable) MintGreen else MintGreen.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .then(if (deckClickable) Modifier.clickable(onClick = onDrawFromActionDeck) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (deckClickable) {
                        "TAP to draw action card ($actionDrawPileCount left)"
                    } else {
                        "Action Draw Deck ($actionDrawPileCount left)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (deckClickable) PrimaryGreen else MutedText,
                    fontWeight = if (deckClickable) FontWeight.Bold else FontWeight.Normal,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (cards.isEmpty()) {
                Text(
                    text = "No visible action cards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cards.forEachIndexed { index, action ->
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = at.aau.se2.skyjo.ui.theme.GreenSurface,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("draw_visible_action_card_$index")
                                .then(
                                    if (clickable) {
                                        Modifier.clickable { onDrawVisibleActionCard(index) }
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            ActionCardFaceContent(
                                card = action,
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandActionCardsSection(
    cards: List<ActionCard>,
    canUseCards: Boolean,
    swapState: SwapSelectionState?,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
) {
    val activeCardIndex = when (swapState) {
        is SwapSelectionState.AwaitingFirst -> swapState.cardIndex
        is SwapSelectionState.AwaitingSecond -> swapState.cardIndex
        null -> null
    }

    SectionCard(title = "Your Action Cards", badge = "${cards.size} cards") {
        if (cards.isEmpty()) {
            EmptyActionCardHandMessage()
        } else {
            HandActionCardsRow(
                cards = cards,
                canUseCards = canUseCards && swapState == null,
                activeCardIndex = activeCardIndex,
                onPlayActionCard = onPlayActionCard,
                onDiscardActionCard = onDiscardActionCard,
            )
        }
    }
}

@Composable
private fun EmptyActionCardHandMessage() {
    Text(
        text = "No action cards in hand",
        style = MaterialTheme.typography.bodyMedium,
        color = MutedText,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun HandActionCardsRow(
    cards: List<ActionCard>,
    canUseCards: Boolean,
    activeCardIndex: Int?,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEachIndexed { index, card ->
            HandActionCardItem(
                index = index,
                card = card,
                canUseCards = canUseCards,
                isActive = activeCardIndex == index,
                onPlayActionCard = onPlayActionCard,
                onDiscardActionCard = onDiscardActionCard,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HandActionCardItem(
    index: Int,
    card: ActionCard,
    canUseCards: Boolean,
    isActive: Boolean,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = at.aau.se2.skyjo.ui.theme.GreenSurface,
            modifier = actionCardModifier(
                index = index,
                card = card,
                canUseCards = canUseCards,
                isActive = isActive,
                onPlayActionCard = onPlayActionCard,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                ActionCardFaceContent(
                    card = card,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        DiscardActionCardButton(
            index = index,
            canUseCards = canUseCards,
            onDiscardActionCard = onDiscardActionCard,
        )
    }
}

private fun actionCardModifier(
    index: Int,
    card: ActionCard,
    canUseCards: Boolean,
    isActive: Boolean,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
): Modifier {
    val clickModifier = if (canUseCards) {
        Modifier
            .testTag("play_action_card_$index")
            .clickable { onPlayActionCard(index) }
    } else {
        Modifier
    }

    return Modifier
        .aspectRatio(0.65f)
        .border(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) MintGreen else MintGreen.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
        )
        .then(clickModifier)
        .semantics {
            contentDescription = "Play ${actionCardAccessibilityLabel(card)} action card"
        }
}

@Composable
private fun DiscardActionCardButton(
    index: Int,
    canUseCards: Boolean,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
) {
    val clickModifier = if (canUseCards) {
        Modifier.clickable { onDiscardActionCard(index) }
    } else {
        Modifier
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discard_action_card_$index")
            .then(clickModifier),
    ) {
        Text(
            text = "Discard",
            style = MaterialTheme.typography.labelSmall,
            color = if (canUseCards) PrimaryGreen else MutedText,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun OtherPlayerRow(
    player: GamePlayerState,
    totalScore: Int,
    isCurrentPlayer: Boolean,
    isDisconnected: Boolean = false,
    swapState: SwapSelectionState?,
    onSlotSelected: (row: Int, col: Int) -> Unit,
) {
    val avatarBg = when {
        isDisconnected -> Color(0xFFFFCDD2)
        isCurrentPlayer -> MintGreen
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val avatarTextColor = when {
        isDisconnected -> Color(0xFFB71C1C)
        isCurrentPlayer -> PrimaryGreen
        else -> MutedText
    }
    val surfaceColor = when {
        isDisconnected -> Color(0xFFFFF8F8)
        isCurrentPlayer -> at.aau.se2.skyjo.ui.theme.GreenSurface
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        isDisconnected -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A))
        isCurrentPlayer -> androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
        else -> null
    }
    val isSelectableForSwap = when (val state = swapState) {
        is SwapSelectionState.AwaitingFirst -> true
        is SwapSelectionState.AwaitingSecond -> state.player1Id != player.playerId
        null -> false
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = surfaceColor,
        border = if (isSelectableForSwap) {
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen)
        } else {
            border
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = player.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = avatarTextColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(36.dp)
                    .background(color = avatarBg, shape = androidx.compose.foundation.shape.CircleShape)
                    .padding(8.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDisconnected) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurface,
                )
                if (isDisconnected) {
                    Text(
                        text = "Disconnected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF5350),
                    )
                }
                if (isSelectableForSwap) {
                    Text(
                        text = "Tap a card to swap",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                    )
                }
            }
            Text(
                text = "$totalScore pts",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isDisconnected -> Color(0xFFEF9A9A)
                    isCurrentPlayer -> PrimaryGreen
                    else -> MutedText
                },
                fontWeight = FontWeight.Bold,
            )
        }
            if (isSelectableForSwap) {
                CardGrid(
                    board = player.board,
                    selectable = true,
                    onCardClick = onSlotSelected,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun cardColor(value: Int): Color = when {
    value <= 0 -> CardNegativeBg
    else -> CardPositiveBg
}

private fun cardDisplayValue(card: Card): String =
    if (card.type == "ACTION") "A" else card.value.toString()

private fun actionCardDisplayLabel(card: ActionCard): String =
    when (card.kind) {
        "DEFENSE" -> "🛡️"
        "PLAYER_SWAP" -> "↔"
        "DOUBLE_TURN" -> "⏩"
        else -> "Action"
    }

private fun actionCardAccessibilityLabel(card: ActionCard): String =
    when (card.kind) {
        "DEFENSE" -> "Defense"
        "PLAYER_SWAP" -> "Player swap"
        "DOUBLE_TURN" -> "Double turn"
        else -> "Action"
    }

@Composable
private fun ActionCardFaceContent(
    card: ActionCard,
    modifier: Modifier = Modifier,
) {
    Text(
        text = actionCardDisplayLabel(card),
        style = if (card.kind == "DEFENSE" || card.kind == "PLAYER_SWAP" || card.kind == "DOUBLE_TURN") {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.labelMedium
        },
        color = PrimaryGreen,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

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
