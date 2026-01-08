package ch.heuscher.simplephone.ui

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.data.SettingsRepository
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val contactRepository = ContactRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _missedCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val missedCalls: StateFlow<List<CallLogEntry>> = _missedCalls.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Permission state tracked in VM to avoid loading data before permitted
    private val _hasPermissions = MutableStateFlow(false)

    private val _pendingDialerNumber = MutableStateFlow<String?>(null)
    val pendingDialerNumber: StateFlow<String?> = _pendingDialerNumber.asStateFlow()

    fun setPendingDialerNumber(number: String?) {
        _pendingDialerNumber.value = number
    }

    fun consumePendingDialerNumber() {
        _pendingDialerNumber.value = null
    }

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }

    private var isObserverRegistered = false

    init {
        // Try to register immediately
        registerObservers()
    }

    private fun registerObservers() {
        if (isObserverRegistered) return
        
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contentObserver
            )
            getApplication<Application>().contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                contentObserver
            )
            isObserverRegistered = true
        } catch (e: SecurityException) {
            // Permissions not granted yet. Will retry when permissions are updated.
        } catch (e: Exception) {
            // Log other errors if needed
        }
    }

    fun updatePermissionsState(granted: Boolean) {
        _hasPermissions.value = granted
        if (granted) {
            registerObservers()
            loadData()
        }
    }

    fun loadData() {
        val isDemo = settingsRepository.isDemoMode
        if (!_hasPermissions.value && !isDemo) return

        viewModelScope.launch {
            _isLoading.value = true
            
            // Fix blocking I/O: Run on IO dispatcher
            val loadedContacts = withContext(Dispatchers.IO) {
                val isDemo = settingsRepository.isDemoMode // Accessing shared prefs is fast, but okay to be here
                if (isDemo) {
                    ch.heuscher.simplephone.data.MockData.demoContacts
                } else {
                    contactRepository.getContacts()
                }
            }

            // Apply Sort Order (Computing on background thread is good)
            val sortedContacts = withContext(Dispatchers.Default) {
                val savedOrder = settingsRepository.getFavoritesOrder()
                if (savedOrder.isNotEmpty()) {
                    loadedContacts.map { contact ->
                        val savedIndex = savedOrder.indexOf(contact.id)
                        if (savedIndex >= 0) contact.copy(sortOrder = savedIndex) else contact.copy(sortOrder = Int.MAX_VALUE)
                    }.sortedWith(compareBy({ !it.isFavorite }, { it.sortOrder }, { it.name }))
                } else {
                    loadedContacts
                }
            }
            
            _contacts.value = sortedContacts

            // Load Missed Calls
            // Load Missed Calls (only if permissions granted, or mock them if demo mode extended to calls)
            // For now, only load real calls if permitted, otherwise empty list check happening in repo anyway?
            // Safer to check permission here or let repo handle it?
            // Repo crashes if permisson denied usually.
            
            val loadedMissedCalls = if (_hasPermissions.value) {
                withContext(Dispatchers.IO) {
                    contactRepository.getMissedCallsInLastHours(settingsRepository.missedCallsHours)
                }
            } else {
                emptyList()
            }
            _missedCalls.value = loadedMissedCalls
            
            _isLoading.value = false
        }
    }

    fun onFavoritesReorder(newOrder: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) {
             settingsRepository.saveFavoritesOrder(newOrder.map { it.id })
             // Reload to apply sort
             loadData()
        }
    }
    
    fun toggleDemoMode() {
        val newState = !settingsRepository.isDemoMode
        settingsRepository.isDemoMode = newState
        loadData()
    }
    
    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        if (isObserverRegistered) {
            try {
                getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
