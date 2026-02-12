package ch.heuscher.simplephone.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("simple_phone_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        // Mock editor chaining (needed before constructor since loadSettings may trigger reads)
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun `default values are correct`() {
        every { sharedPreferences.getInt("dark_mode_option", 0) } returns 0
        assertEquals(0, settingsRepository.darkModeOption)
    }

    @Test
    fun `saving darkModeOption writes to SharedPreferences`() {
        settingsRepository.darkModeOption = 2
        
        verify { editor.putInt("dark_mode_option", 2) }
        verify { editor.apply() }
    }
    
    @Test
    fun `saving missedCallsHours writes to SharedPreferences`() {
        settingsRepository.missedCallsHours = 48
        
        verify { editor.putInt("missed_calls_hours", 48) }
        verify { editor.apply() }
    }

    @Test
    fun `saving ringtoneSilenceTimeout writes to SharedPreferences`() {
        settingsRepository.ringtoneSilenceTimeout = 30
        
        verify { editor.putInt("ringtone_silence_timeout", 30) }
        verify { editor.apply() }
    }
}
