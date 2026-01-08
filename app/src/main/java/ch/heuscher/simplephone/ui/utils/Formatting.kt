package ch.heuscher.simplephone.ui.utils

import android.telephony.PhoneNumberUtils
import java.util.Locale

fun formatPhoneNumber(number: String): String {
    val countryCode = Locale.getDefault().country
    return PhoneNumberUtils.formatNumber(number, countryCode) ?: number
}
