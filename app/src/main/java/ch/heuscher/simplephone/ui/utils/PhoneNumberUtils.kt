package ch.heuscher.simplephone.ui.utils

import android.telephony.PhoneNumberUtils
import java.util.Locale

/**
 * centralized logic for phone number operations to ensure consistency across the app.
 */
object PhoneNumberHelper {

    /**
     * Formats a phone number for display.
     * Uses the default locale country code.
     */
    fun format(number: String?): String {
        if (number.isNullOrBlank()) return ""
        val countryCode = Locale.getDefault().country
        return PhoneNumberUtils.formatNumber(number, countryCode) ?: number
    }

    /**
     * Normalizes a phone number for storage or comparison.
     * Removes all non-dialable characters except '+'.
     */
    fun normalize(number: String?): String {
        if (number.isNullOrBlank()) return ""
        return number.replace(Regex("[^0-9+]"), "")
    }

    /**
     * Compares two phone numbers to see if they represent the same contact.
     * Uses Android's PhoneNumberUtils.compare which handles country codes loosely,
     * but also falls back to strict normalized comparison.
     */
    fun areNumbersSame(number1: String?, number2: String?, context: android.content.Context? = null): Boolean {
        if (number1.isNullOrBlank() || number2.isNullOrBlank()) return false
        
        // 1. Exact match of normalized numbers
        val n1 = normalize(number1)
        val n2 = normalize(number2)
        if (n1 == n2) return true
        
        // 2. Loose match using Android's util (handles +1 vs 1, etc.)
        if (context != null) {
            // Context is needed for geolocation-based matching in some versions, 
            // but compare(opt) is deprecated? 
            // PhoneNumberUtils.compare(context, n1, n2) checks against network country
           if (PhoneNumberUtils.compare(context, number1, number2)) return true
        } else {
             if (PhoneNumberUtils.compare(number1, number2)) return true
        }

        // 3. Last resort: Suffix match (last 7 digits) if both are long enough
        // This helps when one format is 079... and other is +4179... and Utils fails
        if (n1.length >= 7 && n2.length >= 7) {
            return n1.endsWith(n2) || n2.endsWith(n1)
        }

        return false
    }
}
