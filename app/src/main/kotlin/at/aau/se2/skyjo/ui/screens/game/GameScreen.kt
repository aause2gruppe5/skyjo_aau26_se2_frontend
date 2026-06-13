package at.aau.se2.skyjo.ui.screens.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.PlayActionCardCommand
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
import at.aau.se2.skyjo.model.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private sealed class SwapSelectionState {
    data class AwaitingFirst(
        val cardIndex: Int,
        val ownCardsOnly: Boolean = false,
    ) : SwapSelectionState()

    data class AwaitingSecond(
        val cardIndex: Int,
        val player1Id: String,
        val player1Row: Int,
        val player1Col: Int,
        val ownCardsOnly: Boolean = false,
    ) : SwapSelectionState()
}

private const val ACTION_CARD_KIND_ENLIGHTENMENT = "ENLIGHTENMENT"
private const val ACTION_CARD_KIND_PLAYER_SWAP = "PLAYER_SWAP"
private const val ACTION_CARD_KIND_SWAP_OWN_CARDS = "SWAP_OWN_CARDS"
private const val ACTION_CARD_KIND_DOUBLE_TURN = "DOUBLE_TURN"
private const val ACTION_CARD_RESULT_ENLIGHTENMENT = "ENLIGHTENMENT"
private const val PHASE_AWAITING_DRAW = "AWAITING_DRAW"
private const val PHASE_AWAITING_REPLACEMENT = "AWAITING_REPLACEMENT"
private const val PHASE_ROUND_FINISHED = "ROUND_FINISHED"
private const val PHASE_FINAL_TURNS = "FINAL_TURNS"
private const val SHAKE_THRESHOLD = 18f
private const val SHAKE_COOLDOWN_MS = 1_500L
private const val SCORE_PENALTY_FLASH_MS = 900L

@Composable
fun GameScreen(
    gameState: GameUpdateMessage? = null,
    myPlayerId: String? = null,
    isMyTurn: Boolean = false,
    isHost: Boolean = false,
    privateActionCardResult: ActionCardResultMessage? = null,
    privateCheatPeekResult: CheatPeekResultMessage? = null,
    onDrawFromDeck: () -> Unit = {},
    onDrawFromDiscard: () -> Unit = {},
    onDrawFromActionDeck: () -> Unit = {},
    onDrawVisibleActionCard: (actionCardIndex: Int) -> Unit = {},
    onReplaceCard: (row: Int, col: Int) -> Unit = { _, _ -> },
    onDiscardAndReveal: (row: Int, col: Int) -> Unit = { _, _ -> },
    onPlayPlayerSwapCard: (actionCardIndex: Int, p1Id: String, p1Row: Int, p1Col: Int, p2Id: String, p2Row: Int, p2Col: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onPlaySwapOwnCards: (actionCardIndex: Int, firstRow: Int, firstCol: Int, secondRow: Int, secondCol: Int) -> Unit = { _, _, _, _, _ -> },
    onPlayActionCard: (actionCardIndex: Int) -> Unit = {},
    onPlayEnlightenmentCard: (PlayActionCardCommand) -> Unit = {},
    onDiscardActionCard: (actionCardIndex: Int) -> Unit = {},
    onDismissActionCardResult: () -> Unit = {},
    onCheatPeekDrawPile: () -> Unit = {},
    onReportCheat: () -> Unit = {},
    onDismissCheatPeekResult: () -> Unit = {},
    onReadyForNextRoundClick: () -> Unit = {},
    onBack: () -> Unit,
    onNavigateToGameOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingAction by remember { mutableStateOf<String?>(null) }

    var activeRoundResultDialog by remember {
        mutableStateOf<Pair<Int, RoundResult>?>(null)
    }

    var isStartingNextRound by remember { mutableStateOf(false) }

    LaunchedEffect(gameState?.roundResult) {
        val result = gameState?.roundResult
        val number = gameState?.roundNumber
        if (result != null && number != null) {
            activeRoundResultDialog = Pair(number, result)
            isStartingNextRound = false
        }else {
            // Neue Runde gestartet (Server hat das Ergebnis gelöscht) -> Pop-up schließen!
            activeRoundResultDialog = null
        }
    }

    LaunchedEffect(gameState?.gameOver, gameState?.roundResult) {
        if (gameState?.gameOver == true && gameState.roundResult == null) {
            onNavigateToGameOver()
        }
    }

    var swapState by remember(isMyTurn, gameState?.roundNumber) { mutableStateOf<SwapSelectionState?>(null) }
    var pendingEnlightenmentCardIndex by remember(isMyTurn, gameState?.roundNumber) {
        mutableStateOf<Int?>(null)
    }
    var viewedPlayerIndex by remember(isMyTurn, gameState?.roundNumber) {
        mutableStateOf(
            gameState?.players?.indexOfFirst { it.playerId == myPlayerId }?.coerceAtLeast(0) ?: 0
        )
    }
    var reportedCheatTurnId by remember(gameState?.roundNumber) { mutableStateOf<Int?>(null) }
    var previousTotalScores by remember { mutableStateOf<Map<String, Int>?>(null) }
    var penaltyFlashPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

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

    val currentTotalScores = gameState?.totalScores?.associate { it.playerId to it.totalScore }

    SideEffect {
        val previousScores = previousTotalScores
        previousTotalScores = currentTotalScores

        if (gameState != null && currentTotalScores != null && previousScores != null &&
            gameState.roundResult == null && gameState.phase != PHASE_ROUND_FINISHED
        ) {
            val penalizedPlayerIds = currentTotalScores
                .filter { (playerId, totalScore) -> totalScore > (previousScores[playerId] ?: totalScore) }
                .keys
            if (penalizedPlayerIds.isNotEmpty()) {
                penaltyFlashPlayerIds = penaltyFlashPlayerIds + penalizedPlayerIds
            }
        }
    }

    LaunchedEffect(penaltyFlashPlayerIds) {
        if (penaltyFlashPlayerIds.isNotEmpty()) {
            delay(SCORE_PENALTY_FLASH_MS)
            penaltyFlashPlayerIds = emptySet()
        }
    }

    val canCheatPeek = gameState != null &&
        !gameState.gameOver &&
        isMyTurn &&
        privateCheatPeekResult == null &&
        (currentPhase == PHASE_AWAITING_DRAW || currentPhase == PHASE_FINAL_TURNS)

    ShakeCheatDetector(
        enabled = canCheatPeek,
        onShake = onCheatPeekDrawPile,
    )

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
            penaltyFlashPlayerIds = penaltyFlashPlayerIds,
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
                    hasReportedCheatThisTurn = reportedCheatTurnId == gameState.turnId,
                    pendingAction = pendingAction,
                    swapState = swapState,
                    pendingEnlightenmentCardIndex = pendingEnlightenmentCardIndex,
                    viewedPlayerIndex = viewedPlayerIndex,
                    onViewedPlayerIndexChange = { viewedPlayerIndex = it },
                    onPendingActionChange = { pendingAction = it },
                    onSwapStateChange = { swapState = it },
                    onPendingEnlightenmentCardIndexChange = { pendingEnlightenmentCardIndex = it },
                    onDrawFromDeck = onDrawFromDeck,
                    onDrawFromDiscard = onDrawFromDiscard,
                    onDrawFromActionDeck = onDrawFromActionDeck,
                    onDrawVisibleActionCard = onDrawVisibleActionCard,
                    onReplaceCard = onReplaceCard,
                    onDiscardAndReveal = onDiscardAndReveal,
                    myActionCards = myActionCards,
                    onPlayPlayerSwapCard = onPlayPlayerSwapCard,
                    onPlaySwapOwnCards = onPlaySwapOwnCards,
                    onPlayActionCard = onPlayActionCard,
                    onPlayEnlightenmentCard = onPlayEnlightenmentCard,
                    onDiscardActionCard = onDiscardActionCard,
                    onReportCheat = {
                        reportedCheatTurnId = gameState.turnId
                        onReportCheat()
                    },
                    enlightenmentResult = if (privateActionCardResult?.type == ACTION_CARD_RESULT_ENLIGHTENMENT) privateActionCardResult else null,
                    onDismissEnlightenmentResult = onDismissActionCardResult,
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

    if (privateCheatPeekResult != null) {
        CheatPeekDialog(
            result = privateCheatPeekResult,
            onDismiss = onDismissCheatPeekResult,
        )
    }

    val dialogData = activeRoundResultDialog
    val isGameOver = gameState?.gameOver == true
    if (dialogData != null) {

        Dialog(onDismissRequest = { /* Leer lassen, damit man es nicht wegklicken kann */ }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = SurfaceWhite,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Hier wird deine bestehende UI-Komponente aufgerufen!
                    RoundResultSection(
                        roundResult = dialogData.second,
                        players = gameState?.players ?: emptyList(),
                        roundNumber = dialogData.first
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isGameOver) {
                        PrimaryButton(
                            text = "Game Over! See Results",
                            onClick = {
                                activeRoundResultDialog = null
                                onNavigateToGameOver()
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                        )
                    } else if (isHost) {
                        PrimaryButton(
                            text = if (isStartingNextRound) "Starting..." else "Start next Round",
                            enabled = !isStartingNextRound, // <--- Verhindert Spam-Klicks
                            onClick = {
                                // WICHTIG: activeRoundResultDialog = null WURDE HIER GELÖSCHT!
                                isStartingNextRound = true // Ladeanimation starten
                                onReadyForNextRoundClick() // Kommando ans Backend feuern
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                        )
                    } else {
                        // Die anderen Spieler warten einfach
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = BlueSurface,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                        ) {
                            val hostName = gameState?.players?.firstOrNull()?.nickname ?: "Host"
                            Text(
                                text = "Waiting for $hostName...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun ShakeCheatDetector(
    enabled: Boolean,
    onShake: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(context, enabled) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (!enabled || sensorManager == null || accelerometer == null) {
            onDispose {}
        } else {
            var lastShakeAt = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values.getOrNull(0) ?: return
                    val y = event.values.getOrNull(1) ?: return
                    val z = event.values.getOrNull(2) ?: return
                    val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val now = SystemClock.elapsedRealtime()

                    if (magnitude >= SHAKE_THRESHOLD && now - lastShakeAt >= SHAKE_COOLDOWN_MS) {
                        lastShakeAt = now
                        currentOnShake()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
}

@Composable
private fun CheatPeekDialog(
    result: CheatPeekResultMessage,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = SurfaceWhite,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Cheat Peek",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "Top card of the draw pile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 132.dp)
                        .background(
                            color = cardColor(result.card.value),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .border(2.dp, PrimaryGreen, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = cardDisplayValue(result.card),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    text = "${result.remainingCheatPeeks} peeks left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedText,
                )
                PrimaryButton(
                    text = "OK",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
    penaltyFlashPlayerIds: Set<String>,
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
                            isPenaltyFlashing = score.playerId in penaltyFlashPlayerIds,
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
    hasReportedCheatThisTurn: Boolean,
    pendingAction: String?,
    swapState: SwapSelectionState?,
    pendingEnlightenmentCardIndex: Int?,
    viewedPlayerIndex: Int,
    onViewedPlayerIndexChange: (Int) -> Unit,
    onPendingActionChange: (String?) -> Unit,
    onSwapStateChange: (SwapSelectionState?) -> Unit,
    onPendingEnlightenmentCardIndexChange: (Int?) -> Unit,
    onDrawFromDeck: () -> Unit,
    onDrawFromDiscard: () -> Unit,
    onDrawFromActionDeck: () -> Unit,
    onDrawVisibleActionCard: (actionCardIndex: Int) -> Unit,
    onReplaceCard: (row: Int, col: Int) -> Unit,
    onDiscardAndReveal: (row: Int, col: Int) -> Unit,
    myActionCards: List<ActionCard>,
    onPlayPlayerSwapCard: (Int, String, Int, Int, String, Int, Int) -> Unit,
    onPlaySwapOwnCards: (Int, Int, Int, Int, Int) -> Unit,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
    onPlayEnlightenmentCard: (PlayActionCardCommand) -> Unit,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
    onReportCheat: () -> Unit = {},
    enlightenmentResult: ActionCardResultMessage? = null,
    onDismissEnlightenmentResult: () -> Unit = {},
) {
    val isDrawPhase = currentPhase == PHASE_AWAITING_DRAW
    val isReplacementPhase = currentPhase == PHASE_AWAITING_REPLACEMENT
    val reportsAvailable = gameState.players.find { it.playerId == myPlayerId }?.remainingCheatReports ?: 0
    val canShowReportCheat = myPlayerId != null && !isMyTurn &&
        !gameState.gameOver && currentPhase != PHASE_ROUND_FINISHED
    val reportEnabled = reportsAvailable > 0 && !hasReportedCheatThisTurn
    val activeActionCardIndex = pendingEnlightenmentCardIndex ?: when (swapState) {
        is SwapSelectionState.AwaitingFirst -> swapState.cardIndex
        is SwapSelectionState.AwaitingSecond -> swapState.cardIndex
        null -> null
    }

    if (canShowReportCheat) {
        ReportCheatSection(
            reportsAvailable = reportsAvailable,
            enabled = reportEnabled,
            onReportCheat = onReportCheat,
        )
    }

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

    PlayerGridCarousel(
        players = gameState.players,
        myPlayerId = myPlayerId ?: "",
        myBoard = myBoard ?: emptyList(),
        myVisibleScore = myVisibleScore,
        totalScores = gameState.totalScores,
        disconnectedPlayers = gameState.disconnectedPlayers,
        isMyTurn = isMyTurn,
        isReplacementPhase = isReplacementPhase,
        pendingAction = pendingAction,
        swapState = swapState,
        viewedPlayerIndex = viewedPlayerIndex,
        onViewedPlayerIndexChange = onViewedPlayerIndexChange,
        onPendingActionChange = onPendingActionChange,
        onReplaceCard = onReplaceCard,
        onDiscardAndReveal = onDiscardAndReveal,
        onSwapSlotSelected = { row, col ->
            if (myPlayerId != null) {
                when (val state = swapState) {
                    is SwapSelectionState.AwaitingFirst -> {
                        onSwapStateChange(
                            SwapSelectionState.AwaitingSecond(
                                cardIndex = state.cardIndex,
                                player1Id = myPlayerId,
                                player1Row = row,
                                player1Col = col,
                                ownCardsOnly = state.ownCardsOnly,
                            ),
                        )
                    }
                    is SwapSelectionState.AwaitingSecond -> {
                        if (state.ownCardsOnly && state.player1Id == myPlayerId) {
                            val selectedSameSlot = row == state.player1Row && col == state.player1Col
                            if (!selectedSameSlot) {
                                onPlaySwapOwnCards(
                                    state.cardIndex,
                                    state.player1Row,
                                    state.player1Col,
                                    row,
                                    col,
                                )
                                onSwapStateChange(null)
                            }
                        } else if (!state.ownCardsOnly && myPlayerId != state.player1Id) {
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
        onOtherSlotSelected = { playerId, row, col ->
            when (val state = swapState) {
                is SwapSelectionState.AwaitingFirst -> {
                    if (!state.ownCardsOnly) {
                        onSwapStateChange(
                            SwapSelectionState.AwaitingSecond(
                                cardIndex = state.cardIndex,
                                player1Id = playerId,
                                player1Row = row,
                                player1Col = col,
                                ownCardsOnly = false,
                            ),
                        )
                    }
                }
                is SwapSelectionState.AwaitingSecond -> {
                    if (!state.ownCardsOnly && playerId != state.player1Id) {
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
        peekResult = enlightenmentResult,
        onDismissPeek = onDismissEnlightenmentResult,
    )

    HandActionCardsSection(
        cards = myActionCards,
        canUseCards = isMyTurn && isDrawPhase && swapState == null && pendingEnlightenmentCardIndex == null,
        activeCardIndex = activeActionCardIndex,
        onPlayActionCard = { index ->
            when (myActionCards.getOrNull(index)?.kind) {
                ACTION_CARD_KIND_PLAYER_SWAP -> {
                    onPendingActionChange(null)
                    onPendingEnlightenmentCardIndexChange(null)
                    onSwapStateChange(SwapSelectionState.AwaitingFirst(index))
                }
                ACTION_CARD_KIND_SWAP_OWN_CARDS -> {
                    onPendingActionChange(null)
                    onPendingEnlightenmentCardIndexChange(null)
                    onSwapStateChange(SwapSelectionState.AwaitingFirst(index, ownCardsOnly = true))
                }
                ACTION_CARD_KIND_ENLIGHTENMENT -> {
                    onPendingActionChange(null)
                    onSwapStateChange(null)
                    onPendingEnlightenmentCardIndexChange(index)
                }
                else -> {
                    onPendingActionChange(null)
                    onSwapStateChange(null)
                    onPendingEnlightenmentCardIndexChange(null)
                    onPlayActionCard(index)
                }
            }
        },
        onDiscardActionCard = { index ->
            onDiscardActionCard(index)
            onSwapStateChange(null)
            onPendingEnlightenmentCardIndexChange(null)
        },
    )

    if (isMyTurn && isDrawPhase && pendingEnlightenmentCardIndex != null && myPlayerId != null) {
        EnlightenmentTargetPicker(
            players = gameState.players,
            myPlayerId = myPlayerId,
            onTargetSelected = { targetPlayerId, targetType, lineIndex ->
                onPlayEnlightenmentCard(
                    PlayActionCardCommand(
                        actionCardIndex = pendingEnlightenmentCardIndex,
                        parameters = ActionCardParameters.BoardLineTarget(
                            targetPlayerId = targetPlayerId,
                            targetType = targetType,
                            lineIndex = lineIndex,
                        ),
                    ),
                )
                onPendingEnlightenmentCardIndexChange(null)
            },
            onCancel = { onPendingEnlightenmentCardIndexChange(null) },
        )
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
private fun ReportCheatSection(
    reportsAvailable: Int,
    enabled: Boolean,
    onReportCheat: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Suspect cheating?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedText,
                )
                Text(
                    text = "$reportsAvailable reports left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                )
            }
            TextButton(
                onClick = onReportCheat,
                enabled = enabled,
                modifier = Modifier.testTag("report_cheat_button"),
            ) {
                Text(
                    text = "Report Cheat ($reportsAvailable left)",
                    color = if (enabled) PrimaryGreen else MutedText,
                )
            }
        }
    }
}

@Composable
private fun PlayerGridCarousel(
    players: List<GamePlayerState>,
    myPlayerId: String,
    myBoard: List<List<BoardSlot>>,
    myVisibleScore: Int,
    totalScores: List<TotalScore>,
    disconnectedPlayers: List<String>,
    isMyTurn: Boolean,
    isReplacementPhase: Boolean,
    pendingAction: String?,
    swapState: SwapSelectionState?,
    viewedPlayerIndex: Int,
    onViewedPlayerIndexChange: (Int) -> Unit,
    onPendingActionChange: (String?) -> Unit,
    onReplaceCard: (row: Int, col: Int) -> Unit,
    onDiscardAndReveal: (row: Int, col: Int) -> Unit,
    onSwapSlotSelected: (row: Int, col: Int) -> Unit,
    onOtherSlotSelected: (playerId: String, row: Int, col: Int) -> Unit,
    peekResult: ActionCardResultMessage? = null,
    onDismissPeek: () -> Unit = {},
) {
    if (players.isEmpty()) return
    val pageCount = players.size
    val safeIndex = viewedPlayerIndex.coerceIn(0, players.lastIndex)
    val viewedPlayer = players[safeIndex]
    val isOwnGrid = viewedPlayer.playerId == myPlayerId

    LaunchedEffect(peekResult) {
        if (peekResult != null) {
            val targetIndex = players.indexOfFirst { it.playerId == peekResult.targetPlayerId }
            if (targetIndex >= 0) onViewedPlayerIndexChange(targetIndex)
        }
    }

    val peekedSlots: Map<Pair<Int, Int>, Int?> =
        if (peekResult != null && viewedPlayer.playerId == peekResult.targetPlayerId) {
            peekResult.toPeekedSlots()
        } else {
            emptyMap()
        }

    val canSelectOwnForSwap = when (val state = swapState) {
        is SwapSelectionState.AwaitingFirst -> myPlayerId.isNotBlank()
        is SwapSelectionState.AwaitingSecond -> myPlayerId.isNotBlank() &&
                (state.ownCardsOnly || state.player1Id != myPlayerId)
        null -> false
    }

    val viewedPlayerHasDefense = !isOwnGrid && viewedPlayer.actionCards.any { it.kind == "DEFENSE" }
    val isProtectedFromSwap = swapState != null && !isOwnGrid && viewedPlayerHasDefense

    val isSelectableOtherGrid = when (val state = swapState) {
        is SwapSelectionState.AwaitingFirst -> !state.ownCardsOnly && !isProtectedFromSwap
        is SwapSelectionState.AwaitingSecond -> !state.ownCardsOnly &&
                state.player1Id != viewedPlayer.playerId && !isProtectedFromSwap
        null -> false
    }

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
                IconButton(
                    onClick = { onViewedPlayerIndexChange((safeIndex - 1 + pageCount) % pageCount) },
                    modifier = Modifier.testTag("carousel_prev"),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous player",
                        tint = PrimaryGreen,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isOwnGrid) "Your Grid" else "${viewedPlayer.nickname}'s Grid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "(${safeIndex + 1}/$pageCount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                    )
                }

                if (isOwnGrid) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = at.aau.se2.skyjo.ui.theme.GreenSurface,
                    ) {
                        Text(
                            text = "Visible: $myVisibleScore",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                IconButton(
                    onClick = { onViewedPlayerIndexChange((safeIndex + 1) % pageCount) },
                    modifier = Modifier.testTag("carousel_next"),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next player",
                        tint = PrimaryGreen,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (peekedSlots.isNotEmpty()) {
                val peekTargetLabel = peekResult?.let { r ->
                    when (r.targetType) {
                        BoardLineTargetType.ROW -> "Row ${r.lineIndex}"
                        BoardLineTargetType.COLUMN -> "Column ${r.lineIndex}"
                    }
                } ?: ""
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BlueSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("peek_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Private Peek",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = peekTargetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText,
                            )
                        }
                        TextButton(onClick = onDismissPeek) {
                            Text(text = "Got it", color = PrimaryGreen)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isOwnGrid) {
                CardGrid(
                    board = myBoard,
                    peekedSlots = peekedSlots,
                    selectable = canSelectOwnForSwap || (isMyTurn && isReplacementPhase && pendingAction != null),
                    onCardClick = { row, col ->
                        if (canSelectOwnForSwap) {
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
            } else {
                val isDisconnected = viewedPlayer.nickname in disconnectedPlayers
                val playerScore = totalScores.find { it.playerId == viewedPlayer.playerId }?.totalScore ?: 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isDisconnected) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color(0xFFFFCDD2),
                        ) {
                            Text(
                                text = "Disconnected",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB71C1C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$playerScore pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (isProtectedFromSwap) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("defense_protected_indicator"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Protected by Defense card — cannot swap",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else if (isSelectableOtherGrid) {
                    Text(
                        text = "Tap a card to swap",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                CardGrid(
                    board = viewedPlayer.board,
                    peekedSlots = peekedSlots,
                    selectable = isSelectableOtherGrid,
                    onCardClick = { row, col ->
                        onOtherSlotSelected(viewedPlayer.playerId, row, col)
                    },
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
    isPenaltyFlashing: Boolean,
    modifier: Modifier = Modifier,
) {
    val surfaceColor = when {
        isPenaltyFlashing -> Color(0xFFD32F2F)
        isActive -> PrimaryGreen
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isPenaltyFlashing -> SurfaceWhite
        isActive -> SurfaceWhite
        else -> MutedText
    }
    val scoreColor = when {
        isPenaltyFlashing -> SurfaceWhite
        isActive -> MintGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
    val animatedSurfaceColor by animateColorAsState(
        targetValue = surfaceColor,
        animationSpec = tween(durationMillis = 250),
        label = "penaltyScoreSurfaceColor",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = textColor,
        animationSpec = tween(durationMillis = 250),
        label = "penaltyScoreTextColor",
    )
    val animatedScoreColor by animateColorAsState(
        targetValue = scoreColor,
        animationSpec = tween(durationMillis = 250),
        label = "penaltyScoreValueColor",
    )
    val pillModifier = if (isPenaltyFlashing) {
        modifier.testTag("score_penalty_flash_${score.playerId}")
    } else {
        modifier
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = animatedSurfaceColor,
        border = if (!isActive && !isPenaltyFlashing) androidx.compose.foundation.BorderStroke(1.dp, BorderColor) else null,
        modifier = pillModifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = score.nickname,
                style = MaterialTheme.typography.labelSmall,
                color = animatedTextColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${score.totalScore} pts",
                style = MaterialTheme.typography.labelMedium,
                color = animatedScoreColor,
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
    peekedSlots: Map<Pair<Int, Int>, Int?> = emptyMap(),
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
                    val key = Pair(rowIndex, colIndex)
                    BoardSlotTile(
                        slot = slot,
                        selectable = selectable && slot.type == "OCCUPIED",
                        onClick = { onCardClick(rowIndex, colIndex) },
                        isPeeked = peekedSlots.containsKey(key),
                        peekedValue = peekedSlots[key],
                        modifier = Modifier
                            .weight(1f)
                            .testTag("board_slot_${rowIndex}_${colIndex}"),
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
    isPeeked: Boolean = false,
    peekedValue: Int? = null,
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
                isPeeked -> BlueSurface
                !isFaceUp -> CardHiddenBg
                cardValue != null && cardValue <= 0 -> CardNegativeBg
                else -> CardPositiveBg
            }
            val displayText = when {
                isPeeked -> peekedValue?.toString() ?: "?"
                !isFaceUp -> "?"
                slot.card?.type == "ACTION" -> "A"
                cardValue != null -> cardValue.toString()
                else -> "?"
            }
            val borderColor = when {
                selectable -> PrimaryGreen
                isPeeked -> MintGreen
                !isFaceUp -> MintGreen.copy(alpha = 0.4f)
                else -> BorderColor
            }
            val textColor = when {
                isPeeked -> PrimaryGreen
                !isFaceUp -> CardHiddenText
                else -> MaterialTheme.colorScheme.onBackground
            }

            Box(
                modifier = modifier
                    .aspectRatio(0.65f)
                    .background(color = bgColor, shape = RoundedCornerShape(10.dp))
                    .border(
                        width = if (selectable || isPeeked) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .then(if (selectable) Modifier.clickable(onClick = onClick) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
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
                    color = if (score.finalScore <= 0) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
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
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ActionCardFaceContent(card = action)
                                Text(
                                    text = actionCardAccessibilityLabel(action),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
                                )
                            }
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
    activeCardIndex: Int?,
    onPlayActionCard: (actionCardIndex: Int) -> Unit,
    onDiscardActionCard: (actionCardIndex: Int) -> Unit,
) {
    SectionCard(title = "Your Action Cards", badge = "${cards.size} cards") {
        if (cards.isEmpty()) {
            EmptyActionCardHandMessage()
        } else {
            HandActionCardsRow(
                cards = cards,
                canUseCards = canUseCards,
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
private fun EnlightenmentTargetPicker(
    players: List<GamePlayerState>,
    myPlayerId: String,
    onTargetSelected: (targetPlayerId: String, targetType: BoardLineTargetType, lineIndex: Int) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedTargetPlayerId by remember { mutableStateOf(myPlayerId) }
    val selectedPlayer = players.firstOrNull { it.playerId == selectedTargetPlayerId }
        ?: players.firstOrNull()
    val board = selectedPlayer?.board ?: emptyList()
    val columnCount = board.maxOfOrNull { it.size } ?: 0

    SectionCard(title = "Enlightenment") {
        Text(
            text = "Select a target player, then a row or column",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Target Player",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryGreen,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            players.forEach { player ->
                SecondaryButton(
                    text = if (player.playerId == myPlayerId) {
                        "${player.nickname} (You)"
                    } else {
                        "Target ${player.nickname}"
                    },
                    onClick = { selectedTargetPlayerId = player.playerId },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (selectedPlayer != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target: ${selectedPlayer.nickname}",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Rows",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryGreen,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            board.indices.forEach { rowIndex ->
                SecondaryButton(
                    text = "Row $rowIndex",
                    onClick = {
                        selectedPlayer?.let {
                            onTargetSelected(it.playerId, BoardLineTargetType.ROW, rowIndex)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Columns",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryGreen,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(columnCount) { colIndex ->
                SecondaryButton(
                    text = "Column $colIndex",
                    onClick = {
                        selectedPlayer?.let {
                            onTargetSelected(it.playerId, BoardLineTargetType.COLUMN, colIndex)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onCancel) {
            Text(text = "Cancel")
        }
    }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ActionCardFaceContent(
                    card = card,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = actionCardAccessibilityLabel(card),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
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
        Modifier.clickable { onPlayActionCard(index) }
    } else {
        Modifier
    }

    return Modifier
        .aspectRatio(0.65f)
        .testTag("play_action_card_$index")
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

private fun cardColor(value: Int): Color = when {
    value <= 0 -> CardNegativeBg
    else -> CardPositiveBg
}

private fun cardDisplayValue(card: Card): String =
    if (card.type == "ACTION") "A" else card.value.toString()

private fun actionCardIcon(card: ActionCard): ImageVector =
    when (card.kind) {
        "DEFENSE" -> Icons.Default.Shield
        ACTION_CARD_KIND_PLAYER_SWAP -> Icons.Default.SwapHoriz
        ACTION_CARD_KIND_DOUBLE_TURN -> Icons.Default.FastForward
        ACTION_CARD_KIND_SWAP_OWN_CARDS -> Icons.Default.Autorenew
        ACTION_CARD_KIND_ENLIGHTENMENT -> Icons.Default.Lightbulb
        else -> Icons.Default.Style
    }

private fun actionCardAccessibilityLabel(card: ActionCard): String =
    when (card.kind) {
        "DEFENSE" -> "Defense"
        ACTION_CARD_KIND_PLAYER_SWAP -> "Player swap"
        ACTION_CARD_KIND_DOUBLE_TURN -> "Double turn"
        ACTION_CARD_KIND_SWAP_OWN_CARDS -> "Swap own cards"
        ACTION_CARD_KIND_ENLIGHTENMENT -> "Enlightenment"
        else -> card.label.ifBlank { "Action" }
    }

private fun ActionCardResultMessage.toPeekedSlots(): Map<Pair<Int, Int>, Int?> {
    if (inspectedCards.isNotEmpty()) {
        return inspectedCards.associate { Pair(it.row, it.col) to it.value }
    }
    return when (targetType) {
        BoardLineTargetType.ROW ->
            inspectedValues.mapIndexed { col, value -> Pair(lineIndex, col) to value }.toMap()
        BoardLineTargetType.COLUMN ->
            inspectedValues.mapIndexed { row, value -> Pair(row, lineIndex) to value }.toMap()
    }
}

@Composable
private fun ActionCardFaceContent(
    card: ActionCard,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = actionCardIcon(card),
        contentDescription = null,
        tint = PrimaryGreen,
        modifier = modifier.size(32.dp),
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
            onNavigateToGameOver = {},
        )
    }
}
