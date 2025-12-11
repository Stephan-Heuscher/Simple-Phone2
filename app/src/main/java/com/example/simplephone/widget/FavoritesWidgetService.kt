package com.example.simplephone.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.simplephone.R
import com.example.simplephone.data.MockData
import com.example.simplephone.model.Contact

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesRemoteViewsFactory(this.applicationContext)
    }
}

class FavoritesRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val favorites = mutableListOf<Contact>()

    override fun onCreate() {
        // Initialize data
    }

    override fun onDataSetChanged() {
        favorites.clear()
        favorites.addAll(MockData.contacts.filter { it.isFavorite })
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
        
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_name, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}