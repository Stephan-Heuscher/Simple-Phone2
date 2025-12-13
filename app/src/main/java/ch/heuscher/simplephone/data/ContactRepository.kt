package ch.heuscher.simplephone.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import ch.heuscher.simplephone.model.Contact

class ContactRepository(private val context: Context) {

    fun getContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver
        
        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoUriIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    val photoUri = it.getString(photoUriIndex)
                    val isStarred = it.getInt(starredIndex) == 1

                    // Filter out contacts without numbers (already handled by querying CommonDataKinds.Phone, but good to be safe)
                    if (!number.isNullOrBlank()) {
                        contacts.add(
                            Contact(
                                id = id,
                                name = name ?: "Unknown",
                                number = number,
                                isFavorite = isStarred,
                                imageUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("ContactRepository", "Permission denied reading contacts", e)
        } catch (e: Exception) {
            android.util.Log.e("ContactRepository", "Error reading contacts", e)
        }
        
        // Remove duplicates if any (same contact multiple numbers) - for simplicity, we might keep them or distinct by ID
        // The user requirement: "only display contacts, if a phone number is present"
        // CommonDataKinds.Phone query only returns contacts with phone numbers.
        
        return contacts.distinctBy { it.id }
    }

    fun getCallLogs(): List<ch.heuscher.simplephone.model.CallLogEntry> {
        val callLogs = mutableListOf<ch.heuscher.simplephone.model.CallLogEntry>()
        val contentResolver: ContentResolver = context.contentResolver
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val cursor: Cursor? = contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            android.provider.CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val typeIndex = it.getColumnIndex(android.provider.CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(android.provider.CallLog.Calls.DATE)
            val idIndex = it.getColumnIndex(android.provider.CallLog.Calls._ID)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                val type = it.getInt(typeIndex)
                val date = it.getLong(dateIndex)
                val id = it.getString(idIndex)

                val callType = when (type) {
                    android.provider.CallLog.Calls.INCOMING_TYPE -> ch.heuscher.simplephone.model.CallType.INCOMING
                    android.provider.CallLog.Calls.OUTGOING_TYPE -> ch.heuscher.simplephone.model.CallType.OUTGOING
                    android.provider.CallLog.Calls.MISSED_TYPE -> ch.heuscher.simplephone.model.CallType.MISSED
                    else -> null
                }

                if (callType != null) {
                    callLogs.add(
                        ch.heuscher.simplephone.model.CallLogEntry(
                            id = id,
                            contactId = number, 
                            timestamp = java.time.Instant.ofEpochMilli(date).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
                            type = callType
                        )
                    )
                }
            }
        }
        return callLogs
    }

    fun getMissedCallsInLastHours(hours: Int): List<ch.heuscher.simplephone.model.CallLogEntry> {
        val allCalls = getCallLogs()
        val cutoffTime = java.time.LocalDateTime.now().minusHours(hours.toLong())
        return allCalls.filter { 
            it.type == ch.heuscher.simplephone.model.CallType.MISSED && it.timestamp.isAfter(cutoffTime)
        }
    }
}
