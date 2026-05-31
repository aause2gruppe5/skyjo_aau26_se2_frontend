package at.aau.se2.skyjo.ui.screens.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.components.SkyjoDrawerScaffold
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme

@Composable
fun LeaderboardScreen(
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    entries: List<LeaderboardEntryDto> = emptyList(),
) {
    SkyjoDrawerScaffold(
        currentDestination = AppDestination.Leaderboard,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Leaderboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (entries.isEmpty()) {
                SkyjoCard {
                    Text("No games on the leaderboard yet", color = MutedText)
                }
            } else {
                entries.forEach { entry ->
                    SkyjoCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("#${entry.rank} ${entry.username}", fontWeight = FontWeight.Bold)
                                Text("${entry.gamesPlayed} Games - ${entry.wins} Wins", color = MutedText)
                            }
                            Text("%.1f".format(entry.averageScore), color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardScreenPreview() {
    SkyjoTheme {
        LeaderboardScreen(onNavigate = {})
    }
}
