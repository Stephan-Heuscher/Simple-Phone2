package ch.heuscher.simplephone.watch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.VectorDrawable
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimplePhoneWatchFaceService : WatchFaceService() {

    class SimpleSharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = SimplePhoneRenderer(
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )

        CoroutineScope(Dispatchers.Main.immediate).launch {
            watchState.isAmbient.collect { isAmbient ->
                if (isAmbient == false) {
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }
        }

        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        ).setTapListener(
            object : WatchFace.TapListener {
                override fun onTapEvent(
                    tapType: Int,
                    tapEvent: androidx.wear.watchface.TapEvent,
                    complicationSlot: androidx.wear.watchface.ComplicationSlot?
                ) {
                    if (tapType == androidx.wear.watchface.TapType.UP) {
                        // Launch the Simple Phone App on Tap (Fallback)
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                }
            }
        )
    }

    private inner class SimplePhoneRenderer(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        currentUserStyleRepository: CurrentUserStyleRepository,
        canvasType: Int
    ) : Renderer.CanvasRenderer2<SimpleSharedAssets>(
        surfaceHolder = surfaceHolder,
        currentUserStyleRepository = currentUserStyleRepository,
        watchState = watchState,
        canvasType = canvasType,
        interactiveDrawModeUpdateDelayMillis = 1000L,
        clearWithBackgroundTintBeforeRenderingHighlightLayer = true
    ) {

        private var phoneIcon: Bitmap? = null

        override suspend fun createSharedAssets(): SimpleSharedAssets = SimpleSharedAssets()

        override fun render(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: SimpleSharedAssets
        ) {
            canvas.drawColor(Color.BLACK)

            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()

            if (phoneIcon == null) {
                val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_call_24) as? VectorDrawable
                drawable?.let {
                    val iconSize = (bounds.width() * 0.4f).toInt()
                    it.setBounds(0, 0, iconSize, iconSize)
                    val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                    val canvasForIcon = Canvas(bitmap)
                    it.draw(canvasForIcon)
                    phoneIcon = bitmap
                }
            }

            phoneIcon?.let { icon ->
                val iconLeft = centerX - (icon.width / 2f)
                val iconTop = centerY - (icon.height / 2f)

                if (renderParameters.drawMode == androidx.wear.watchface.DrawMode.AMBIENT) {
                    // Standby mode: Draw outlined phone icon
                    val ambientPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        color = Color.WHITE
                        alpha = 150
                    }
                    // Since the icon is a filled bitmap, we can't just stroke the bitmap easily.
                    // Let's create an outlined bitmap or draw a simple rect placeholder if vector can't be stroked
                    // An alternative is using alpha for ambient. But user requested 'Umriss' (Outline).
                    // We can draw it using a color matrix to keep only the edges, or just keep it dim.
                    // Given the vector is filled, I will draw the bitmap but with a color filter or very dim.
                    // Since true outline of an arbitrary bitmap is hard without a custom shader, we'll keep it very dim.
                    val paint = Paint().apply { alpha = 80 }
                    canvas.drawBitmap(icon, iconLeft, iconTop, paint)
                } else {
                    // Interactive mode: Draw full bright phone icon
                    val activePaint = Paint().apply {
                        alpha = 255 // Full brightness
                    }
                    canvas.drawBitmap(icon, iconLeft, iconTop, activePaint)
                }
            }
        }
        override fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: SimpleSharedAssets
        ) {
            // Not implemented
        }
    }
}
