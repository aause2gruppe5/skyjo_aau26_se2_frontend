package at.aau.se2.skyjo.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.aau.se2.skyjo.haptic.LocalHaptic
import at.aau.se2.skyjo.ui.components.SkyjoCard
import at.aau.se2.skyjo.ui.components.SkyjoDrawerScaffold
import at.aau.se2.skyjo.ui.components.screenHorizontalPadding
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.MintGreen
import at.aau.se2.skyjo.ui.theme.MutedText
import at.aau.se2.skyjo.ui.theme.PrimaryGreen
import at.aau.se2.skyjo.ui.theme.SkyjoTheme

@Composable
fun SettingsScreen(
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    musicEnabled: Boolean = false,
    soundEnabled: Boolean = true,
    hapticEnabled: Boolean = true,
    onMusicChange: (Boolean) -> Unit = {},
    onSoundChange: (Boolean) -> Unit = {},
    onHapticChange: (Boolean) -> Unit = {},
) {
    SkyjoDrawerScaffold(
        currentDestination = AppDestination.Settings,
        onNavigate = onNavigate,
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = screenHorizontalPadding(), vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // ── Profile Card ─────────────────────────────────────────────
            SkyjoCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MintGreen,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "S",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Skyjo Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Session securely stored",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText,
                            )
                        }
                    }
                }
            }

            // ── Game Preferences ─────────────────────────────────────────
            SkyjoCard {
                Column {
                    Text(
                        text = "Game Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleRow(
                        icon = Icons.Default.VolumeUp,
                        label = "Sound FX",
                        checked = soundEnabled,
                        onCheckedChange = onSoundChange,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsToggleRow(
                        icon = Icons.Default.MusicNote,
                        label = "Music",
                        checked = musicEnabled,
                        onCheckedChange = onMusicChange,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        label = "Haptic Feedback",
                        checked = hapticEnabled,
                        onCheckedChange = onHapticChange,
                    )
                }
            }

            // ── Account ──────────────────────────────────────────────────
            SkyjoCard {
                Column {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link Account",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Session",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── Footer Removed

            Spacer(modifier = Modifier.height(8.dp))
        }
        } // Box
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = PrimaryGreen,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        val haptic = LocalHaptic.current
        Switch(
            modifier = Modifier.testTag("toggle_$label"),
            checked = checked,
            onCheckedChange = {
                haptic?.tick()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    SkyjoTheme {
        SettingsScreen(onNavigate = {})
    }
}
