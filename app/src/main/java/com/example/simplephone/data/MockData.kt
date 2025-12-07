package com.example.simplephone.data

import com.example.simplephone.model.CallLogEntry
import com.example.simplephone.model.CallType
import com.example.simplephone.model.Contact
import java.time.LocalDateTime

object MockData {
    val contacts = listOf(
        Contact("1", "Amelia", "0123456789", isFavorite = true),
        Contact("2", "Bob", "0987654321", isFavorite = true),
        Contact("3", "Charlie", "1122334455", isFavorite = false),
        Contact("4", "Doctor Smith", "555-0123", isFavorite = true),
        Contact("5", "Emergency", "911", isFavorite = true),
        Contact("6", "Frank", "4455667788", isFavorite = false),
        Contact("7", "Grandson", "9988776655", isFavorite = true),
    )

    fun getRecents(): List<CallLogEntry> {
        val now = LocalDateTime.now()
        return listOf(
            CallLogEntry("1", "1", now.minusMinutes(15), CallType.INCOMING), // 15 mins ago
            CallLogEntry("2", "4", now.minusMinutes(45), CallType.OUTGOING), // 45 mins ago
            CallLogEntry("3", "7", now.minusHours(1).minusMinutes(30), CallType.MISSED), // 1h 30m ago
            CallLogEntry("4", "2", now.minusHours(3), CallType.INCOMING), // 3h ago (Subject to filter)
            CallLogEntry("5", "3", now.minusHours(5), CallType.MISSED)   // 5h ago (Subject to filter)
        )
    }

    fun getContactById(id: String): Contact? {
        return contacts.find { it.id == id }
    }
}
