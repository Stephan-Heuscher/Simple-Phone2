package ch.heuscher.simplephone.watch

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class AppLauncherComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(type)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        return createComplicationData(request.complicationType)
    }

    private fun createComplicationData(type: ComplicationType): ComplicationData? {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_complication_outline)
        val bitmap = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)

        // Use a bitmap-backed icon since some watchfaces reject vector PHOTO_IMAGEs
        val outlineIcon = Icon.createWithBitmap(bitmap)

        val contentDesc = PlainComplicationText.Builder(
            getString(R.string.watch_complication_open)
        ).build()

        return when (type) {
            ComplicationType.PHOTO_IMAGE -> {
                PhotoImageComplicationData.Builder(
                    photoImage = outlineIcon,
                    contentDescription = contentDesc
                )
                .setTapAction(pendingIntent)
                .build()
            }
            ComplicationType.SHORT_TEXT -> {
                androidx.wear.watchface.complications.data.ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Phone").build(),
                    contentDescription = contentDesc
                )
                .setTapAction(pendingIntent)
                .build()
            }
            else -> null
        }
    }
}
