package ch.heuscher.simplephone.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import ch.heuscher.simplephone.R

class FavoritesWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("FavoritesWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            android.util.Log.d("FavoritesWidget", "Updating widget $appWidgetId")
            val intent = Intent(context, FavoritesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_favorites).apply {
                setRemoteAdapter(R.id.widget_list, intent)
                setEmptyView(R.id.widget_list, R.id.empty_view)
            }
            
            // Set up pending intent template for call actions.
            // Explicitly target MainActivity so the ACTION_CALL routes deterministically
            // to our dialer instead of relying on (Android 14+ restricted) implicit
            // intent resolution, which could open the app without placing the call.
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                setClass(context, ch.heuscher.simplephone.MainActivity::class.java)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

            val callPendingIntent = PendingIntent.getActivity(
                context,
                0,
                callIntent,
                flags
            )
            views.setPendingIntentTemplate(R.id.widget_list, callPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun sendRefreshBroadcast(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, FavoritesWidget::class.java)
            )
            
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            intent.component = android.content.ComponentName(context, FavoritesWidget::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
            
            // Also notify the collection view to refresh
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        }
    }
}
