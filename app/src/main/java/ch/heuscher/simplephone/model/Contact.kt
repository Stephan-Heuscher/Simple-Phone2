package ch.heuscher.simplephone.model

import java.time.LocalDateTime

data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val isFavorite: Boolean = false,
    val initial: Char = name.firstOrNull()?.uppercaseChar() ?: '?',
    val imageUri: String? = null, // Contact photo URI if available
    val sortOrder: Int = 0, // For custom favorites ordering
    val isPrimary: Boolean = false, // IS_PRIMARY flag from contacts
    val isSuperPrimary: Boolean = false, // IS_SUPER_PRIMARY flag from contacts (default number)
    val allNumbers: List<String> = listOf(number) // All phone numbers associated with this contact
) {
    companion object {
        /**
         * Comparator for prioritizing contacts when matching by number.
         * Priority:
         * 1. Favorite
         * 2. Has Photo
         */
        val PRIORITY_COMPARATOR = compareByDescending<Contact> { it.isFavorite }
            .thenByDescending { it.imageUri != null }
            .thenBy { it.sortOrder }
    }
}

data class CallLogEntry(
    val id: String,
    val contactId: String,
    val timestamp: LocalDateTime,
    val type: CallType,
    val duration: Long = 0
)

enum class CallType {
    INCOMING, OUTGOING, MISSED
}

// Audio output options for in-call screen
enum class AudioOutput {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET,
    HEARING_AID
}
