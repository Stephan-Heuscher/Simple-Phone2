package ch.heuscher.simplephone.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Application
import androidx.lifecycle.viewModelScope
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.data.SettingsRepository
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ch.heuscher.simplephone.data.MockData

class SimplePhoneViewModel @JvmOverloads constructor(
    application: Application,
    // Allow injection for testing
    private val contactRepository: ContactRepository = ContactRepository(application),
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _missedCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val missedCalls: StateFlow<List<CallLogEntry>> = _missedCalls.asStateFlow()

    private val _displayMode = MutableStateFlow(settingsRepository.displayMode)
    val displayMode: StateFlow<Int> = _displayMode.asStateFlow()

    // Derived state for UI convenience (optional, but good for migration)
    val useHugeText: StateFlow<Boolean> = _displayMode.map { it == SettingsRepository.DISPLAY_MODE_LARGE_TEXT }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, settingsRepository.useHugeText)

    val useHugeContactPicture: StateFlow<Boolean> = _displayMode.map { it == SettingsRepository.DISPLAY_MODE_BIG_PHOTOS }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, settingsRepository.useHugeContactPicture)

    val useGridContactImages: StateFlow<Boolean> = _displayMode.map { it == SettingsRepository.DISPLAY_MODE_GRID }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, settingsRepository.useGridContactImages)

    private val _missedCallsHours = MutableStateFlow(settingsRepository.missedCallsHours)
    val missedCallsHours: StateFlow<Int> = _missedCallsHours.asStateFlow()

    private val _darkModeOption = MutableStateFlow(settingsRepository.darkModeOption)
    val darkModeOption: StateFlow<Int> = _darkModeOption.asStateFlow()

    private val _confirmBeforeCall = MutableStateFlow(settingsRepository.confirmBeforeCall)
    val confirmBeforeCall: StateFlow<Boolean> = _confirmBeforeCall.asStateFlow()

    private val _useHapticFeedback = MutableStateFlow(settingsRepository.useHapticFeedback)
    val useHapticFeedback: StateFlow<Boolean> = _useHapticFeedback.asStateFlow()

    private val _useVoiceAnnouncements = MutableStateFlow(settingsRepository.useVoiceAnnouncements)
    val useVoiceAnnouncements: StateFlow<Boolean> = _useVoiceAnnouncements.asStateFlow()

    private val _isDemoMode = MutableStateFlow(settingsRepository.isDemoMode)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(settingsRepository.onboardingCompleted)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _blockUnknownCallers = MutableStateFlow(settingsRepository.blockUnknownCallers)
    val blockUnknownCallers: StateFlow<Boolean> = _blockUnknownCallers.asStateFlow()

    private val _answerOnSpeakerIfFlat = MutableStateFlow(settingsRepository.answerOnSpeakerIfFlat)
    val answerOnSpeakerIfFlat: StateFlow<Boolean> = _answerOnSpeakerIfFlat.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val loadedContacts = if (_isDemoMode.value) MockData.demoContacts else contactRepository.getContacts()

            val savedOrder = settingsRepository.getFavoritesOrder()
            val orderedContacts = if (savedOrder.isNotEmpty()) {
                loadedContacts.map { contact ->
                    val savedIndex = savedOrder.indexOf(contact.id)
                    if (savedIndex >= 0) contact.copy(sortOrder = savedIndex) else contact.copy(sortOrder = Int.MAX_VALUE)
                }.sortedWith(compareBy({ !it.isFavorite }, { it.sortOrder }, { it.name }))
            } else {
                loadedContacts
            }

            _contacts.value = orderedContacts
            _missedCalls.value = contactRepository.getMissedCallsInLastHours(_missedCallsHours.value)
        }
    }

    fun setDisplayMode(mode: Int) {
        settingsRepository.displayMode = mode
        _displayMode.value = mode
    }

    fun setMissedCallsHours(hours: Int) {
        settingsRepository.missedCallsHours = hours
        _missedCallsHours.value = hours
        loadData() // Reload missed calls
    }

    fun setDarkModeOption(option: Int) {
        settingsRepository.darkModeOption = option
        _darkModeOption.value = option
    }

    fun setConfirmBeforeCall(enabled: Boolean) {
        settingsRepository.confirmBeforeCall = enabled
        _confirmBeforeCall.value = enabled
    }

    fun setUseHapticFeedback(enabled: Boolean) {
        settingsRepository.useHapticFeedback = enabled
        _useHapticFeedback.value = enabled
    }

    fun setUseVoiceAnnouncements(enabled: Boolean) {
        settingsRepository.useVoiceAnnouncements = enabled
        _useVoiceAnnouncements.value = enabled
    }

    fun setDemoMode(enabled: Boolean) {
        settingsRepository.isDemoMode = enabled
        _isDemoMode.value = enabled
        loadData()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        settingsRepository.onboardingCompleted = completed
        _onboardingCompleted.value = completed
    }

    fun setBlockUnknownCallers(enabled: Boolean) {
        settingsRepository.blockUnknownCallers = enabled
        _blockUnknownCallers.value = enabled
    }

    fun setAnswerOnSpeakerIfFlat(enabled: Boolean) {
        settingsRepository.answerOnSpeakerIfFlat = enabled
        _answerOnSpeakerIfFlat.value = enabled
    }

    fun saveFavoritesOrder(contactIds: List<String>) {
        settingsRepository.saveFavoritesOrder(contactIds)
        loadData() // Re-sort contacts
    }
}
