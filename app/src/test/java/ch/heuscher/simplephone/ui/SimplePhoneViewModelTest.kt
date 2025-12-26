package ch.heuscher.simplephone.ui

import androidx.lifecycle.Application
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.data.SettingsRepository
import ch.heuscher.simplephone.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class SimplePhoneViewModelTest {

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var contactRepository: ContactRepository

    @Mock
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var viewModel: SimplePhoneViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Default mocks
        whenever(settingsRepository.useHugeText).thenReturn(false)
        whenever(settingsRepository.useHugeContactPicture).thenReturn(false)
        whenever(settingsRepository.useGridContactImages).thenReturn(false)
        whenever(settingsRepository.missedCallsHours).thenReturn(24)
        whenever(settingsRepository.darkModeOption).thenReturn(0)
        whenever(settingsRepository.confirmBeforeCall).thenReturn(false)
        whenever(settingsRepository.useHapticFeedback).thenReturn(true)
        whenever(settingsRepository.useVoiceAnnouncements).thenReturn(false)
        whenever(settingsRepository.isDemoMode).thenReturn(false)
        whenever(settingsRepository.onboardingCompleted).thenReturn(true)
        whenever(settingsRepository.blockUnknownCallers).thenReturn(false)
        whenever(settingsRepository.answerOnSpeakerIfFlat).thenReturn(false)
        whenever(settingsRepository.getFavoritesOrder()).thenReturn(emptyList())
        whenever(contactRepository.getContacts()).thenReturn(emptyList())
        whenever(contactRepository.getMissedCallsInLastHours(any())).thenReturn(emptyList())

        viewModel = SimplePhoneViewModel(application, contactRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads data`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        verify(contactRepository).getContacts()
        assertEquals(emptyList<Contact>(), viewModel.contacts.value)
    }

    @Test
    fun `setUseHugeText updates state and repository`() = runTest {
        viewModel.setUseHugeText(true)
        assertEquals(true, viewModel.useHugeText.value)
        verify(settingsRepository).useHugeText = true
    }

    @Test
    fun `loadData with contacts updates state`() = runTest {
        val contacts = listOf(
            Contact("1", "Alice", "123", false),
            Contact("2", "Bob", "456", true)
        )
        whenever(contactRepository.getContacts()).thenReturn(contacts)

        viewModel.loadData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(contacts, viewModel.contacts.value)
    }
}
