package ch.heuscher.simplephone.watch

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
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
import java.time.format.DateTimeFormatter

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
                        // Launch the Simple Phone App on Tap
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

        override suspend fun createSharedAssets(): SimpleSharedAssets = SimpleSharedAssets()

        private val backgroundPaint = Paint().apply {
            color = Color.BLACK
        }

        private val favoritesPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        
        private val sosPaint = Paint().apply {
            color = Color.RED
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        override fun render(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            sharedAssets: SimpleSharedAssets
        ) {
            canvas.drawColor(Color.BLACK)

            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            
            val offset = 60f

            // Draw "Favoriten" and "Notruf" directly on the watch face
            canvas.drawText("Favoriten", centerX, centerY - offset / 2, favoritesPaint)
            
            if (renderParameters.drawMode == androidx.wear.watchface.DrawMode.INTERACTIVE) {
                 canvas.drawText("Notruf", centerX, centerY + offset, sosPaint)
            } else {
                 canvas.drawText("Notruf", centerX, centerY + offset, favoritesPaint) // less bright in ambient
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
