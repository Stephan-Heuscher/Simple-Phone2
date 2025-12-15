package ch.heuscher.simplephone.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import ch.heuscher.simplephone.model.CallLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Repository to fetch call logs
 */
class CallLogRepository(private val context: Context) {

    suspend fun getAllCallLogs(): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val callLogs = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME
        )

        // Permission check should be done by the caller or UI
        try {
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                
                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val number = it.getString(numberIndex)
                    val typeInt = it.getInt(typeIndex)
                    val dateMillis = it.getLong(dateIndex)
                    val duration = it.getLong(durationIndex)
                    
                    val type = when (typeInt) {
                        CallLog.Calls.MISSED_TYPE -> ch.heuscher.simplephone.model.CallType.MISSED
                        CallLog.Calls.OUTGOING_TYPE -> ch.heuscher.simplephone.model.CallType.OUTGOING
                        else -> ch.heuscher.simplephone.model.CallType.INCOMING
                    }
                    
                    val timestamp = java.time.Instant.ofEpochMilli(dateMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()

                   callLogs.add(
                        CallLogEntry(
                            id = id,
                            contactId = number ?: "Unknown",
                            timestamp = timestamp,
                            type = type,
                            duration = duration
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        
        return@withContext callLogs
    }
}
