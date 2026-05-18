package ch.heuscher.simplephone.widget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/**
 * Invisible trampoline activity used exclusively by the home-screen widget.
 *
 * When the user taps a favorite on the widget, this activity receives the
 * ACTION_CALL intent, places the call via TelecomManager, and immediately
 * finishes — the user never sees any dialer UI.
 *
 * Uses Theme.Transparent (no window, no animation) so nothing is visible.
 */
class WidgetCallActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val phoneNumber = intent?.data?.schemeSpecificPart

        if (!phoneNumber.isNullOrEmpty() &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED
        ) {
            placeCall(phoneNumber)
        } else if (!phoneNumber.isNullOrEmpty()) {
            // Permission not granted – fall back to opening the main dialer
            // with the number pre-filled so the user can grant permission there.
            val fallback = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.fromParts("tel", phoneNumber, null)
                setClass(this@WidgetCallActivity, ch.heuscher.simplephone.MainActivity::class.java)
            }
            startActivity(fallback)
        }

        finish()
    }

    @Suppress("MissingPermission")
    private fun placeCall(phoneNumber: String) {
        val normalized = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.normalize(phoneNumber)
        val uri = Uri.fromParts("tel", normalized, null)

        // Cancel any missed-call notification for this number
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notifId = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.missedCallNotifId(phoneNumber)
        notificationManager.cancel(notifId)

        try {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            telecomManager.placeCall(uri, null)
        } catch (e: Exception) {
            android.util.Log.e("WidgetCallActivity", "placeCall failed, falling back to ACTION_DIAL", e)
            val fallback = Intent(Intent.ACTION_DIAL).apply {
                data = uri
            }
            startActivity(fallback)
        }
    }
}
