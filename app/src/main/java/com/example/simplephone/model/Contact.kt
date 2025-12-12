package com.example.simplephone.model

import java.time.LocalDateTime

data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val isFavorite: Boolean = false,
    val initial: Char = name.firstOrNull()?.uppercaseChar() ?: '?',
    val imageUri: String? = null, // Contact photo URI if available
    val sortOrder: Int = 0 // For custom favorites ordering
)

data class CallLogEntry(
    val id: String,
    val contactId: String,
    val timestamp: LocalDateTime,
    val type: CallType
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
