package at.aau.se2.skyjo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.haptic.LocalHaptic
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.BackgroundGray
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.ui.theme.SurfaceWhite

// ── Top Bar ────────────────────────────────────────────────────────────────

@Composable
fun SkyjoTopBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SKYJO ACTION",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen,
            )
        }
    }
}

// ── Bottom Nav ─────────────────────────────────────────────────────────────

private data class NavTab(
    val destination: AppDestination,
    val icon: ImageVector,
    val label: String,
)

private val navTabs = listOf(
    NavTab(AppDestination.Start, Icons.Default.PlayArrow, "Play"),
    NavTab(AppDestination.Friends, Icons.Default.Group, "Friends"),
    NavTab(AppDestination.Leaderboard, Icons.Default.EmojiEvents, "Board"),
    NavTab(AppDestination.Settings, Icons.Default.Settings, "Settings"),
)

@Composable
fun SkyjoBottomNavBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = SurfaceWhite,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                val haptic = LocalHaptic.current
                navTabs.forEach { tab ->
                    val selected = currentDestination == tab.destination
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                haptic?.tick()
                                onNavigate(tab.destination)
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selected) MintGreen else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) PrimaryGreen else MutedText,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) PrimaryGreen else MutedText,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

// ── Full Scaffold Wrapper ──────────────────────────────────────────────────

@Composable
fun SkyjoDrawerScaffold(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundGray,
        topBar = { SkyjoTopBar() },
        bottomBar = {
            SkyjoBottomNavBar(
                currentDestination = currentDestination,
                onNavigate = onNavigate,
            )
        },
    ) { paddingValues ->
        content(paddingValues)
    }
}

// ── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    SkyjoTheme { SkyjoTopBar() }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun BottomNavPreview() {
    SkyjoTheme {
        SkyjoBottomNavBar(
            currentDestination = AppDestination.Start,
            onNavigate = {},
        )
    }
}
