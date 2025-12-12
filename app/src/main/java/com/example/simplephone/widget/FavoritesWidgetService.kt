package com.example.simplephone.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.simplephone.R
import com.example.simplephone.data.ContactRepository
import com.example.simplephone.model.Contact

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesRemoteViewsFactory(this.applicationContext)
    }
}

class FavoritesRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val favorites = mutableListOf<Contact>()
    private val contactRepository = ContactRepository(context)
    private val settingsRepository = com.example.simplephone.data.SettingsRepository(context)

    override fun onCreate() {
        // Initialize data
    }

    override fun onDataSetChanged() {
        favorites.clear()
        // Use real contacts from ContactRepository
        val allContacts = contactRepository.getContacts()
        val favContacts = allContacts.filter { it.isFavorite }
        
        // Apply saved order
        val savedOrder = settingsRepository.getFavoritesOrder()
        val orderedFavorites = if (savedOrder.isNotEmpty()) {
            favContacts.map { contact ->
                val savedIndex = savedOrder.indexOf(contact.id)
                if (savedIndex >= 0) contact.copy(sortOrder = savedIndex) else contact.copy(sortOrder = Int.MAX_VALUE)
            }.sortedBy { it.sortOrder }
        } else {
            favContacts.sortedBy { it.name }
        }
        
        favorites.addAll(orderedFavorites)
    }

    override fun onDestroy() {
        favorites.clear()
    }

    override fun getCount(): Int = favorites.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position == -1 || position >= favorites.size) return RemoteViews(context.packageName, R.layout.widget_item)

        val contact = favorites[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        views.setTextViewText(R.id.widget_item_name, contact.name)
        
        // Load contact photo if available
        if (contact.imageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(contact.imageUri))
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_item_icon, bitmap)
                }
            } catch (e: Exception) {
                // Fallback to default icon
            }
        }
        
        // Set up click intent to call this contact directly
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${contact.number}")
        }
        views.setOnClickFillInIntent(R.id.widget_item_call, callIntent)
        
        // Tapping anywhere on the row should also trigger call
        views.setOnClickFillInIntent(R.id.widget_item_icon, callIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}