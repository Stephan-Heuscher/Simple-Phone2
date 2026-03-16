package ch.heuscher.simplephone.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import ch.heuscher.simplephone.model.Contact
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.ByteArrayOutputStream

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

                // Attach image asset if available
                if (contact.imageUri != null) {
                    val asset = createAssetFromUri(contact.imageUri)
                    if (asset != null) {
                        map.putAsset("photo", asset)
                    }
                }

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

    private fun createAssetFromUri(uriString: String): Asset? {
        try {
            val uri = Uri.parse(uriString)
            // Use contentResolver to open the contact photo URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Keep image relatively large but not huge to ensure it transmits, but good quality
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                val byteStream = ByteArrayOutputStream()
                // Use high quality PNG instead of JPEG to preserve transparency if any, although JPEG is fine. Let's use PNG.
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                return Asset.createFromBytes(byteStream.toByteArray())
            } else {
                Log.w("WearSyncManager", "BitmapFactory returned null for $uriString")
            }
        } catch (e: Exception) {
            Log.e("WearSyncManager", "Failed to load image for sync: $uriString", e)
        }
        return null
    }
}
