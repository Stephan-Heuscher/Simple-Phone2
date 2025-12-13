package ch.heuscher.simplephone.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import ch.heuscher.simplephone.R
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.model.Contact
import kotlin.math.absoluteValue

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesRemoteViewsFactory(this.applicationContext)
    }
}

class FavoritesRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val favorites = mutableListOf<Contact>()
    private val contactRepository = ContactRepository(context)
    private val settingsRepository = ch.heuscher.simplephone.data.SettingsRepository(context)

    override fun onCreate() {
        // Initialize data
    }

    override fun onDataSetChanged() {
        android.util.Log.d("FavoritesWidgetService", "onDataSetChanged called")
        favorites.clear()
        // Use real contacts from ContactRepository or demo contacts
        val isDemoMode = settingsRepository.isDemoMode
        val allContacts = if (isDemoMode) {
             ch.heuscher.simplephone.data.MockData.demoContacts
        } else {
             contactRepository.getContacts()
        }
        android.util.Log.d("FavoritesWidgetService", "Loaded ${allContacts.size} contacts (Demo: $isDemoMode)")
        val favContacts = allContacts.filter { it.isFavorite }
        android.util.Log.d("FavoritesWidgetService", "Found ${favContacts.size} favorites")
        
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
        
        // Check if huge text is enabled
        val useHugeText = settingsRepository.useHugeText
        val textSize = if (useHugeText) 32f else 24f
        views.setTextViewTextSize(R.id.widget_item_name, android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
        views.setTextViewText(R.id.widget_item_name, contact.name)
        
        // Load contact photo if available
        var bitmap: Bitmap? = null
        if (contact.imageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(contact.imageUri))
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    bitmap = createRoundedBitmap(originalBitmap)
                    if (originalBitmap != bitmap) {
                        originalBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                // Fallback to initials
            }
        }
        
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_item_icon, bitmap)
        } else {
            // Create initials bitmap with colored background (64dp = ~168px at xxxhdpi)
            val sizePx = (64 * context.resources.displayMetrics.density).toInt()
            val initialsBitmap = createInitialsBitmap(contact.name, sizePx)
            views.setImageViewBitmap(R.id.widget_item_icon, initialsBitmap)
        }
        
        // Set up click intent to call this contact directly
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${contact.number}")
        }
        views.setOnClickFillInIntent(R.id.widget_item_call, callIntent)
        
        // Tapping anywhere on the row should also trigger call
        views.setOnClickFillInIntent(R.id.widget_item_icon, callIntent)
        views.setOnClickFillInIntent(R.id.widget_item_name, callIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
    
    // Avatar colors matching the app theme
    private val avatarColors = listOf(
        0xFF6200EE.toInt(), // Purple
        0xFF03DAC5.toInt(), // Teal
        0xFFFF5722.toInt(), // Deep Orange
        0xFF4CAF50.toInt(), // Green
        0xFF2196F3.toInt(), // Blue
        0xFFE91E63.toInt(), // Pink
        0xFF9C27B0.toInt(), // Purple variant
        0xFF00BCD4.toInt(), // Cyan
        0xFFFF9800.toInt(), // Orange
        0xFF795548.toInt()  // Brown
    )
    
    private fun getColorForName(name: String): Int {
        val index = name.hashCode().absoluteValue % avatarColors.size
        return avatarColors[index]
    }
    
    private fun getInitials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
        }
    }
    
    private fun createRoundedBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Draw rounded rectangle (16dp corner radius scaled to bitmap size)
        // 16dp radius for 64dp size = 25% of size
        val cornerRadius = size * 0.25f
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        
        // Draw bitmap with SRC_IN to clip to rounded rectangle
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        // Center crop the bitmap
        val srcLeft = (bitmap.width - size) / 2
        val srcTop = (bitmap.height - size) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + size, srcTop + size)
        val dstRect = Rect(0, 0, size, size)
        
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        return output
    }
    
    private fun createInitialsBitmap(name: String, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background paint
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = getColorForName(name)
            style = Paint.Style.FILL
        }
        
        // Draw rounded rectangle background (16dp corner radius scaled = 25% of size)
        val cornerRadius = sizePx * 0.25f
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        
        // Text paint
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFFFFFFF.toInt() // White
            textSize = sizePx * 0.4f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        
        // Draw initials centered
        val initials = getInitials(name)
        val textBounds = Rect()
        textPaint.getTextBounds(initials, 0, initials.length, textBounds)
        val yPos = sizePx / 2f - textBounds.exactCenterY()
        
        canvas.drawText(initials, sizePx / 2f, yPos, textPaint)
        
        return bitmap
    }
}
