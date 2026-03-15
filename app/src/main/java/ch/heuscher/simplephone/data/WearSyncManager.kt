package ch.heuscher.simplephone.data

import android.content.Context
import android.util.Log
import ch.heuscher.simplephone.model.Contact
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class WearSyncManager(private val context: Context) {
    
    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun syncContacts(favorites: List<Contact>) {
        try {
            val putDataMapReq = PutDataMapRequest.create("/contacts")
            
            // Build a list of DataMaps for each contact
            val dataMapList = favorites.map { contact ->
                val map = DataMap()
                map.putString("id", contact.id)
                map.putString("name", contact.name)
                map.putString("number", contact.number)
                map
            }
            
            // Put the list into the main DataMap
            putDataMapReq.dataMap.putDataMapArrayList("favorites", ArrayList(dataMapList))
            
            // Add a timestamp to ensure the data item changes and triggers an update
            putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())

            val putDataReq = putDataMapReq.asPutDataRequest()
            putDataReq.setUrgent()

            dataClient.putDataItem(putDataReq)
                .addOnSuccessListener {
                    Log.d("WearSyncManager", "Successfully synced ${favorites.size} contacts to wear")
                }
                .addOnFailureListener { e ->
                    Log.e("WearSyncManager", "Failed to sync contacts to wear", e)
                }
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Error in syncContacts", e)
        }
    }
}
