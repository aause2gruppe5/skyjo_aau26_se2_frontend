package at.aau.se2.skyjo.game.service

import at.aau.se2.skyjo.game.error.*
import at.aau.se2.skyjo.game.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*
import org.assertj.core.api.Assertions.assertThat



class SkyjoEngineTest {
    private lateinit var engine: SkyjoEngine

    @BeforeEach
    fun setUp() {
        engine = SkyjoEngine()
    }

    @Nested
    inner class StartGameTest{
        @Test
        fun playerCountInvalidTooLow(){
            val playerIds = listOf("p1")
            val initialReveals = mapOf("p1" to setOf(mockPosition(),mockPosition()))
            val exception = assertThrows<InvalidGameSetupException>{engine.startGame(playerIds,initialReveals)}

            assertThat(exception).hasMessageContaining("between 2 and 8 players")
        }

        @Test
        fun playerCountInvalidTooHigh(){
            val playerIds = listOf("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9")
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(),mockPosition()),
                "p2" to setOf(mockPosition(),mockPosition()),
                "p3" to setOf(mockPosition(),mockPosition()),
                "p4" to setOf(mockPosition(),mockPosition()),
                "p5" to setOf(mockPosition(),mockPosition()),
                "p6" to setOf(mockPosition(),mockPosition()),
                "p7" to setOf(mockPosition(),mockPosition()),
                "p8" to setOf(mockPosition(),mockPosition()),
                "p9" to setOf(mockPosition(),mockPosition()))
            val exception = assertThrows<InvalidGameSetupException>{engine.startGame(playerIds,initialReveals)}

            assertThat(exception).hasMessageContaining("between 2 and 8 players")
        }

        @Test
        fun initialRevealsNotComplete(){
            val playerIds = listOf("p1", "p2")
            val initialReveals = mapOf("p1" to setOf(mockPosition(),mockPosition()))
            val exception = assertThrows<InvalidGameSetupException>{engine.startGame(playerIds,initialReveals)}

            assertThat(exception).hasMessageContaining("initial reveals must be provided for every player id")
        }

        @Test
        fun startGameWorksCorrectly(){
            val playerIds = listOf("p1", "p2")
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0,0),mockPosition(0,2)),
                "p2" to setOf(mockPosition(1,1),mockPosition(2,2)),
            )
            val seed = 42L
            val state = engine.startGame(playerIds,initialReveals,seed)

            assertThat(state.players).hasSize(2)
            assertThat(state.phase).isEqualTo(GamePhase.AWAITING_DRAW)
            assertThat(state.discardPile.size).isEqualTo(1)
            assertThat(state.shuffleSeed).isEqualTo(seed)
        }
    }
    @Nested
    inner class DrawFromDeckTest{
        @Test
        fun drawFromDeckChangesPhaseFromAwaitingDraw(){
            val initialState = mockGameState(phase = GamePhase.AWAITING_DRAW, drawPile = DrawPile(cards = listOf(mockCard(), mockCard(), mockCard(), mockCard())))
            val initialDrawPileSize = initialState.drawPile.size
            val newState = engine.drawFromDeck(initialState)

            assertThat(newState.phase).isEqualTo(GamePhase.AWAITING_REPLACEMENT)
            assertThat(newState.drawSource).isEqualTo(DrawSource.DECK)
            assertThat(newState.drawnCard).isNotNull
            assertThat(newState.drawPile.size).isEqualTo(initialDrawPileSize - 1)
        }

        @Test
        fun drawFromDeckChangesPhaseFromFinalTurns(){
            val initialState = mockGameState(phase = GamePhase.FINAL_TURNS, drawPile = DrawPile(cards = listOf(mockCard(), mockCard(), mockCard(), mockCard())))
            val initialDrawPileSize = initialState.drawPile.size
            val newState = engine.drawFromDeck(initialState)

            assertThat(newState.phase).isEqualTo(GamePhase.AWAITING_REPLACEMENT)
            assertThat(newState.drawSource).isEqualTo(DrawSource.DECK)
            assertThat(newState.drawnCard).isNotNull
            assertThat(newState.drawPile.size).isEqualTo(initialDrawPileSize - 1)
        }

        @Test
        fun drawFromDeckWrongPhase(){
            val initialState = mockGameState(phase = GamePhase.AWAITING_REPLACEMENT, drawPile = DrawPile(cards = listOf(mockCard(), mockCard(), mockCard(), mockCard())))
            val exception = assertThrows<InvalidMoveException>{engine.drawFromDeck(initialState)}

            assertThat(exception).hasMessageContaining("cannot draw from deck while phase is")

        }
    }

    @Nested
    inner class TakeDiscardTest{
        @Test
        fun cardOnTopIsTakenPhaseAwaitingDraw(){
            val topDiscardCard = mockCard(value = 5)
            val initialState = mockGameState(phase = GamePhase.AWAITING_DRAW, discardPile = DiscardPile(cards = listOf(mockCard(value = -1), topDiscardCard)))
            val newState = engine.takeDiscardCard(initialState)

            assertThat(newState.phase).isEqualTo(GamePhase.AWAITING_REPLACEMENT)
            assertThat(newState.drawSource).isEqualTo(DrawSource.DISCARD)
            assertThat(newState.drawnCard).isEqualTo(topDiscardCard)
            assertThat(newState.discardPile.size).isEqualTo(1)
        }

        @Test
        fun cardOnTopIsTakenPhaseFinalTurns(){
            val topDiscardCard = mockCard(value = 5)
            val initialState = mockGameState(phase = GamePhase.FINAL_TURNS, discardPile = DiscardPile(cards = listOf(mockCard(value = -1), topDiscardCard)))
            val newState = engine.takeDiscardCard(initialState)

            assertThat(newState.phase).isEqualTo(GamePhase.AWAITING_REPLACEMENT)
            assertThat(newState.drawSource).isEqualTo(DrawSource.DISCARD)
            assertThat(newState.drawnCard).isEqualTo(topDiscardCard)
            assertThat(newState.discardPile.size).isEqualTo(1)
        }

        @Test
        fun cardOnTopIsTakenWrongPhase(){
            val initialState = mockGameState(phase = GamePhase.AWAITING_REPLACEMENT)

            val exception = assertThrows<InvalidMoveException>{engine.takeDiscardCard(initialState)}
            assertThat(exception).hasMessageContaining("cannot take discard card while phase is")
        }
    }

    @Nested
    inner class ReplaceDrawnCardTets{
        @Test
        fun replaceDrawnCardWorks(){
            val drawnCard = mockCard(1, 5)
            val initialState = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawnCard = drawnCard,
                drawSource = DrawSource.DECK,
                currentPlayerIndex = 0
            )
            val targetPosition = mockPosition()
            val newState = engine.replaceDrawnCard(initialState, targetPosition)

            assertThat(newState.phase).isEqualTo(GamePhase.AWAITING_DRAW)
            assertThat(newState.drawSource).isNull()
            assertThat(newState.drawnCard).isNull()
            assertThat(newState.discardPile.size).isEqualTo(1)
        }

        @Test
        fun noDrawnCard(){
            val initialState = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK,
                currentPlayerIndex = 0 //keine DrawnCard (standartmäßig 0)
            )
            val targetPosition = mockPosition()
            val exception = assertThrows<InvalidMoveException>{engine.replaceDrawnCard(initialState, targetPosition)}
            assertThat(exception).hasMessageContaining("no drawn card is available")
        }
    }

    @Nested
    inner class DiscardDrawnCardAndRevealTest{
        @Test
        fun cardNotFromDrawDeck(){
            val initialState = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DISCARD //löst Fehler aus
            )
            val targetPosition = mockPosition()

            val exception = assertThrows<InvalidMoveException>{engine.discardDrawnCardAndReveal(initialState, targetPosition)}
            assertThat(exception).hasMessageContaining("discard and reveal is only allowed after drawing from the deck")
        }
        @Test
        fun noDrawnCard(){
            val initialState = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK
            )
            val targetPosition = mockPosition()

            val exception = assertThrows<InvalidMoveException> {engine.discardDrawnCardAndReveal(initialState, targetPosition)}
            assertThat(exception).hasMessageContaining("no drawn card is available")
        }

        @Test
        fun slotIsFaceUpOrCleared(){
            val targetPosition = mockPosition()
            val drawnCard = mockCard(1, 5)
            val boardFaceUp = mockPlayerBoard(faceUp = true)
            val boardCleared = mockPlayerBoardMissingColumn()
            val initialStateFaceUp = mockGameState( //alle Karten sind FaceUp -> Fehler
                players = listOf(mockPlayer(board = boardFaceUp), mockPlayer(board = boardFaceUp)),
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK,
                drawnCard = drawnCard
            )
            val initialStateCleared = mockGameState(
                players = listOf(mockPlayer(board = boardCleared), mockPlayer(board = boardCleared)),
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK,
                drawnCard = drawnCard
            )

            val exceptionFaceUp = assertThrows<InvalidMoveException> {engine.discardDrawnCardAndReveal(initialStateFaceUp, targetPosition)}
            assertThat(exceptionFaceUp).hasMessageContaining("discard and reveal requires a face-down occupied slot")

            val exceptionCleared = assertThrows<InvalidMoveException> {engine.discardDrawnCardAndReveal(initialStateCleared, targetPosition)}
            assertThat(exceptionCleared).hasMessageContaining("discard and reveal requires a face-down occupied slot")
        }



        @Test
        fun allWorks(){
            val targetPosition = mockPosition()
            val drawnCard = mockCard(1, 5)
            val initialState = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK,
                drawnCard = drawnCard
            )
            val newState = engine.discardDrawnCardAndReveal(initialState, targetPosition)

            assertThat(newState.phase).isEqualTo(GamePhase.AWAITING_DRAW)
            assertThat(newState.drawSource).isNull()
            assertThat(newState.drawnCard).isNull()
            assertThat(newState.discardPile.size).isEqualTo(1)
            assertThat(newState.discardPile.topCard()).isEqualTo(drawnCard)
        }
    }

    @Nested
    inner class ValidateSetupTest {

        @Test
        fun tooFewPlayers() {
            val playerIds = listOf("p1")
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0), mockPosition(0, 1))
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("between 2 and 8 players"))
        }

        @Test
        fun tooManyPlayers() {
            val playerIds = (1..9).map { "p$it" }
            val initialReveals = playerIds.associateWith {
                setOf(mockPosition(0, 0), mockPosition(0, 1))
            }

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("between 2 and 8 players"))
        }

        @Test
        fun noUniqueIDs() {
            val playerIds = listOf("p1", "p2", "p1") // "p1" ist doppelt
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0), mockPosition(0, 1)),
                "p2" to setOf(mockPosition(1, 0), mockPosition(1, 1))
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("player ids must be unique"))
        }

        @Test
        fun blankIDs() {
            val playerIds = listOf("p1", "   ") // Blank ID
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0), mockPosition(0, 1)),
                "   " to setOf(mockPosition(1, 0), mockPosition(1, 1))
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("player ids must not be blank"))
        }

        @Test
        fun missingReveals() {
            val playerIds = listOf("p1", "p2")
            // Reveal für "p2" fehlt
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0), mockPosition(0, 1))
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("initial reveals must be provided for every player id"))
        }

        @Test
        fun unknownID() {
            val playerIds = listOf("p1", "p2")
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0), mockPosition(0, 1)),
                "p2" to setOf(mockPosition(1, 0), mockPosition(1, 1)),
                "p3" to setOf(mockPosition(2, 0), mockPosition(2, 1)) // p3 spielt gar nicht mit
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("initial reveals must be provided for every player id"))
        }

        @Test
        fun notEnoughRevealsPerPlayer() {
            val playerIds = listOf("p1", "p2")
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0)), // Nur 1 Position!
                "p2" to setOf(mockPosition(1, 0), mockPosition(1, 1))
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("each player must reveal exactly two positions"))
        }

        @Test
        fun tooManyRevealsPerPlayer() {
            val playerIds = listOf("p1", "p2")
            val initialReveals = mapOf(
                "p1" to setOf(mockPosition(0, 0), mockPosition(0, 1), mockPosition(0, 2)), // 3 Positionen!
                "p2" to setOf(mockPosition(1, 0), mockPosition(1, 1))
            )

            val exception = assertThrows<InvalidGameSetupException> {
                engine.startGame(playerIds, initialReveals)
            }
            assertTrue(exception.message!!.contains("each player must reveal exactly two positions"))
        }
    }

    @Nested
    inner class RequireActiveRoundTest {

        @Test
        fun gameNotStarted() {
            val state = mockGameState(phase = GamePhase.NOT_STARTED)

            assertThrows<GameNotStartedException> { engine.drawFromDeck(state) }
        }

        @Test
        fun roundIsFinished() {
            val state = mockGameState(phase = GamePhase.ROUND_FINISHED)


            assertThrows<RoundAlreadyFinishedException> { engine.takeDiscardCard(state) }
        }

        @Test
        fun awaitingDraw() {
            val state = mockGameState(phase = GamePhase.AWAITING_DRAW, drawPile = DrawPile(cards = listOf(mockCard(), mockCard())))
            val result = engine.drawFromDeck(state)

            assertEquals(GamePhase.AWAITING_REPLACEMENT, result.phase)
        }

        @Test
        fun finalTurns() {
            val state = mockGameState(phase = GamePhase.FINAL_TURNS, drawPile = DrawPile(cards = listOf(mockCard(), mockCard())))
            val result = engine.drawFromDeck(state)

            assertEquals(GamePhase.AWAITING_REPLACEMENT, result.phase)
        }
    }

    @Nested
    inner class RequireAwaitingReplacementTest {

        @Test
        fun awaitingDraw() {
            val state = mockGameState(phase = GamePhase.AWAITING_DRAW)
            val pos = mockPosition()

            val exception = assertThrows<InvalidMoveException> {
                engine.replaceDrawnCard(state, pos)
            }
            assertTrue(exception.message!!.contains("a card has to be drawn before this action"))
        }

        @Test
        fun finalTurns() {
            val state = mockGameState(phase = GamePhase.FINAL_TURNS)
            val pos = mockPosition()

            val exception = assertThrows<InvalidMoveException> {
               engine.replaceDrawnCard(state, pos)
            }
            assertTrue(exception.message!!.contains("a card has to be drawn before this action"))
        }


        @Test
        fun notStarted() {
            val state = mockGameState(phase = GamePhase.NOT_STARTED)
            val pos = mockPosition()

            assertThrows<GameNotStartedException> {
                engine.replaceDrawnCard(state, pos)
            }
        }

        @Test
        fun roundFinished() {
            val state = mockGameState(phase = GamePhase.ROUND_FINISHED)
            val pos = mockPosition()

            assertThrows<RoundAlreadyFinishedException> {
                engine.replaceDrawnCard(state, pos)
            }
        }
    }

    @Nested
    inner class ReplenishDrawPileIfNeededTests {

        @Test
        fun drawPileFull() {
            val state = mockGameState(
                phase = GamePhase.AWAITING_DRAW,
                drawPile = DrawPile(listOf(mockCard(id = 1))),
                discardPile = DiscardPile(listOf(mockCard(id = 2), mockCard(id = 3)))
            )
            val result = engine.drawFromDeck(state)

            assertEquals(result.phase, GamePhase.AWAITING_REPLACEMENT) //war erfolgreich, nächste phase
        }

        @Test
        fun emptyDeckEmptyDiscard() {
            val state = mockGameState(
                phase = GamePhase.AWAITING_DRAW,
                drawPile = DrawPile.empty(),
                discardPile = DiscardPile(listOf(mockCard(id = 1)))
            )
            val exception = assertThrows<InvalidMoveException> {
                engine.drawFromDeck(state)
            }
            assertTrue(exception.message!!.contains("discard pile has no spare cards"))
        }

        @Test
        fun replenishesDrawPile() {
            val card1 = mockCard(id = 1)
            val card2 = mockCard(id = 2)
            val topCard = mockCard(id = 3) // Diese muss auf dem Ablagestapel bleiben
            val state = mockGameState(
                phase = GamePhase.AWAITING_DRAW,
                drawPile = DrawPile.empty(),
                discardPile = DiscardPile(listOf(card1, card2, mockCard(), topCard)),
                shuffleCount = 0
            )
            val result = engine.drawFromDeck(state)

            assertEquals(2, result.drawPile.size) // card1 und card2 wurden gemischt
            assertEquals(1, result.discardPile.size) // Nur noch die topCard ist da
            assertEquals(topCard, result.discardPile.cards.last()) // Prüft, ob die richtige Karte geschützt wurde
            assertEquals(1, result.shuffleCount) // Count wurde hochgezählt
        }

        @Test
        fun shuffelsCardsCorrectly() {
            val cards = List(10) { mockCard(id = it) }
            val topCard = mockCard(id = 99)
            val state1 = mockGameState(
                phase = GamePhase.AWAITING_DRAW,
                drawPile = DrawPile.empty(),
                discardPile = DiscardPile(cards + topCard),
                shuffleSeed = 42L,
                shuffleCount = 2
            )
            // Ein exakter Klon des States, um zu prüfen, ob das Mischen identisch abläuft
            val state2 = mockGameState(
                phase = GamePhase.AWAITING_DRAW,
                drawPile = DrawPile.empty(),
                discardPile = DiscardPile(cards + topCard),
                shuffleSeed = 42L,
                shuffleCount = 2
            )
            val result1 = engine.drawFromDeck(state1)
            val result2 = engine.drawFromDeck(state2)

            assertEquals(result1.drawPile.cards, result2.drawPile.cards)
        }
    }

    @Nested
    inner class AdvanceAfterTurnTest {

        @Test
        fun advancesToAwaitingDraw() {
            //Normaler Spielzug, Spieler 1 hat noch verdeckte Karten (faceUp = false)
            val p1 = mockPlayer("p1", mockPlayerBoard(faceUp = false))
            val p2 = mockPlayer("p2", mockPlayerBoard(faceUp = false))
            val drawnCard = mockCard()
            val lastPos = mockPosition(0,0)
            val state = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK,
                drawnCard = drawnCard,
                players = listOf(p1, p2),
                currentPlayerIndex = 0 // p1 ist dran
            )
            val result = engine.discardDrawnCardAndReveal(state, lastPos)


            assertEquals(1, result.currentPlayerIndex) // p2 ist als nächstes dran
            assertEquals(GamePhase.AWAITING_DRAW, result.phase)
            assertNull(result.finisherPlayerId)
        }

        @Test
        fun finalTurnsAfterAllCardsOpen() {
            //Spieler 1 hat soeben seine letzte Karte aufgedeckt (faceUp = true)
            val p1 = mockPlayer("p1", mockPlayerBoardOneFaceDown())
            val p2 = mockPlayer("p2", mockPlayerBoard(faceUp = false))
            val lastPos = mockPosition(0,0)
            val state = mockGameState(
                phase = GamePhase.AWAITING_REPLACEMENT,
                drawSource = DrawSource.DECK,
                drawnCard = mockCard(),
                players = listOf(p1, p2),
                currentPlayerIndex = 0,
                finisherPlayerId = null
            )
            val result = engine.discardDrawnCardAndReveal(state, lastPos)

            assertEquals(1, result.currentPlayerIndex) // p2 ist dran
            assertEquals(GamePhase.FINAL_TURNS, result.phase)
            assertEquals("p1", result.finisherPlayerId) // p1 hat das Ende eingeleitet
            assertEquals(1, result.finalTurnsRemaining) // Bei 2 Spielern bleibt noch 1 Zug
        }

        @Test
        fun decrementsFinalTurnsRemaining() {
            // p1 hat das Ende eingeleitet, wir sind in den letzten Zügen.
            // 3 Spieler insgesamt, p2 hat seinen Zug beendet.
            val p1 = mockPlayer("p1", mockPlayerBoard(faceUp = true))
            val p2 = mockPlayer("p2", mockPlayerBoard(faceUp = false))
            val p3 = mockPlayer("p3", mockPlayerBoard(faceUp = false))
            val pos = mockPosition(0,0)
            val drawnCard = mockCard()
            val state = mockGameState(
                drawSource = DrawSource.DECK,
                drawnCard = drawnCard,
                players = listOf(p1, p2, p3),
                currentPlayerIndex = 1, // p2 war gerade dran
                finisherPlayerId = "p1",
                finalTurnsRemaining = 2, // p2 und p3 durften noch
                phase = GamePhase.AWAITING_REPLACEMENT
            )
            val result = engine.discardDrawnCardAndReveal(state, pos)

            assertEquals(2, result.currentPlayerIndex) // p3 ist nun dran
            assertEquals(GamePhase.FINAL_TURNS, result.phase)
            assertEquals(1, result.finalTurnsRemaining) // Nur noch p3 darf
        }

        @Test
        fun finalTurnsRemainingReaches0() {
            // p1 hat das Ende eingeleitet. p2 macht seinen letzten Zug.
            val p1 = mockPlayer("p1", mockPlayerBoard(faceUp = true))
            val p2 = mockPlayer("p2", mockPlayerBoard(faceUp = false))
            val pos = mockPosition()

            val state = mockGameState(
                drawSource = DrawSource.DECK,
                drawnCard = mockCard(),
                players = listOf(p1, p2),
                currentPlayerIndex = 1, // p2 war gerade dran
                finisherPlayerId = "p1",
                finalTurnsRemaining = 1, // Dies war der letzte mögliche Zug
                phase = GamePhase.AWAITING_REPLACEMENT
            )

            val result = engine.discardDrawnCardAndReveal(state, pos)

            // Then: Wir prüfen, ob die Phase auf ROUND_FINISHED gesprungen ist.
            // Das beweist, dass die finishRound() Methode aufgerufen wurde.
            assertEquals(GamePhase.ROUND_FINISHED, result.phase)
            assertEquals(0, result.finalTurnsRemaining)
        }

        @Test
        fun edgeCaseOnePlayer() {
            // Dieser Test simuliert einen Edge-Case, der durch die Validierung eigentlich verhindert wird, aber den logischen Zweig "finalTurns <= 0" abdeckt.
            // Nur ein Spieler, der gerade seine letzte Karte aufgedeckt hat
            val p1 = mockPlayer("p1", mockPlayerBoardOneFaceDown())
            val pos = mockPosition()
            val state = mockGameState(
                drawSource = DrawSource.DECK,
                drawnCard = mockCard(),
                players = listOf(p1),
                currentPlayerIndex = 0,
                finisherPlayerId = null,
                phase = GamePhase.AWAITING_REPLACEMENT
            )
            val result = engine.discardDrawnCardAndReveal(state, pos)

            //Der Zweig (finalTurns <= 0) wird ausgelöst
            assertEquals(GamePhase.ROUND_FINISHED, result.phase)
            assertEquals("p1", result.finisherPlayerId)
            assertEquals(0, result.finalTurnsRemaining)
        }
    }

    @Nested
    inner class FinishRoundTest {

        @Test
        fun `calculates scores correctly without any clearing`() {
            // 12 Karten mit Wert 1 = 12 Punkte (Keine Spalte gleich, da Skyjo 3 pro Spalte hat)
            // Wir nehmen Werte, die sich in der Spalte unterscheiden: 1, 2, 3, 1, 2, 3...
            val finisherBoard = mockPlayerBoardWithValues(listOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3)) // Summe = 4 * (1+2+3) = 24
            val otherBoard = mockPlayerBoardWithValues(listOf(5, 5, 6, 5, 5, 6, 5, 5, 6, 5, 5, 6)) // Summe = 4 * (5+5+6) = 64
            val p1 = mockPlayer("finisher", finisherBoard)
            val p2 = mockPlayer("other", otherBoard)
            val state = mockGameState(
                players = listOf(p1, p2),
                finisherPlayerId = "finisher",
                phase = GamePhase.FINAL_TURNS
            )
            val result = engine.finishRound(state)
            val finisherResult = result.roundResult!!.scores.first { it.playerId == "finisher" }
            val otherResult = result.roundResult!!.scores.first { it.playerId == "other" }

            assertEquals(24, finisherResult.rawScore)
            assertEquals(64, otherResult.rawScore)
        }

        @Test
        fun `doubles finisher score on a tie`() {
            // Beide Spieler haben exakt 24 Punkte (4 Spalten à 1+2+3)
            val values = listOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3)
            val finisherBoard = mockPlayerBoardWithValues(values)
            val otherBoard = mockPlayerBoardWithValues(values)
            val p1 = mockPlayer("finisher", finisherBoard)
            val p2 = mockPlayer("other", otherBoard)
            val state = mockGameState(
                players = listOf(p1, p2),
                finisherPlayerId = "finisher",
                phase = GamePhase.FINAL_TURNS
            )
            val result = engine.finishRound(state)
            val finisherResult = result.roundResult!!.scores.first { it.playerId == "finisher" }

            // Raw: 24, Final: 48 (Verdoppelt wegen Gleichstand)
            assertEquals(24, finisherResult.rawScore)
            assertEquals(48, finisherResult.finalScore)
        }

        @Test
        fun `doubles finisher score when another player is better`() {
            // Finisher hat 64 Punkte (4 Spalten à 5+5+6)
            val finisherBoard = mockPlayerBoardWithValues(listOf(5, 5, 6, 5, 5, 6, 5, 5, 6, 5, 5, 6))
            // Andere Spieler hat nur 24 Punkte
            val otherBoard = mockPlayerBoardWithValues(listOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3))
            val p1 = mockPlayer("finisher", finisherBoard)
            val p2 = mockPlayer("other", otherBoard)
            val state = mockGameState(
                players = listOf(p1, p2),
                finisherPlayerId = "finisher"
            )
            val result = engine.finishRound(state)
            val finisherResult = result.roundResult!!.scores.first { it.playerId == "finisher" }

            // Raw: 64, Final: 128
            assertEquals(64, finisherResult.rawScore)
            assertEquals(128, finisherResult.finalScore)
        }

        @Test
        fun `does not double negative scores even if someone else is lower`() {
            // Finisher hat -12 Punkte (4 Spalten à -1, 0, -2)
            val finisherBoard = mockPlayerBoardWithValues(listOf(-1, 0, -2, -1, 0, -2, -1, 0, -2, -1, 0, -2))
            // Jemand anderes ist noch besser: -24 Punkte
            val otherBoard = mockPlayerBoardWithValues(listOf(-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2))
            val p1 = mockPlayer("finisher", finisherBoard)
            val p2 = mockPlayer("other", otherBoard)
            val state = mockGameState(
                players = listOf(p1, p2),
                finisherPlayerId = "finisher"
            )
            val result = engine.finishRound(state)
            val finisherResult = result.roundResult!!.scores.first { it.playerId == "finisher" }

            // Bleibt bei -12, da negative Scores nicht verdoppelt werden
            assertEquals(-12, finisherResult.rawScore)
            assertEquals(-12, finisherResult.finalScore)
        }

        @Test
        fun `throws InvalidMoveException if finisherPlayerId is null`() {
            // Given
            val state = mockGameState(
                phase = GamePhase.FINAL_TURNS,
                finisherPlayerId = null
            )

            // When / Then
            val exception = assertThrows<InvalidMoveException> {
                engine.finishRound(state) // Setze die Methode im Code ggf. auf internal
            }
            assertTrue(exception.message!!.contains("cannot finish round without a finisher"))
        }
    }

    @Nested
    inner class StartingPlayerLogic {

        @Test
        fun highestVisibleSum() {
            // Player 0: Enthüllt (0,0) und (1,0) -> Werte 1 und 2. Summe = 3
            val p0 = mockPlayer("p0", mockPlayerBoardWithValues(listOf(1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))

            // Player 1: Enthüllt (0,0) und (1,0) -> Werte 5 und 10. Summe = 15
            val p1 = mockPlayer("p1", mockPlayerBoardWithValues(listOf(5, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))

            val initialReveals = mapOf(
                "p0" to setOf(mockPosition(0, 0), mockPosition(1, 0)),
                "p1" to setOf(mockPosition(0, 0), mockPosition(1, 0))
            )
            val startIndex = engine.determineStartingPlayerIndex(listOf(p0, p1), initialReveals)

            // Player 1 (Index 1) hat 15, Player 0 nur 3.
            assertEquals(1, startIndex)
        }

        @Test
        fun firstPlayerWhenTie() {
            // Beide haben die gleiche Summe (3)
            val p0 = mockPlayer("p0", mockPlayerBoardWithValues(listOf(1, 2, 10, 10)))
            val p1 = mockPlayer("p1", mockPlayerBoardWithValues(listOf(1, 2, 5, 5)))

            val initialReveals = mapOf(
                "p0" to setOf(mockPosition(0, 0), mockPosition(1, 0)),
                "p1" to setOf(mockPosition(0, 0), mockPosition(1, 0))
            )

            val startIndex = engine.determineStartingPlayerIndex(listOf(p0, p1), initialReveals)

            // maxByOrNull gibt bei Gleichstand das erste Element zurück
            assertEquals(0, startIndex)
        }

        @Test
        fun negativeValues() {
            // Wir definieren zwei feste Positionen für den Reveal
            val pos1 = mockPosition(col = 0, row = 0)
            val pos2 = mockPosition(col = 1, row = 0)
            val revealSet = setOf(pos1, pos2)

            // Player 0: Summe soll -5 sein (-3 + -2)
            val p0 = mockPlayer("p0", mockPlayerBoardWithExplicitValues(mapOf(
                pos1 to -3,
                pos2 to -2
            )))

            // Player 1: Summe soll -2 sein (0 + -2)
            val p1 = mockPlayer("p1", mockPlayerBoardWithExplicitValues(mapOf(
                pos1 to 0,
                pos2 to -2
            )))

            val initialReveals = mapOf("p0" to revealSet, "p1" to revealSet)
            val startIndex = engine.determineStartingPlayerIndex(listOf(p0, p1), initialReveals)

            // Player 1 (Index 1) muss anfangen, da -2 größer ist als -5
            assertEquals(1, startIndex, "Player 1 should start because -2 > -5")
        }

        @Test
        fun emptyPlayerList() {
            // Dieser Test deckt das ?: 0 am Ende ab
            val startIndex = engine.determineStartingPlayerIndex(emptyList(), emptyMap())
            assertEquals(0, startIndex)
        }
    }

    //Hilfsfunktionen (Mocking)
    private fun mockPosition(col: Int = 0, row: Int = 0) = BoardPosition(col, row)
    private fun mockCard(id: Int = 1, value: Int = 1) = SkyjoCard(id, value)
    private fun mockPlayer(id: String = "p1", board: PlayerBoard = mockPlayerBoard()) = PlayerState(id, board)
    private fun mockPlayerBoard(defaultValue: Int = 0, faceUp: Boolean = false): PlayerBoard {
        val slots = BoardLayout.POSITIONS.associateWith{pos ->
            val uniqueId = pos.row * BoardLayout.COLUMNS + pos.column
            BoardSlot.Occupied(SkyjoCard(id = uniqueId, defaultValue), faceUp)
        }
        return PlayerBoard(slots)
    }
    private fun mockPlayerBoardWithValues(values: List<Int>): PlayerBoard {
        // Falls die Liste zu kurz ist, füllen wir mit hohen Werten auf
        val cardValues = values + List(12) { it + 100 }

        val slots = BoardLayout.POSITIONS.mapIndexed { index, pos ->
            val uniqueId = pos.row * BoardLayout.COLUMNS + pos.column
            pos to BoardSlot.Occupied(SkyjoCard(id = uniqueId, value = cardValues[index]), faceUp = true)
        }.toMap()
        return PlayerBoard(slots)
    }
    private fun mockPlayerBoardWithExplicitValues(positionValues: Map<BoardPosition, Int>): PlayerBoard {
        val slots = BoardLayout.POSITIONS.associateWith { pos ->
            val value = positionValues[pos] ?: 0 // Nimm den definierten Wert oder 0
            val uniqueId = pos.row * BoardLayout.COLUMNS + pos.column
            BoardSlot.Occupied(SkyjoCard(id = uniqueId, value = value), faceUp = true)
        }
        return PlayerBoard(slots)
    }
    private fun mockPlayerBoardMissingColumn(faceUp: Boolean = false): PlayerBoard {
        val slots = BoardLayout.POSITIONS.associateWith{pos ->
            val uniqueId = pos.row * BoardLayout.COLUMNS + pos.column
            val uniqueVal = pos.row + pos.column
            val clear = pos.column == 0
            if (clear) {
                BoardSlot.Cleared
            }else {
                BoardSlot.Occupied(SkyjoCard(id = uniqueId, uniqueVal), faceUp)
            }
        }
        return PlayerBoard(slots)
    }
    private fun mockPlayerBoardOneFaceDown(faceUp: Boolean = true): PlayerBoard {
        val slots = BoardLayout.POSITIONS.associateWith{pos ->
            val uniqueId = pos.row * BoardLayout.COLUMNS + pos.column
            val uniqueVal = pos.row + pos.column
            val close = pos.column == 0 && pos.row == 0
            if (close) {
                BoardSlot.Occupied(SkyjoCard(id = uniqueId, uniqueVal), faceUp = false)
            }else {
                BoardSlot.Occupied(SkyjoCard(id = uniqueId, uniqueVal), faceUp)
            }
        }
        return PlayerBoard(slots)
    }
    private fun mockGameState(
        players: List<PlayerState> = listOf(mockPlayer("p1"), mockPlayer("p2")),
        currentPlayerIndex: Int = 0,
        phase: GamePhase = GamePhase.NOT_STARTED,
        drawPile: DrawPile = DrawPile.empty(),
        discardPile: DiscardPile = DiscardPile.empty(),
        drawnCard: SkyjoCard? = null,
        drawSource: DrawSource? = null,
        finisherPlayerId: String? = null,
        finalTurnsRemaining: Int = 0,
        roundResult: RoundResult? = null,
        shuffleSeed: Long? = 123L,
        shuffleCount: Int = 0
    )= GameState(
        players = players,
        currentPlayerIndex = currentPlayerIndex,
        drawPile = drawPile,
        discardPile = discardPile,
        phase = phase,
        drawnCard = drawnCard,
        drawSource = drawSource,
        finisherPlayerId = finisherPlayerId,
        finalTurnsRemaining = finalTurnsRemaining,
        roundResult = roundResult,
        shuffleSeed = shuffleSeed,
        shuffleCount = shuffleCount
    )

}