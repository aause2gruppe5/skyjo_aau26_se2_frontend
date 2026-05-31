package at.aau.se2.skyjo.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CommonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── PrimaryButton ──────────────────────────────────────────────────────

    @Test
    fun primaryButton_displays_text() {
        composeTestRule.setContent {
            SkyjoTheme {
                PrimaryButton(text = "START GAME", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("START GAME").assertIsDisplayed()
    }

    @Test
    fun primaryButton_triggers_onClick() {
        var clicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                PrimaryButton(text = "CLICK ME", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("CLICK ME").performClick()
        assert(clicked)
    }

    @Test
    fun primaryButton_disabled_does_not_trigger_onClick() {
        var clicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                PrimaryButton(text = "DISABLED", onClick = { clicked = true }, enabled = false)
            }
        }
        composeTestRule.onNodeWithText("DISABLED").assertIsNotEnabled()
        assert(!clicked)
    }

    // ── SecondaryButton ────────────────────────────────────────────────────

    @Test
    fun secondaryButton_displays_text() {
        composeTestRule.setContent {
            SkyjoTheme {
                SecondaryButton(text = "CANCEL", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
    }

    @Test
    fun secondaryButton_triggers_onClick() {
        var clicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                SecondaryButton(text = "TAP", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("TAP").performClick()
        assert(clicked)
    }

    // ── SkyjoCard ─────────────────────────────────────────────────────────

    @Test
    fun skyjoCard_renders_content() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoCard {
                    androidx.compose.material3.Text("Card Content")
                }
            }
        }
        composeTestRule.onNodeWithText("Card Content").assertIsDisplayed()
    }

    // ── SectionTitle ──────────────────────────────────────────────────────

    @Test
    fun sectionTitle_shows_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                SectionTitle(title = "My Title")
            }
        }
        composeTestRule.onNodeWithText("My Title").assertIsDisplayed()
    }

    @Test
    fun sectionTitle_shows_eyebrow_when_provided() {
        composeTestRule.setContent {
            SkyjoTheme {
                SectionTitle(eyebrow = "eyebrow", title = "Main Title")
            }
        }
        composeTestRule.onNodeWithText("EYEBROW").assertIsDisplayed()
        composeTestRule.onNodeWithText("Main Title").assertIsDisplayed()
    }

    @Test
    fun sectionTitle_no_eyebrow_when_null() {
        composeTestRule.setContent {
            SkyjoTheme {
                SectionTitle(eyebrow = null, title = "Solo Title")
            }
        }
        composeTestRule.onNodeWithText("Solo Title").assertIsDisplayed()
    }

    // ── StatChip ──────────────────────────────────────────────────────────

    @Test
    fun statChip_shows_label_and_value() {
        composeTestRule.setContent {
            SkyjoTheme {
                StatChip(label = "Turn", value = "Alice")
            }
        }
        composeTestRule.onNodeWithText("Turn").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    // ── BadgeChip ─────────────────────────────────────────────────────────

    @Test
    fun badgeChip_shows_text() {
        composeTestRule.setContent {
            SkyjoTheme {
                BadgeChip(text = "Status")
            }
        }
        composeTestRule.onNodeWithText("Status").assertIsDisplayed()
    }

    // ── AvatarBadge ───────────────────────────────────────────────────────

    @Test
    fun avatarBadge_shows_initial() {
        composeTestRule.setContent {
            SkyjoTheme {
                AvatarBadge(initial = 'A')
            }
        }
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun avatarBadge_uppercases_initial() {
        composeTestRule.setContent {
            SkyjoTheme {
                AvatarBadge(initial = 'z')
            }
        }
        composeTestRule.onNodeWithText("Z").assertIsDisplayed()
    }

    // ── PlayerRow ─────────────────────────────────────────────────────────

    @Test
    fun playerRow_shows_name() {
        composeTestRule.setContent {
            SkyjoTheme {
                PlayerRow(name = "Alice", status = "Ready")
            }
        }
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun playerRow_shows_status() {
        composeTestRule.setContent {
            SkyjoTheme {
                PlayerRow(name = "Bob", status = "Ready")
            }
        }
        composeTestRule.onNodeWithText("Ready").assertIsDisplayed()
    }

    @Test
    fun playerRow_host_shows_crown() {
        composeTestRule.setContent {
            SkyjoTheme {
                PlayerRow(name = "Alice", status = "Ready", isHost = true)
            }
        }
        composeTestRule.onNodeWithText("Alice  👑").assertIsDisplayed()
    }
}
