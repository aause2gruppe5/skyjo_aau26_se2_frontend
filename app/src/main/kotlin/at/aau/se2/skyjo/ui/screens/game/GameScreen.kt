package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

internal data class SkyjoCardState(val value: Int, val isFaceUp: Boolean)

internal enum class ErleuchtungSelectionType {
    ROW,
    COLUMN,
}

internal data class ErleuchtungSelection(
    val playerName: String,
    val type: ErleuchtungSelectionType,
    val index: Int,
)

private data class ErleuchtungReveal(
    val selection: ErleuchtungSelection,
    val values: List<Int>,
)

private data class GamePlayer(
    val name: String,
    val score: Int,
    val isActive: Boolean = false,
    val grid: List<List<SkyjoCardState>>,
)

internal fun erleuchtungPeekValues(
    grid: List<List<SkyjoCardState>>,
    selectionType: ErleuchtungSelectionType,
    index: Int,
): List<Int> {
    val selectedCards = when (selectionType) {
        ErleuchtungSelectionType.ROW -> grid.getOrNull(index).orEmpty()
        ErleuchtungSelectionType.COLUMN -> grid.mapNotNull { row -> row.getOrNull(index) }
    }

    return selectedCards
        .filter { card -> !card.isFaceUp }
        .map { card -> card.value }
}

private val dummyPlayers = listOf(
    GamePlayer(
        name = "Alice",
        score = 12,
        isActive = true,
        grid = listOf(
            listOf(
                SkyjoCardState(9, isFaceUp = false),
                SkyjoCardState(3, isFaceUp = true),
                SkyjoCardState(-1, isFaceUp = false),
                SkyjoCardState(8, isFaceUp = true),
            ),
            listOf(
                SkyjoCardState(7, isFaceUp = true),
                SkyjoCardState(12, isFaceUp = false),
                SkyjoCardState(11, isFaceUp = true),
                SkyjoCardState(4, isFaceUp = false),
            ),
            listOf(
                SkyjoCardState(6, isFaceUp = false),
                SkyjoCardState(5, isFaceUp = true),
                SkyjoCardState(0, isFaceUp = false),
                SkyjoCardState(2, isFaceUp = true),
            ),
        ),
    ),
    GamePlayer(
        name = "Bob",
        score = 8,
        grid = listOf(
            listOf(
                SkyjoCardState(1, isFaceUp = true),
                SkyjoCardState(10, isFaceUp = false),
                SkyjoCardState(-2, isFaceUp = false),
                SkyjoCardState(5, isFaceUp = true),
            ),
            listOf(
                SkyjoCardState(6, isFaceUp = false),
                SkyjoCardState(2, isFaceUp = true),
                SkyjoCardState(8, isFaceUp = false),
                SkyjoCardState(0, isFaceUp = true),
            ),
            listOf(
                SkyjoCardState(11, isFaceUp = true),
                SkyjoCardState(7, isFaceUp = false),
                SkyjoCardState(4, isFaceUp = true),
                SkyjoCardState(3, isFaceUp = false),
            ),
        ),
    ),
    GamePlayer(
        name = "Charlie",
        score = 15,
        grid = listOf(
            listOf(
                SkyjoCardState(12, isFaceUp = false),
                SkyjoCardState(4, isFaceUp = true),
                SkyjoCardState(1, isFaceUp = false),
                SkyjoCardState(7, isFaceUp = true),
            ),
            listOf(
                SkyjoCardState(3, isFaceUp = true),
                SkyjoCardState(9, isFaceUp = false),
                SkyjoCardState(6, isFaceUp = true),
                SkyjoCardState(-1, isFaceUp = false),
            ),
            listOf(
                SkyjoCardState(2, isFaceUp = false),
                SkyjoCardState(5, isFaceUp = true),
                SkyjoCardState(10, isFaceUp = false),
                SkyjoCardState(8, isFaceUp = true),
            ),
        ),
    ),
)

// ── Screen ──────────────────────────────────────────────────────────────────

private val actionCards = listOf("Peek", "Trade", "Double", "Erleuchtung")

@Composable
fun GameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePlayer = dummyPlayers.firstOrNull { player -> player.isActive } ?: dummyPlayers.first()
    var isErleuchtungSelecting by remember { mutableStateOf(false) }
    var selectedErleuchtungSelection by remember { mutableStateOf<ErleuchtungSelection?>(null) }
    var erleuchtungReveal by remember { mutableStateOf<ErleuchtungReveal?>(null) }

    fun closeErleuchtungReveal() {
        erleuchtungReveal = null
        selectedErleuchtungSelection = null
        isErleuchtungSelecting = false
    }

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
                    StatChip(label = "Turn", value = activePlayer.name)
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
            ActionMarketSection(
                isErleuchtungSelecting = isErleuchtungSelecting,
                onErleuchtungClick = {
                    isErleuchtungSelecting = true
                    erleuchtungReveal = null
                    selectedErleuchtungSelection = null
                },
            )

            // ── Deck + Discard ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeckCard(label = "Draw Deck", modifier = Modifier.weight(1f))
                DiscardCard(value = "4", label = "Discard Pile", modifier = Modifier.weight(1f))
            }

            // ── Player Grid ───────────────────────────────────────────────
            dummyPlayers.forEach { player ->
                PlayerGridSection(
                    player = player,
                    isOwnGrid = player.name == activePlayer.name,
                    isErleuchtungSelecting = isErleuchtungSelecting,
                    selectedSelection = selectedErleuchtungSelection,
                    onErleuchtungSelection = { selection ->
                        selectedErleuchtungSelection = selection
                        erleuchtungReveal = ErleuchtungReveal(
                            selection = selection,
                            values = erleuchtungPeekValues(
                                grid = player.grid,
                                selectionType = selection.type,
                                index = selection.index,
                            ),
                        )
                    },
                )
            }

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

    erleuchtungReveal?.let { reveal ->
        ErleuchtungRevealDialog(
            reveal = reveal,
            onDismiss = { closeErleuchtungReveal() },
        )
    }
}

// ── Subcomponents ────────────────────────────────────────────────────────────

@Composable
private fun PlayerPill(player: GamePlayer, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (player.isActive) PrimaryGreen else MaterialTheme.colorScheme.surface,
        border = if (!player.isActive)
            BorderStroke(1.dp, BorderColor)
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
private fun ActionMarketSection(
    isErleuchtungSelecting: Boolean,
    onErleuchtungClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ActionMarketHeader()
            Spacer(modifier = Modifier.height(14.dp))
            ActionDeckPreview()
            Spacer(modifier = Modifier.height(14.dp))
            ActionCardChips(
                isErleuchtungSelecting = isErleuchtungSelecting,
                onErleuchtungClick = onErleuchtungClick,
            )
            if (isErleuchtungSelecting) {
                ErleuchtungSelectionHint()
            }
        }
    }
}

@Composable
private fun ActionMarketHeader() {
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
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = GreenSurface,
        ) {
            Text(
                text = "${actionCards.size} cards",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ActionDeckPreview() {
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
}

@Composable
private fun ActionCardChips(
    isErleuchtungSelecting: Boolean,
    onErleuchtungClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actionCards.forEach { action ->
            ActionCardChip(
                action = action,
                isErleuchtungSelecting = isErleuchtungSelecting,
                onErleuchtungClick = onErleuchtungClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionCardChip(
    action: String,
    isErleuchtungSelecting: Boolean,
    onErleuchtungClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isErleuchtung = action == "Erleuchtung"
    val isSelected = isErleuchtung && isErleuchtungSelecting
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) PrimaryGreen else GreenSurface,
        border = if (isErleuchtung && !isSelected) {
            BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.45f))
        } else {
            null
        },
        modifier = modifier.clickable(enabled = isErleuchtung) { onErleuchtungClick() },
    ) {
        Text(
            text = action,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) SurfaceWhite else PrimaryGreen,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun ErleuchtungSelectionHint() {
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = BlueSurface,
    ) {
        Text(
            text = "Select any row or column to peek at face-down cards",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PlayerGridSection(
    player: GamePlayer,
    isOwnGrid: Boolean,
    isErleuchtungSelecting: Boolean,
    selectedSelection: ErleuchtungSelection?,
    onErleuchtungSelection: (ErleuchtungSelection) -> Unit,
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
                    text = if (isOwnGrid) "Your Grid" else "${player.name}'s Grid",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = GreenSurface,
                ) {
                    Text(
                        text = "Score: ${player.score}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            CardGrid(
                playerName = player.name,
                cards = player.grid,
                selectedSelection = selectedSelection,
            )
            if (isErleuchtungSelecting) {
                Spacer(modifier = Modifier.height(12.dp))
                ErleuchtungAxisSelector(
                    player = player,
                    selectedSelection = selectedSelection,
                    onSelection = onErleuchtungSelection,
                )
            }
        }
    }
}

@Composable
private fun ErleuchtungAxisSelector(
    player: GamePlayer,
    selectedSelection: ErleuchtungSelection?,
    onSelection: (ErleuchtungSelection) -> Unit,
) {
    val columnCount = player.grid.maxOfOrNull { row -> row.size } ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AxisSelectorRow(
            label = "Rows",
            itemCount = player.grid.size,
            itemPrefix = "R",
            playerName = player.name,
            selectionType = ErleuchtungSelectionType.ROW,
            selectedSelection = selectedSelection,
            onSelection = onSelection,
        )
        AxisSelectorRow(
            label = "Columns",
            itemCount = columnCount,
            itemPrefix = "C",
            playerName = player.name,
            selectionType = ErleuchtungSelectionType.COLUMN,
            selectedSelection = selectedSelection,
            onSelection = onSelection,
        )
    }
}

@Composable
private fun AxisSelectorRow(
    label: String,
    itemCount: Int,
    itemPrefix: String,
    playerName: String,
    selectionType: ErleuchtungSelectionType,
    selectedSelection: ErleuchtungSelection?,
    onSelection: (ErleuchtungSelection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            modifier = Modifier.width(58.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(itemCount) { index ->
                val selection = ErleuchtungSelection(
                    playerName = playerName,
                    type = selectionType,
                    index = index,
                )
                AxisSelectorButton(
                    label = "$itemPrefix${index + 1}",
                    contentDescription = "Select $playerName ${selectionType.axisName()} ${index + 1}",
                    selected = selectedSelection == selection,
                    onClick = { onSelection(selection) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun ErleuchtungSelectionType.axisName(): String = when (this) {
    ErleuchtungSelectionType.ROW -> "row"
    ErleuchtungSelectionType.COLUMN -> "column"
}

@Composable
private fun AxisSelectorButton(
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) PrimaryGreen else GreenSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) PrimaryGreen else PrimaryGreen.copy(alpha = 0.35f),
        ),
        modifier = modifier
            .clickable { onClick() }
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) SurfaceWhite else PrimaryGreen,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
        )
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
private fun CardGrid(
    playerName: String,
    cards: List<List<SkyjoCardState>>,
    selectedSelection: ErleuchtungSelection?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { columnIndex, card ->
                    val highlighted = selectedSelection?.let { selection ->
                        selection.playerName == playerName &&
                            when (selection.type) {
                                ErleuchtungSelectionType.ROW -> selection.index == rowIndex
                                ErleuchtungSelectionType.COLUMN -> selection.index == columnIndex
                            }
                    } ?: false
                    GameCardTile(
                        value = if (card.isFaceUp) card.value.toString() else null,
                        modifier = Modifier.weight(1f),
                        highlighted = highlighted,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCardTile(
    value: String?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
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
                width = if (highlighted) 2.dp else 1.dp,
                color = when {
                    highlighted -> PrimaryGreen
                    isHidden -> MintGreen.copy(alpha = 0.4f)
                    else -> BorderColor
                },
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

@Composable
private fun ErleuchtungRevealDialog(
    reveal: ErleuchtungReveal,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Column {
                Text(
                    text = "Erleuchtung",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = reveal.selection.displayText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (reveal.values.isEmpty()) {
                    Text(
                        text = "No face-down cards in this selection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        reveal.values.forEach { value ->
                            PeekValueCard(
                                value = value,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = GreenSurface,
                ) {
                    Text(
                        text = "Private peek only. These cards stay face-down.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}

@Composable
private fun PeekValueCard(value: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .background(
                color = if (value <= 0) CardNegativeBg else CardPositiveBg,
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = BorderColor,
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

private fun ErleuchtungSelection.displayText(): String {
    val axis = when (type) {
        ErleuchtungSelectionType.ROW -> "Row"
        ErleuchtungSelectionType.COLUMN -> "Column"
    }

    return "$playerName $axis ${index + 1}"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun GameScreenPreview() {
    SkyjoTheme {
        GameScreen(onBack = {})
    }
}
