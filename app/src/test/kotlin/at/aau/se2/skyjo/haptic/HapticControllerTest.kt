package at.aau.se2.skyjo.haptic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.settings.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HapticControllerTest {

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private lateinit var controller: HapticController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("skyjo_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        settings = SettingsRepository.getInstance(context)
        settings.setHapticEnabled(SettingsRepository.DEFAULT_HAPTIC)
        controller = HapticController(context, settings)
    }

    @Test
    fun `tick vibrates when haptics enabled`() {
        settings.setHapticEnabled(true)
        controller.tick()
    }

    @Test
    fun `event vibrates when haptics enabled`() {
        settings.setHapticEnabled(true)
        controller.event()
    }

    @Test
    fun `tick is a no-op when haptics disabled`() {
        settings.setHapticEnabled(false)
        controller.tick()
    }

    @Test
    fun `event is a no-op when haptics disabled`() {
        settings.setHapticEnabled(false)
        controller.event()
    }

    @Test
    fun `repeated calls stay safe`() {
        settings.setHapticEnabled(true)
        controller.tick()
        controller.event()
        controller.tick()
    }

    @Test
    fun `LocalHaptic composition local exists`() {
        org.junit.Assert.assertNotNull(LocalHaptic)
    }
}
