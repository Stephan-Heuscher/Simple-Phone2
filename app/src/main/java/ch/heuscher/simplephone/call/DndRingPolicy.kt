package ch.heuscher.simplephone.call

import android.app.NotificationManager
import ch.heuscher.simplephone.model.Contact

/**
 * Pure decision logic for whether to ring on an incoming call.
 *
 * Extracted from CallService.shouldRingForCall() so it can be unit-tested
 * without Android framework mocking. CallService delegates to this class.
 *
 * User benefit: This logic determines if a call rings — a bug here could
 * cause a missed emergency call or an unwanted ring during Do Not Disturb.
 * Having it fully tested gives confidence in correctness.
 */
class DndRingPolicy(
    private val blockUnknownCallers: Boolean,
    private val contactLookup: (String) -> Contact?,
    private val recentCallers: MutableMap<String, Long> = mutableMapOf(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val REPEAT_CALLER_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
    }

    /**
     * Result of the ringing decision with a reason for debugging / logging.
     */
    data class RingDecision(val shouldRing: Boolean, val reason: String)

    /**
     * Parameters describing the current DND state from the system.
     */
    data class DndState(
        val interruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
        val priorityCategoryCallsEnabled: Boolean = false,
        val priorityCallSenders: Int = NotificationManager.Policy.PRIORITY_SENDERS_ANY,
        val priorityCategoryRepeatCallersEnabled: Boolean = false
    )

    /**
     * Normalize a phone number by stripping everything except digits and '+'.
     */
    fun normalizeNumber(number: String?): String? {
        if (number == null) return null
        val clean = number.replace(Regex("[^0-9+]"), "")
        return clean.ifEmpty { null }
    }

    /**
     * Determine if the phone should ring for the given caller.
     *
     * Decision cascade:
     * 1. If blockUnknownCallers is ON → block hidden/unknown numbers
     * 2. If DND is OFF (FILTER_ALL) → always ring
     * 3. If DND is ALARMS or NONE → never ring
     * 4. If DND is PRIORITY → check repeat caller, then priority senders
     */
    fun shouldRing(callerNumber: String?, dndState: DndState): RingDecision {
        // --- Step 1: Internal "block unknown callers" check ---
        if (blockUnknownCallers) {
            val normalized = normalizeNumber(callerNumber)
            if (normalized == null) {
                return RingDecision(false, "Blocked: empty/hidden number")
            }
            val contact = contactLookup(normalized)
            if (contact == null) {
                return RingDecision(false, "Blocked: unknown number $callerNumber")
            }
        }

        // --- Step 2: DND interruption filter ---
        when (dndState.interruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> {
                return RingDecision(true, "DND off (FILTER_ALL)")
            }
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_NONE -> {
                return RingDecision(false, "DND blocks calls (ALARMS/NONE)")
            }
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                return evaluatePriorityMode(callerNumber, dndState)
            }
            else -> {
                // Unknown filter value — default to ring for safety
                return RingDecision(true, "Unknown filter, defaulting to ring")
            }
        }
    }

    /**
     * Evaluate whether to ring in DND PRIORITY mode.
     */
    private fun evaluatePriorityMode(callerNumber: String?, dndState: DndState): RingDecision {
        // --- Repeat caller check ---
        if (callerNumber != null) {
            val normalized = normalizeNumber(callerNumber) ?: callerNumber
            val now = currentTimeProvider()
            val lastCallTime = recentCallers[normalized]

            if (lastCallTime != null && (now - lastCallTime) <= REPEAT_CALLER_WINDOW_MS) {
                return RingDecision(true, "Repeat caller within 15 min")
            }

            // Record this call for future repeat-caller detection
            recentCallers[normalized] = now

            // Clean up old entries
            val cutoff = now - REPEAT_CALLER_WINDOW_MS
            recentCallers.entries.removeAll { it.value < cutoff }
        }

        // --- Priority call senders check ---
        if (dndState.priorityCategoryCallsEnabled) {
            return when (dndState.priorityCallSenders) {
                NotificationManager.Policy.PRIORITY_SENDERS_ANY -> {
                    RingDecision(true, "Priority: ANY senders allowed")
                }
                NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS -> {
                    val normalized = normalizeNumber(callerNumber)
                    if (normalized == null) {
                        RingDecision(false, "Priority CONTACTS: no number")
                    } else {
                        val contact = contactLookup(normalized)
                        if (contact != null) {
                            RingDecision(true, "Priority CONTACTS: caller is a contact")
                        } else {
                            RingDecision(false, "Priority CONTACTS: caller not in contacts")
                        }
                    }
                }
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED -> {
                    val normalized = normalizeNumber(callerNumber)
                    if (normalized == null) {
                        RingDecision(false, "Priority STARRED: no number")
                    } else {
                        val contact = contactLookup(normalized)
                        if (contact?.isFavorite == true) {
                            RingDecision(true, "Priority STARRED: caller is starred")
                        } else {
                            RingDecision(false, "Priority STARRED: caller not starred")
                        }
                    }
                }
                else -> {
                    RingDecision(false, "Priority: unknown sender policy, blocking")
                }
            }
        }

        // Repeat callers category check (without matching repeat above)
        if (dndState.priorityCategoryRepeatCallersEnabled) {
            return RingDecision(false, "Priority REPEAT_CALLERS: not a repeat caller")
        }

        return RingDecision(false, "Priority mode: default block")
    }
}
