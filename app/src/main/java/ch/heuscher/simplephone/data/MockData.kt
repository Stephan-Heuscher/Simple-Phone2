package ch.heuscher.simplephone.data

import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.CallType
import ch.heuscher.simplephone.model.Contact
import java.time.LocalDateTime

object MockData {
    val contacts = listOf(
        Contact("1", "Amelia", "0123456789", isFavorite = true, sortOrder = 0),
        Contact("2", "Bob", "0987654321", isFavorite = true, sortOrder = 1),
        Contact("3", "Charlie", "1122334455", isFavorite = false),
        Contact("4", "Doctor Smith", "555-0123", isFavorite = true, sortOrder = 2),
        Contact("5", "Emergency", "911", isFavorite = true, sortOrder = 3),
        Contact("6", "Frank", "4455667788", isFavorite = false),
        Contact("7", "Grandson", "9988776655", isFavorite = true, sortOrder = 4),
    )

    val demoContacts = listOf(
        Contact("d1", "Grandson Tom", "0123456789", isFavorite = true, sortOrder = 0, imageUri = "android.resource://ch.heuscher.simplephone/drawable/demo_tom"),
        Contact("d2", "Martha (Bingo)", "0987654321", isFavorite = true, sortOrder = 1, imageUri = "android.resource://ch.heuscher.simplephone/drawable/demo_martha"),
        Contact("d3", "Dr. Smith", "555-0123", isFavorite = true, sortOrder = 2, imageUri = "android.resource://ch.heuscher.simplephone/drawable/demo_doctor"),
        Contact("d4", "Emergency", "112", isFavorite = true, sortOrder = 3),
        Contact("d5", "Daughter Sarah", "4455667788", isFavorite = true, sortOrder = 4, imageUri = "android.resource://ch.heuscher.simplephone/drawable/demo_sarah"),
        Contact("d6", "Taxi Service", "9988776655", isFavorite = false),
        Contact("d7", "Pharmacy", "1122334455", isFavorite = false),
        Contact("d8", "Neighbor John", "6677889900", isFavorite = false)
    )

    // Mutable list for favorites ordering
    private var _favoritesOrder: MutableList<String> = contacts
        .filter { it.isFavorite }
        .sortedBy { it.sortOrder }
        .map { it.id }
        .toMutableList()

    fun getFavoritesOrdered(): List<Contact> {
        return _favoritesOrder.mapNotNull { id -> contacts.find { it.id == id } }
    }

    fun moveFavoriteUp(contactId: String) {
        val index = _favoritesOrder.indexOf(contactId)
        if (index > 0) {
            _favoritesOrder.removeAt(index)
            _favoritesOrder.add(index - 1, contactId)
        }
    }

    fun moveFavoriteDown(contactId: String) {
        val index = _favoritesOrder.indexOf(contactId)
        if (index >= 0 && index < _favoritesOrder.size - 1) {
            _favoritesOrder.removeAt(index)
            _favoritesOrder.add(index + 1, contactId)
        }
    }

    fun getRecents(): List<CallLogEntry> {
        val now = LocalDateTime.now()
        return listOf(
            CallLogEntry("1", "1", now.minusMinutes(15), CallType.INCOMING), // 15 mins ago
            CallLogEntry("2", "4", now.minusMinutes(45), CallType.OUTGOING), // 45 mins ago
            CallLogEntry("3", "7", now.minusHours(1).minusMinutes(30), CallType.MISSED), // 1h 30m ago
            CallLogEntry("4", "2", now.minusHours(3), CallType.INCOMING), // 3h ago (Subject to filter)
            CallLogEntry("5", "3", now.minusHours(5), CallType.MISSED),   // 5h ago (Subject to filter)
            CallLogEntry("d_missed", "d2", now.minusMinutes(5), CallType.MISSED) // Missed call from Martha (d2)
        )
    }

    fun getLastMissedCall(): CallLogEntry? {
        return getRecents()
            .filter { it.type == CallType.MISSED }
            .maxByOrNull { it.timestamp }
    }

    fun getContactById(id: String): Contact? {
        return contacts.find { it.id == id }
    }
}
