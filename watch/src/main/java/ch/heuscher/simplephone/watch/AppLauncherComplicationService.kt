package ch.heuscher.simplephone.watch

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
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
        // Create an Intent to launch our MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Wrap the Intent in a PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The icon we want to show on the watch face
        val icon = Icon.createWithResource(this, R.drawable.ic_call_24)

        return when (type) {
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = androidx.wear.watchface.complications.data.SmallImage.Builder(
                        image = icon,
                        type = SmallImageType.ICON
                    ).build(),
                    contentDescription = PlainComplicationText.Builder("Simple Phone öffnen").build()
                )
                .setTapAction(pendingIntent)
                .build()
            }
            ComplicationType.SHORT_TEXT -> {
                // Some watch faces prefer SHORT_TEXT even for icons, so we provide text + icon
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Telefon").build(),
                    contentDescription = PlainComplicationText.Builder("Simple Phone öffnen").build()
                )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(icon).build()
                )
                .setTapAction(pendingIntent)
                .build()
            }
            // If the watch face asks for a type we don't support, return null
            else -> null
        }
    }
}
