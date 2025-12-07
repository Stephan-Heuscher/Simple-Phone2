package com.example.simplephone.model

import java.time.LocalDateTime

data class Contact(
    val id: String,
    val name: String,
    val number: String,
    val isFavorite: Boolean = false,
    val initial: Char = name.firstOrNull()?.uppercaseChar() ?: '?'
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
