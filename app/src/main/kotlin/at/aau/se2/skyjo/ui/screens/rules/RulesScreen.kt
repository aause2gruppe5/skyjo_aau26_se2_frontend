package at.aau.se2.skyjo.ui.screens.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.aau.se2.skyjo.ui.components.*
import at.aau.se2.skyjo.ui.navigation.AppDestination
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Surface
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.toArgb
import android.widget.TextView

//Importiere hier dein bevorzugtes Scaffold, z.B.:
//import at.aau.se2.skyjo.ui.components.*
// import at.aau.se2.skyjo.ui.navigation.AppDestination

@Composable
fun RulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ein eigenes Scaffold nur für diesen Unter-Screen
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Falls du BackgroundGray in deinem Theme definiert hast, trage es hier ein.
        // Ansonsten nutzt es die Standard-Hintergrundfarbe.
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Angepasste Top-Bar mit Zurück-Pfeil, passend zu deinem Design
            Surface(
                modifier = Modifier.fillMaxWidth(),
                // color = SurfaceWhite, // Nutze hier deine definierte SurfaceWhite Farbe
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(80.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            // Nutze AutoMirrored für korrekte Darstellung in RTL-Sprachen
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            // tint = PrimaryGreen // Nutze hier deine PrimaryGreen Farbe
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HOW TO PLAY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        // color = PrimaryGreen // Nutze hier deine PrimaryGreen Farbe
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    ) { paddingValues ->
        // Der scrollbare Inhalt des Screens
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Hier fügen wir das Padding des Scaffolds ein (damit die TopBar nichts verdeckt)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SKYJO ACTION",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Hier binden wir den formatierten Text ein (Möglichkeit 2 mit AndroidView)
            val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
            val htmlText = skyjoRulesText.replace("\n", "<br>")

            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        textSize = 16f
                        setTextColor(textColor)
                        setLineSpacing(12f, 1.2f) // Etwas mehr Zeilenabstand für bessere Lesbarkeit
                    }
                },
                update = { textView ->
                    textView.text = HtmlCompat.fromHtml(
                        htmlText,
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private val skyjoRulesText = """
<b>GAME IDEA AND GOAL</b>
The game <i>SKYJO ACTION</i> is played over several rounds that can be chosen by the Host and ends as soon as a player reaches <i>100</i> or more points or the chosen number of rounds are over. The player who has scored the fewest points at the end wins the game. Thus, the goal of the game is to collect as few points as possible.

<b>PREPARING THE GAME ROUND</b>
Two well-shuffled card decks are formed at the beginning of the game: a playing deck consisting of numbered and star cards, and an action deck consisting of action cards.

First, each player is dealt twelve cards face-down from the playing deck. Then one card is placed face-up in the middle of the table, forming the discard pile for the playing cards. The remaining cards are placed face-down next to it as a draw pile.

Next, four action cards from the action deck are placed face-up in a row, and the remaining action cards are placed next to them as an action card draw pile. If one of the four action cards is taken during the game, the empty space is to be filled immediately by the top card of the action card draw pile.

<b>START AND GAMEPLAY OF A GAME ROUND</b>
The game is played over several rounds. At the beginning of a round, each player lays out their twelve playing cards face-down in four vertical rows of three cards. Two cards are turned over.

The first round of play may be started by the player with the highest card value (sum) of the two exposed cards.

For all subsequent rounds: The player who finished the last round may start the current round. Beginning with the starting player, the players take their turn clockwise.

<b>GAMEPLAY</b>
During their turn, a player must choose and perform one of three alternative actions. The turn ends with the execution.

<i>Drawing a playing card:</i> Draw the top card from the discard pile or the draw pile.

<i>Drawing an action card:</i> Take one of the four face-up action cards or draw the top face-down card from the action card draw pile.

<i>Playing or discarding an action card:</i> Play an action card from their own layout or place it on the discard pile with no action performed.

<i>1)</i> The first available move is to draw a playing card:

a. Either by drawing the top card face-up from the discard pile or

b. by drawing the top card face-down from the draw pile.

<i>Re a.:</i> If the player chooses a face-up card from the discard pile, they must immediately exchange it with one of their playing cards and place the card face-up. They may decide freely among their face-up and face-down cards, and the face-down cards must not be turned over and looked at before the exchange. The exchanged card is placed face-up on the discard pile.

<i>Re b.:</i> If the player chooses the face-down card from the draw pile, they may look at the card beforehand and choose whether to exchange it for one of their (face-down or face-up) playing cards. If they want to keep the drawn card, they exchange it with one of their cards, as explained above. If they do not want to keep the drawn card, they place it on the discard pile and must instead reveal one of their face-down cards.

<i>2)</i> The second available move is to draw an action card:

a) Either draw one of the four face-up action cards,

b) or draw the top face-down action card from the action card draw pile.

The action card drawn is placed next to the player's own playing card layout, and may only be played from the next turn.

<i>3)</i> The third available move is to play or discard an action card from their own layout. The turn ends upon completion of the action, and the action card played is no longer in their collection. Each action card may also be discarded without being used. Even then, the turn ends immediately.

<b>COMPLETION AND SCORES OF A GAME ROUND</b>
Play continues until one player reveals their last face-down card. This ends the game round, and each subsequent player may make one more move.
<i>Example:</i> If player B ends the round by revealing their last card, all players except player B have one more move.

Then, each player's face-up and face-down cards are counted and added to their previous score. Numbered cards count as much as is indicated by their numerical value. Action cards that are still in a player's layout have a value of 10 points each.

<b>FEATURES</b>

<b>SPECIAL RULE 1:</b>
The player who has finished the game round (regardless of whose cards are used) must have the lowest score when counting the score of the game round. If they do not have the lowest score because another player has less or the same number of points, the points they have collected in this round are doubled. Note: This rule does not apply if the player's score in the round is negative or zero.

<i>Example I:</i> Player A turns over their last card and thus ends the game round. Player A has 10 points at the end of the game round, player B has 24 points, and player C also has 10 points. Player A does not have the lowest score, so their points are doubled to 20.

<i>Example II:</i> Player A reveals all their cards and ends the game round. At the end of the game round, they have scored -5 points, player B 10 points, and player C -7 points. Because Player A's score is negative, the doubling rule does not apply.

<b>SPECIAL RULE 2:</b>
The player must discard the entire row of cards (also rows of minus cards) if they have equal numbered cards in a complete vertical or horizontal row at the end of their turn.

<i>Note:</i> This rule only applies to rows of cards with at least three cards (triplets and quads).

This rule also applies in the last turn of a game round when the remaining face-down cards are turned over and counted for the game score.

If the row of cards is obtained as a result of a swap with the draw pile or discard pile, the triplet/quad must always be placed on the discard pile after the swapped card.

<b>ACTION CARDS</b>
All action cards taken into a player's hand must not be played until the next turn.

Any number of action cards may be taken and held by a player.

Action cards may not be played or discarded in the final round of the game (once a player has all their cards face-up).

<b>INDIVIDUAL ACTION CARDS</b>
<b>Swap your own cards:</b> Swap any two of a player's own cards with each other. Each card may be taken as desired (face-down or face-up).

<b>Double move (2x):</b> By playing this action card, the player may play two consecutive moves.

<b>Draw three cards:</b> Draw three cards from the playing card draw pile and keep one of them if desired. The remaining cards are placed in any order on the discard pile. If no card is taken, a card from the player's own layout must be turned face-up.

<b>Enlightenment:</b> Look at a column or row: The player may look at the face-down cards of a complete column or row of their own or another player's cards.

<b>Defence + additional move:</b> If this action card is in a player's layout, it protects them once from an "attack" action card. If an attack is made on them, the protection automatically takes effect, and the "Defence" action card is used up. The player may play the Defence card to discard it and to get an additional turn. This ends the protection immediately.

<b>Swap cards with another player (attack card):</b> Swap one card each between two players or between the player and one other player. Both cards can be chosen freely, and may be taken as desired (face-down or face-up).
    """.trimIndent()

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Rules Screen Preview"
)
@Composable
private fun RulesScreenPreview() {
    SkyjoTheme {
        Surface {
            RulesScreen(
                onBack = {  }
            )
        }
    }
}