package ch.heuscher.simplephone.call

import android.telecom.CallAudioState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CallServiceTest {

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Reset CallService static state
        CallService.isSpeakerManuallySelected = false
        CallService.watchRequestedAudioRoute = null
        CallService.currentAudioState = null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setAudioRoute manual speaker sets isSpeakerManuallySelected to true`() {
        CallService.setAudioRoute(CallAudioState.ROUTE_SPEAKER, isManual = true)
        assertTrue(CallService.isSpeakerManuallySelected)
        assertNull(CallService.watchRequestedAudioRoute)
    }

    @Test
    fun `setAudioRoute manual earpiece sets isSpeakerManuallySelected to false`() {
        CallService.setAudioRoute(CallAudioState.ROUTE_SPEAKER, isManual = true)
        assertTrue(CallService.isSpeakerManuallySelected)

        CallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE, isManual = true)
        assertFalse(CallService.isSpeakerManuallySelected)
    }

    @Test
    fun `setAudioRoute non-manual speaker does not set isSpeakerManuallySelected to true`() {
        CallService.setAudioRoute(CallAudioState.ROUTE_SPEAKER, isManual = false)
        assertFalse(CallService.isSpeakerManuallySelected)
    }

    @Test
    fun `onCallAudioStateChanged resets isSpeakerManuallySelected if route is not ROUTE_SPEAKER`() {
        CallService.isSpeakerManuallySelected = true
        
        val service = mockk<CallService>(relaxed = true)
        every { service.onCallAudioStateChanged(any()) } answers { callOriginal() }
        
        val audioState = mockk<CallAudioState>()
        every { audioState.route } returns CallAudioState.ROUTE_EARPIECE
        every { audioState.supportedRouteMask } returns 0
        
        service.onCallAudioStateChanged(audioState)
        assertFalse(CallService.isSpeakerManuallySelected)
    }

    @Test
    fun `onCallRemoved resets isSpeakerManuallySelected`() {
        CallService.isSpeakerManuallySelected = true
        
        val service = mockk<CallService>(relaxed = true)
        every { service.getSystemService(android.content.Context.TELECOM_SERVICE) } returns mockk<android.telecom.TelecomManager>(relaxed = true)
        every { service.packageName } returns "ch.heuscher.simplephone"
        every { service.onCallRemoved(any()) } answers { callOriginal() }
        
        val call = mockk<android.telecom.Call>(relaxed = true)
        // Mock currentCall
        CallService.currentCall = call
        
        service.onCallRemoved(call)
        assertFalse(CallService.isSpeakerManuallySelected)
    }

    @Test
    fun `updateSpeakerHighlightState switches to earpiece when at ear and speaker not manual or watch selected`() {
        val service = mockk<CallService>(relaxed = true)
        every { service.updateSpeakerHighlightState() } answers { callOriginal() }
        
        val field = CallService::class.java.getDeclaredField("isPhoneAtEar")
        field.isAccessible = true
        field.set(service, true)

        val audioState = mockk<CallAudioState>()
        every { audioState.route } returns CallAudioState.ROUTE_SPEAKER
        CallService.currentAudioState = audioState

        mockkObject(CallService.Companion)
        every { CallService.setAudioRoute(any(), any()) } returns Unit

        service.updateSpeakerHighlightState()

        verify { CallService.setAudioRoute(CallAudioState.ROUTE_EARPIECE) }
    }

    @Test
    fun `updateSpeakerHighlightState does not switch to earpiece when at ear and speaker is manually selected`() {
        val service = mockk<CallService>(relaxed = true)
        every { service.updateSpeakerHighlightState() } answers { callOriginal() }
        
        val field = CallService::class.java.getDeclaredField("isPhoneAtEar")
        field.isAccessible = true
        field.set(service, true)

        val audioState = mockk<CallAudioState>()
        every { audioState.route } returns CallAudioState.ROUTE_SPEAKER
        CallService.currentAudioState = audioState
        CallService.isSpeakerManuallySelected = true

        mockkObject(CallService.Companion)
        every { CallService.setAudioRoute(any(), any()) } returns Unit

        service.updateSpeakerHighlightState()

        verify(exactly = 0) { CallService.setAudioRoute(any(), any()) }
    }

    @Test
    fun `updateSpeakerHighlightState does not switch to earpiece when at ear and watch is manually selected`() {
        val service = mockk<CallService>(relaxed = true)
        every { service.updateSpeakerHighlightState() } answers { callOriginal() }
        
        val field = CallService::class.java.getDeclaredField("isPhoneAtEar")
        field.isAccessible = true
        field.set(service, true)

        val audioState = mockk<CallAudioState>()
        every { audioState.route } returns CallAudioState.ROUTE_SPEAKER
        CallService.currentAudioState = audioState
        CallService.watchRequestedAudioRoute = CallAudioState.ROUTE_BLUETOOTH

        mockkObject(CallService.Companion)
        every { CallService.setAudioRoute(any(), any()) } returns Unit

        service.updateSpeakerHighlightState()

        verify(exactly = 0) { CallService.setAudioRoute(any(), any()) }
    }
}
