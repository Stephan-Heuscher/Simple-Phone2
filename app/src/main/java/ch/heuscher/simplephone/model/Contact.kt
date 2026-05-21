package ch.heuscher.simplephone.model

import java.time.LocalDateTime

/**
 * Represents a contact in the phone system, containing identification, contact details, and display preferences.
 *
 * @param id The unique identifier of the contact.
 * @param name The display name of the contact.
 * @param number The primary phone number of the contact.
 * @param isFavorite Whether the contact is marked as a favorite. Defaults to false.
 * @param initial The uppercase starting character of the contact's name, used for display fallback. Defaults to the first character of the name.
 * @param imageUri Optional URI pointing to the contact's photo. Defaults to null.
 * @param sortOrder The order of the contact in the favorites list. Defaults to 0.
 * @param isPrimary Whether this is the primary number among contact numbers. Defaults to false.
 * @param isSuperPrimary Whether this is the default number for the contact. Defaults to false.
 * @param allNumbers A list of all phone numbers associated with this contact. Defaults to a list containing the primary number.
 * @constructor Creates a new [Contact] instance.
 *
 * Example:
 * ```
 * Contact("1", "Amelia", "0123456789", isFavorite = true, sortOrder = 0)
 * ```
 */
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
