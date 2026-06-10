package at.aau.se2.skyjo.ui.screens.gameOver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.ui.components.*
import at.aau.se2.skyjo.ui.theme.*
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.model.TotalScore
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Scaffold
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Box



@Composable
fun GameOverScreen(
    totalScores: List<TotalScore>,
    onBackToStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedScores = totalScores.sortedBy { it.totalScore }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(80.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBackToStart) { // Nutzt denselben Callback wie der Button
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Start",
                            tint = PrimaryGreen,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SKYJO ACTION",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen,
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Wichtig: Scaffold-Padding anwenden, damit die TopBar nichts verdeckt
                .padding(24.dp), // Dein bestehendes Screen-Padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GAME OVER",
                    style = MaterialTheme.typography.displayLarge,
                    color = PrimaryGreen,
                )
            }

            // Leaderboard Card
            SkyjoCard(
                modifier = Modifier.weight(4f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionTitle(
                        eyebrow = "Final Results",
                        title = "Leaderboard",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                        .fillMaxWidth()
                        // 1. Takes the remaining space below the title
                        .weight(1f)
                        // 2. Makes the column scrollable
                        .verticalScroll(rememberScrollState())
                    ) {
                        sortedScores.forEachIndexed { index, score ->
                            LeaderboardRow(index = index, totalScore = score)

                            if (index < sortedScores.lastIndex) {
                                HorizontalDivider(color = BorderColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            PrimaryButton(
                text = "BACK TO START",
                onClick = onBackToStart
            )
        }
    }
}

@Composable
private fun LeaderboardRow(index: Int, totalScore: TotalScore) {
    val rankIndicator = when (index) {
        0 -> "🏆"
        1 -> "🥈"
        2 -> "🥉"
        else -> "${index + 1}."
    }

    val isWinner = index == 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rankIndicator,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = totalScore.nickname,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isWinner) DarkText else MediumText,
            modifier = Modifier.weight(1f)
        )

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = if (isWinner) GoldSurface else BackgroundGray,
        ) {
            Text(
                text = "${totalScore.totalScore} pts",
                style = MaterialTheme.typography.titleMedium,
                color = if (isWinner) GoldDark else MediumText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true) // showSystemUi simulates a full phone screen
@Composable
private fun GameOverScreenPreview() {
    SkyjoTheme {
        GameOverScreen(
            totalScores = listOf(
                TotalScore(nickname = "Alice", playerId = "1", totalScore = -15), // 1st
                TotalScore(nickname = "Diana", playerId = "2", totalScore = 120), // 4th
                TotalScore(nickname = "Bob", playerId = "3", totalScore = 34),    // 2nd
                TotalScore(nickname = "Charlie", playerId = "4", totalScore = 87),
                TotalScore(nickname = "Bob2", playerId = "5", totalScore = 34),    // 2nd
                TotalScore(nickname = "Charlie2", playerId = "6", totalScore = 87) // 3rd
            ),
            onBackToStart = {}
        )
    }
}