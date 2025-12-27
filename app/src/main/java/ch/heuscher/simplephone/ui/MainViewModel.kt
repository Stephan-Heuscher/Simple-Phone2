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

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }

    init {
        // Observers will be registered when permissions are granted or on resume explicitly if needed.
        // For simplicity contributing to the monolithic cleanup, we register once here 
        // but real loading depends on permissions.
        application.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            contentObserver
        )
        application.contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            contentObserver
        )
    }

    fun updatePermissionsState(granted: Boolean) {
        _hasPermissions.value = granted
        if (granted) {
            loadData()
        }
    }

    fun loadData() {
        if (!_hasPermissions.value) return

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
            val loadedMissedCalls = withContext(Dispatchers.IO) {
                contactRepository.getMissedCallsInLastHours(settingsRepository.missedCallsHours)
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
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}
