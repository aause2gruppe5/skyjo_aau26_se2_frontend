package at.aau.se2.skyjo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.BackgroundGray
import at.aau.se2.skyjo.ui.theme.DangerRed
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch

// ── Top Bar ────────────────────────────────────────────────────────────────

@Composable
fun SkyjoTopBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceWhite,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
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

// ── Bottom Nav ─────────────────────────────────────────────────────────────

private data class NavTab(
    val destination: AppDestination,
    val icon: ImageVector,
    val label: String,
)

private val navTabs = listOf(
    NavTab(AppDestination.Start, Icons.Default.PlayArrow, "Play"),
    NavTab(AppDestination.Friends, Icons.Default.Group, "Friends"),
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
                navTabs.forEach { tab ->
                    val selected = currentDestination == tab.destination
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigate(tab.destination) }
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

// ── Drawer Content ─────────────────────────────────────────────────────────

private data class DrawerItem(
    val icon: ImageVector,
    val label: String,
    val destination: AppDestination? = null,
)

private val drawerItems = listOf(
    DrawerItem(Icons.Default.Settings, "Settings", AppDestination.Settings),
    DrawerItem(Icons.Default.Style, "Card Sleeves"),
    DrawerItem(Icons.Default.EmojiEvents, "Leaderboard"),
    DrawerItem(Icons.Default.History, "Match History"),
    DrawerItem(Icons.Default.Help, "How to Play"),
)

@Composable
fun SkyjoDrawerContent(
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(SurfaceWhite)
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        // User header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MintGreen,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "AcePlayer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pro Tier",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen,
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))

        // Menu items
        drawerItems.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { item.destination?.let { onNavigate(it) } }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (item.destination != null) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {}) {
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge,
                color = DangerRed,
                fontWeight = FontWeight.SemiBold,
            )
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navigateAndClose: (AppDestination) -> Unit = { dest ->
        scope.launch { drawerState.close() }
        onNavigate(dest)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContainerColor = SurfaceWhite,
            ) {
                SkyjoDrawerContent(onNavigate = navigateAndClose)
            }
        },
    ) {
        Scaffold(
            containerColor = BackgroundGray,
            topBar = {
                SkyjoTopBar(onMenuClick = { scope.launch { drawerState.open() } })
            },
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
}

// ── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    SkyjoTheme { SkyjoTopBar(onMenuClick = {}) }
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
