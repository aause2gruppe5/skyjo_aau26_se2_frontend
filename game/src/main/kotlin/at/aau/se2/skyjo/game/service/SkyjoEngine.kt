package at.aau.se2.skyjo.game.service

import at.aau.se2.skyjo.game.error.GameNotStartedException
import at.aau.se2.skyjo.game.error.InvalidGameSetupException
import at.aau.se2.skyjo.game.error.InvalidMoveException
import at.aau.se2.skyjo.game.error.RoundAlreadyFinishedException
import at.aau.se2.skyjo.game.model.BoardLayout
import at.aau.se2.skyjo.game.model.BoardPosition
import at.aau.se2.skyjo.game.model.BoardSlot
import at.aau.se2.skyjo.game.model.DiscardPile
import at.aau.se2.skyjo.game.model.DrawPile
import at.aau.se2.skyjo.game.model.DrawSource
import at.aau.se2.skyjo.game.model.GamePhase
import at.aau.se2.skyjo.game.model.GameState
import at.aau.se2.skyjo.game.model.PlayerBoard
import at.aau.se2.skyjo.game.model.PlayerState
import at.aau.se2.skyjo.game.model.RoundResult
import at.aau.se2.skyjo.game.model.SkyjoDeckFactory
import kotlin.random.Random

class SkyjoEngine {

    fun startGame(
        playerIds: List<String>,
        initialReveals: Map<String, Set<BoardPosition>>,
        seed: Long? = null,
    ): GameState {
        validateSetup(playerIds, initialReveals)

        var drawPile = SkyjoDeckFactory.createShuffledDrawPile(seed)
        val players = playerIds.map { playerId ->
            val cards = buildList {
                repeat(BoardLayout.POSITIONS.size) {
                    val drawResult = drawPile.draw()
                    add(drawResult.card)
                    drawPile = drawResult.remainingPile
                }
            }

            PlayerState(
                id = playerId,
                board = PlayerBoard.fromCards(
                    cards = cards,
                    revealedPositions = initialReveals.getValue(playerId),
                ),
            )
        }

        val openingDiscard = drawPile.draw()
        val startingPlayerIndex = determineStartingPlayerIndex(players, initialReveals)

        return GameState(
            players = players,
            currentPlayerIndex = startingPlayerIndex,
            drawPile = openingDiscard.remainingPile,
            discardPile = DiscardPile(listOf(openingDiscard.card)),
            phase = GamePhase.AWAITING_DRAW,
            shuffleSeed = seed,
        )
    }

    fun drawFromDeck(state: GameState): GameState {
        val playableState = requireActiveRound(state)
        if (playableState.phase != GamePhase.AWAITING_DRAW && playableState.phase != GamePhase.FINAL_TURNS) {
            throw InvalidMoveException("cannot draw from deck while phase is ${playableState.phase}")
        }

        val replenishedState = replenishDrawPileIfNeeded(playableState)
        val drawResult = replenishedState.drawPile.draw()
        return replenishedState.copy(
            drawPile = drawResult.remainingPile,
            drawnCard = drawResult.card,
            drawSource = DrawSource.DECK,
            phase = GamePhase.AWAITING_REPLACEMENT,
        )
    }

    fun takeDiscardCard(state: GameState): GameState {
        val playableState = requireActiveRound(state)
        if (playableState.phase != GamePhase.AWAITING_DRAW && playableState.phase != GamePhase.FINAL_TURNS) {
            throw InvalidMoveException("cannot take discard card while phase is ${playableState.phase}")
        }

        val drawResult = playableState.discardPile.takeTop()
        return playableState.copy(
            discardPile = drawResult.remainingPile,
            drawnCard = drawResult.card,
            drawSource = DrawSource.DISCARD,
            phase = GamePhase.AWAITING_REPLACEMENT,
        )
    }

    fun replaceDrawnCard(state: GameState, position: BoardPosition): GameState {
        val playableState = requireAwaitingReplacement(state)
        val currentPlayer = playableState.currentPlayer()
        val drawnCard = playableState.drawnCard ?: throw InvalidMoveException("no drawn card is available")
        val replacementResult = currentPlayer.board.replace(position, drawnCard)
        val cleanupResult = replacementResult.board.clearCompletedLines()

        val updatedDiscardPile = playableState.discardPile
            .add(replacementResult.replacedCard)
            .addAll(cleanupResult.removedCards)

        val updatedPlayers = playableState.players.updated(
            playableState.currentPlayerIndex,
            currentPlayer.copy(board = cleanupResult.board),
        )

        return advanceAfterTurn(
            state = playableState.copy(
                players = updatedPlayers,
                discardPile = updatedDiscardPile,
                drawnCard = null,
                drawSource = null,
            ),
        )
    }

    fun discardDrawnCardAndReveal(state: GameState, position: BoardPosition): GameState {
        val playableState = requireAwaitingReplacement(state)
        if (playableState.drawSource != DrawSource.DECK) {
            throw InvalidMoveException("discard and reveal is only allowed after drawing from the deck")
        }

        val currentPlayer = playableState.currentPlayer()
        val slot = currentPlayer.board.slotAt(position)
        if (slot !is BoardSlot.Occupied || slot.faceUp) {
            throw InvalidMoveException("discard and reveal requires a face-down occupied slot")
        }

        val revealedBoard = currentPlayer.board.reveal(position)
        val cleanupResult = revealedBoard.clearCompletedLines()
        val drawnCard = playableState.drawnCard ?: throw InvalidMoveException("no drawn card is available")

        val updatedPlayers = playableState.players.updated(
            playableState.currentPlayerIndex,
            currentPlayer.copy(board = cleanupResult.board),
        )
        val updatedDiscardPile = playableState.discardPile
            .add(drawnCard)
            .addAll(cleanupResult.removedCards)

        return advanceAfterTurn(
            state = playableState.copy(
                players = updatedPlayers,
                discardPile = updatedDiscardPile,
                drawnCard = null,
                drawSource = null,
            ),
        )
    }

    private fun validateSetup(
        playerIds: List<String>,
        initialReveals: Map<String, Set<BoardPosition>>,
    ) {
        if (playerIds.size !in 2..8) {
            throw InvalidGameSetupException("Skyjo requires between 2 and 8 players")
        }
        if (playerIds.distinct().size != playerIds.size) {
            throw InvalidGameSetupException("player ids must be unique")
        }
        if (playerIds.any { it.isBlank() }) {
            throw InvalidGameSetupException("player ids must not be blank")
        }
        if (initialReveals.keys != playerIds.toSet()) {
            throw InvalidGameSetupException("initial reveals must be provided for every player id")
        }
        if (initialReveals.values.any { it.size != 2 }) {
            throw InvalidGameSetupException("each player must reveal exactly two positions")
        }
    }

    private fun requireActiveRound(state: GameState): GameState {
        when (state.phase) {
            GamePhase.NOT_STARTED -> throw GameNotStartedException("game has not been started yet")
            GamePhase.ROUND_FINISHED -> throw RoundAlreadyFinishedException("round has already finished")
            else -> return state
        }
    }

    private fun requireAwaitingReplacement(state: GameState): GameState {
        val playableState = requireActiveRound(state)
        if (playableState.phase != GamePhase.AWAITING_REPLACEMENT) {
            throw InvalidMoveException("a card has to be drawn before this action")
        }
        return playableState
    }

    private fun replenishDrawPileIfNeeded(state: GameState): GameState {
        if (state.drawPile.size > 0) {
            return state
        }

        if (state.discardPile.size <= 1) {
            throw InvalidMoveException("cannot replenish draw pile because the discard pile has no spare cards")
        }

        val protectedTopCard = state.discardPile.topCard()
        val cardsToShuffle = state.discardPile.cards.dropLast(1)
        val seed = state.shuffleSeed
        val random = if (seed != null) {
            Random(seed + state.shuffleCount.toLong() + 1L)
        } else {
            Random
        }
        val shuffledCards = cardsToShuffle.shuffled(random)

        return state.copy(
            drawPile = DrawPile(shuffledCards),
            discardPile = DiscardPile(listOf(protectedTopCard)),
            shuffleCount = state.shuffleCount + 1,
        )
    }

    private fun advanceAfterTurn(state: GameState): GameState {
        val currentPlayer = state.currentPlayer()
        val finisherTriggered = !currentPlayer.board.hasHiddenCards()

        if (state.finisherPlayerId == null && finisherTriggered) {
            val finalTurns = state.players.size - 1
            val nextPlayerIndex = nextPlayerIndex(state.currentPlayerIndex, state.players.size)
            if (finalTurns <= 0) {
                return finishRound(state.copy(finisherPlayerId = currentPlayer.id, finalTurnsRemaining = 0))
            }

            return state.copy(
                currentPlayerIndex = nextPlayerIndex,
                phase = GamePhase.FINAL_TURNS,
                finisherPlayerId = currentPlayer.id,
                finalTurnsRemaining = finalTurns,
            )
        }

        if (state.finisherPlayerId != null) {
            val remainingFinalTurns = state.finalTurnsRemaining - 1
            if (remainingFinalTurns <= 0) {
                return finishRound(state.copy(finalTurnsRemaining = 0))
            }

            return state.copy(
                currentPlayerIndex = nextPlayerIndex(state.currentPlayerIndex, state.players.size),
                phase = GamePhase.FINAL_TURNS,
                finalTurnsRemaining = remainingFinalTurns,
            )
        }

        return state.copy(
            currentPlayerIndex = nextPlayerIndex(state.currentPlayerIndex, state.players.size),
            phase = GamePhase.AWAITING_DRAW,
        )
    }

    internal fun finishRound(state: GameState): GameState {
        val finisherPlayerId = state.finisherPlayerId ?: throw InvalidMoveException("cannot finish round without a finisher")
        var updatedDiscardPile = state.discardPile
        val revealedPlayers = state.players.map { player ->
            val revealedBoard = player.board.fullyReveal()
            val cleanupResult = revealedBoard.clearCompletedLines()
            updatedDiscardPile = updatedDiscardPile.addAll(cleanupResult.removedCards)
            player.copy(board = cleanupResult.board)
        }

        val rawScores = revealedPlayers.associate { player -> player.id to player.board.rawScore() }
        val finisherScore = rawScores.getValue(finisherPlayerId)
        val mustDoubleFinisher = finisherScore > 0 && rawScores.any { (playerId, score) ->
            playerId != finisherPlayerId && score <= finisherScore
        }

        val roundResult = RoundResult(
            finisherPlayerId = finisherPlayerId,
            scores = revealedPlayers.map { player ->
                val rawScore = rawScores.getValue(player.id)
                RoundResult.PlayerRoundScore(
                    playerId = player.id,
                    rawScore = rawScore,
                    finalScore = when {
                        player.id == finisherPlayerId && mustDoubleFinisher -> rawScore * 2
                        else -> rawScore
                    },
                )
            },
        )

        return state.copy(
            players = revealedPlayers,
            discardPile = updatedDiscardPile,
            phase = GamePhase.ROUND_FINISHED,
            drawnCard = null,
            drawSource = null,
            finalTurnsRemaining = 0,
            roundResult = roundResult,
        )
    }

    internal fun determineStartingPlayerIndex(
        players: List<PlayerState>,
        initialReveals: Map<String, Set<BoardPosition>>,
    ): Int =
        players.indices.maxByOrNull { index ->
            players[index].board.visibleValueSum(initialReveals.getValue(players[index].id))
        } ?: 0

    private fun nextPlayerIndex(currentIndex: Int, playerCount: Int): Int = (currentIndex + 1) % playerCount
}

private fun <T> List<T>.updated(index: Int, value: T): List<T> = mapIndexed { currentIndex, item ->
    if (currentIndex == index) value else item
}
