package ch.heuscher.simplephone.data

import android.content.ContentProviderOperation
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
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY
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
                val isPrimaryIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                val isSuperPrimaryIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    val photoUri = it.getString(photoUriIndex)
                    val isStarred = it.getInt(starredIndex) == 1
                    val isPrimary = it.getInt(isPrimaryIndex) == 1
                    val isSuperPrimary = it.getInt(isSuperPrimaryIndex) == 1

                    // Filter out contacts without numbers (already handled by querying CommonDataKinds.Phone, but good to be safe)
                    if (!number.isNullOrBlank()) {
                        contacts.add(
                            Contact(
                                id = id,
                                name = name ?: "Unknown",
                                number = number,
                                isFavorite = isStarred,
                                imageUri = photoUri,
                                isPrimary = isPrimary,
                                isSuperPrimary = isSuperPrimary
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
        
        // Remove duplicates: for contacts with multiple numbers, prefer super_primary > primary > first
        val groupedById = contacts.groupBy { it.id }
        val deduplicatedContacts = groupedById.map { (_, contactList) ->
            val primaryContact = contactList.find { it.isSuperPrimary }
                ?: contactList.find { it.isPrimary }
                ?: contactList.first()
            
            // Collect all unique numbers for this contact
            val allNumbers = contactList.map { it.number }.distinct()
            
            primaryContact.copy(allNumbers = allNumbers)
        }
        
        return deduplicatedContacts
    }
    
    fun getContactByNumber(number: String?): Contact? {
        if (number.isNullOrEmpty()) return null
        
        val contentResolver: ContentResolver = context.contentResolver
        val candidates = mutableListOf<Contact>()
        
        try {
            val cursor: Cursor? = contentResolver.query(
                android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number)),
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER,
                    ContactsContract.PhoneLookup.PHOTO_URI,
                    ContactsContract.PhoneLookup.STARRED
                ),
                null,
                null,
                null
            )
            
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                val photoUriIndex = it.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val starredIndex = it.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                
                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val foundNumber = it.getString(numberIndex)
                    val photoUri = it.getString(photoUriIndex)
                    val isStarred = it.getInt(starredIndex) == 1
                    
                    candidates.add(Contact(
                        id = id,
                        name = name ?: "Unknown",
                        number = foundNumber,
                        isFavorite = isStarred,
                        imageUri = photoUri,
                        allNumbers = listOf(foundNumber)
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactRepository", "Error looking up contact by number", e)
        }
        
        // Prioritize: 1. Favorites, 2. Has Picture, 3. Rest
        return candidates.sortedWith(
            compareByDescending<Contact> { it.isFavorite }
                .thenByDescending { it.imageUri != null }
        ).firstOrNull()
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
            val durationIndex = it.getColumnIndex(android.provider.CallLog.Calls.DURATION)
            val idIndex = it.getColumnIndex(android.provider.CallLog.Calls._ID)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                val type = it.getInt(typeIndex)
                val date = it.getLong(dateIndex)
                val duration = it.getLong(durationIndex)
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
                            type = callType,
                            duration = duration
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
        
        // 1. Filter for missed calls within the time window
        val recentMissedCalls = allCalls.filter { 
            it.type == ch.heuscher.simplephone.model.CallType.MISSED && it.timestamp.isAfter(cutoffTime)
        }

        // 2. Filter out missed calls if there was a successful conversation (> 20s) AFTER the missed call
        val filteredMissedCalls = recentMissedCalls.filter { missedCall ->
            val hasSubsequentConversation = allCalls.any { otherCall ->
                // Check if it's the same number
                val sameNumber = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.areNumbersSame(otherCall.contactId, missedCall.contactId, context)
                // Check if it's after the missed call
                val isAfter = otherCall.timestamp.isAfter(missedCall.timestamp)
                // Check if it's a conversation (Incoming or Outgoing) and > 20 seconds
                val isConversation = (otherCall.type == ch.heuscher.simplephone.model.CallType.INCOMING || 
                                     otherCall.type == ch.heuscher.simplephone.model.CallType.OUTGOING)
                val isLongEnough = otherCall.duration > 20
                
                sameNumber && isAfter && isConversation && isLongEnough
            }
            !hasSubsequentConversation
        }

        // 3. Deduplicate by number (keep the most recent one)
        return filteredMissedCalls
            .sortedByDescending { it.timestamp }
            .distinctBy { ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.normalize(it.contactId) }
    }
}

