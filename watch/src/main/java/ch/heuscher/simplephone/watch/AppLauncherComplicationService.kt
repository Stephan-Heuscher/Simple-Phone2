package ch.heuscher.simplephone.watch

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class AppLauncherComplicationService : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "AppLauncherComplication"
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        Log.d(TAG, "getPreviewData requested type: $type")
        return createComplicationData(type)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest type: ${request.complicationType}")
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

        val outlineIcon = Icon.createWithBitmap(bitmap)

        val contentDesc = PlainComplicationText.Builder(
            getString(R.string.watch_complication_open)
        ).build()

        Log.d(TAG, "Creating complication data for type: $type")

        return when (type) {
            ComplicationType.PHOTO_IMAGE -> {
                Log.d(TAG, "Returning PHOTO_IMAGE complication")
                PhotoImageComplicationData.Builder(
                    photoImage = outlineIcon,
                    contentDescription = contentDesc
                )
                .setTapAction(pendingIntent)
                .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                Log.d(TAG, "Returning SMALL_IMAGE complication")
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(outlineIcon, SmallImageType.ICON).build(),
                    contentDescription = contentDesc
                )
                .setTapAction(pendingIntent)
                .build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                Log.d(TAG, "Returning MONOCHROMATIC_IMAGE complication")
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = MonochromaticImage.Builder(outlineIcon).build(),
                    contentDescription = contentDesc
                )
                .setTapAction(pendingIntent)
                .build()
            }
            ComplicationType.SHORT_TEXT -> {
                Log.d(TAG, "Returning SHORT_TEXT complication")
                androidx.wear.watchface.complications.data.ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Phone").build(),
                    contentDescription = contentDesc
                )
                .setTapAction(pendingIntent)
                .build()
            }
            else -> {
                Log.w(TAG, "Unsupported type requested: $type")
                null
            }
        }
    }
}
