package ch.heuscher.simplephone.call

import android.app.NotificationManager
import ch.heuscher.simplephone.model.Contact
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for the DND ringing decision logic.
 *
 * These tests validate safety-critical behavior: ensuring calls ring when
 * they should (emergency/repeat callers) and stay silent when DND dictates.
 */
class DndRingPolicyTest {

    private lateinit var recentCallers: MutableMap<String, Long>
    private var currentTime = 1000000L

    // Contacts database for tests
    private val knownContacts = mutableMapOf<String, Contact>()

    private fun contactLookup(number: String): Contact? = knownContacts[number]

    private fun createPolicy(blockUnknown: Boolean = false): DndRingPolicy {
        return DndRingPolicy(
            blockUnknownCallers = blockUnknown,
            contactLookup = ::contactLookup,
            recentCallers = recentCallers,
            currentTimeProvider = { currentTime }
        )
    }

    private fun dndOff() = DndRingPolicy.DndState(
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
    )

    private fun dndAlarms() = DndRingPolicy.DndState(
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALARMS
    )

    private fun dndNone() = DndRingPolicy.DndState(
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE
    )

    private fun dndPriority(
        callsEnabled: Boolean = false,
        senders: Int = NotificationManager.Policy.PRIORITY_SENDERS_ANY,
        repeatCallers: Boolean = false
    ) = DndRingPolicy.DndState(
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        priorityCategoryCallsEnabled = callsEnabled,
        priorityCallSenders = senders,
        priorityCategoryRepeatCallersEnabled = repeatCallers
    )

    private fun aContact(
        name: String = "Test",
        number: String = "+1234567890",
        favorite: Boolean = false
    ) = Contact(
        id = name.hashCode().toString(),
        name = name,
        number = number,
        isFavorite = favorite
    )

    @Before
    fun setUp() {
        recentCallers = mutableMapOf()
        knownContacts.clear()
        currentTime = 1000000L
    }

    // ========================================================================
    // DND OFF — should always ring
    // ========================================================================

    @Test
    fun `DND off - known number rings`() {
        val policy = createPolicy()
        val result = policy.shouldRing("+1234567890", dndOff())
        assertTrue(result.shouldRing)
    }

    @Test
    fun `DND off - unknown number rings`() {
        val policy = createPolicy()
        val result = policy.shouldRing("+9999999999", dndOff())
        assertTrue(result.shouldRing)
    }

    @Test
    fun `DND off - null number rings`() {
        val policy = createPolicy()
        val result = policy.shouldRing(null, dndOff())
        assertTrue(result.shouldRing)
    }

    // ========================================================================
    // DND ALARMS / NONE — should never ring
    // ========================================================================

    @Test
    fun `DND alarms mode - blocks calls`() {
        val policy = createPolicy()
        val result = policy.shouldRing("+1234567890", dndAlarms())
        assertFalse(result.shouldRing)
    }

    @Test
    fun `DND none mode - blocks calls`() {
        val policy = createPolicy()
        val result = policy.shouldRing("+1234567890", dndNone())
        assertFalse(result.shouldRing)
    }

    // ========================================================================
    // BLOCK UNKNOWN CALLERS (app-level setting)
    // ========================================================================

    @Test
    fun `block unknown - null number is blocked`() {
        val policy = createPolicy(blockUnknown = true)
        val result = policy.shouldRing(null, dndOff())
        assertFalse(result.shouldRing)
    }

    @Test
    fun `block unknown - empty number is blocked`() {
        val policy = createPolicy(blockUnknown = true)
        val result = policy.shouldRing("", dndOff())
        assertFalse(result.shouldRing)
    }

    @Test
    fun `block unknown - unknown number is blocked`() {
        val policy = createPolicy(blockUnknown = true)
        val result = policy.shouldRing("+9999999999", dndOff())
        assertFalse(result.shouldRing)
    }

    @Test
    fun `block unknown - known contact is allowed`() {
        knownContacts["+1234567890"] = aContact(number = "+1234567890")
        val policy = createPolicy(blockUnknown = true)
        val result = policy.shouldRing("+1234567890", dndOff())
        assertTrue(result.shouldRing)
    }

    @Test
    fun `block unknown disabled - unknown number rings`() {
        val policy = createPolicy(blockUnknown = false)
        val result = policy.shouldRing("+9999999999", dndOff())
        assertTrue(result.shouldRing)
    }

    // ========================================================================
    // DND PRIORITY — Repeat Caller Detection
    // ========================================================================

    @Test
    fun `priority mode - first call does not ring`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)
        val result = policy.shouldRing("+1234567890", state)
        assertFalse(result.shouldRing)
    }

    @Test
    fun `priority mode - repeat caller within 15 min rings`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)

        // First call at t=1000000
        policy.shouldRing("+1234567890", state)

        // Second call 5 minutes later
        currentTime += 5 * 60 * 1000L
        val result = policy.shouldRing("+1234567890", state)
        assertTrue("Repeat caller should ring", result.shouldRing)
    }

    @Test
    fun `priority mode - repeat caller after 15 min does not ring`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)

        // First call
        policy.shouldRing("+1234567890", state)

        // Second call 16 minutes later (exceeds 15 min window)
        currentTime += 16 * 60 * 1000L
        val result = policy.shouldRing("+1234567890", state)
        assertFalse("Caller after 15 min should NOT ring", result.shouldRing)
    }

    @Test
    fun `priority mode - repeat caller at exactly 15 min rings`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)

        // First call
        policy.shouldRing("+1234567890", state)

        // Exactly at 15 min boundary
        currentTime += 15 * 60 * 1000L
        val result = policy.shouldRing("+1234567890", state)
        assertTrue("Caller at exactly 15 min should ring (<=)", result.shouldRing)
    }

    @Test
    fun `priority mode - different numbers are not repeat callers`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)

        policy.shouldRing("+1111111111", state)
        currentTime += 5 * 60 * 1000L
        val result = policy.shouldRing("+2222222222", state)
        assertFalse("Different number should NOT count as repeat", result.shouldRing)
    }

    @Test
    fun `priority mode - null caller number does not crash`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)
        val result = policy.shouldRing(null, state)
        assertFalse(result.shouldRing)
    }

    // ========================================================================
    // DND PRIORITY — Priority Senders
    // ========================================================================

    @Test
    fun `priority ANY senders - allows all calls`() {
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_ANY
        )
        val result = policy.shouldRing("+9999999999", state)
        assertTrue(result.shouldRing)
    }

    @Test
    fun `priority CONTACTS - allows known contact`() {
        knownContacts["+1234567890"] = aContact(number = "+1234567890")
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
        )
        val result = policy.shouldRing("+1234567890", state)
        assertTrue(result.shouldRing)
    }

    @Test
    fun `priority CONTACTS - blocks unknown number`() {
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
        )
        val result = policy.shouldRing("+9999999999", state)
        assertFalse(result.shouldRing)
    }

    @Test
    fun `priority CONTACTS - blocks null number`() {
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
        )
        val result = policy.shouldRing(null, state)
        assertFalse(result.shouldRing)
    }

    @Test
    fun `priority STARRED - allows starred contact`() {
        knownContacts["+1234567890"] = aContact(
            number = "+1234567890",
            favorite = true
        )
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_STARRED
        )
        val result = policy.shouldRing("+1234567890", state)
        assertTrue(result.shouldRing)
    }

    @Test
    fun `priority STARRED - blocks non-starred contact`() {
        knownContacts["+1234567890"] = aContact(
            number = "+1234567890",
            favorite = false
        )
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_STARRED
        )
        val result = policy.shouldRing("+1234567890", state)
        assertFalse(result.shouldRing)
    }

    @Test
    fun `priority STARRED - blocks unknown number`() {
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = true,
            senders = NotificationManager.Policy.PRIORITY_SENDERS_STARRED
        )
        val result = policy.shouldRing("+9999999999", state)
        assertFalse(result.shouldRing)
    }

    // ========================================================================
    // Number Normalization
    // ========================================================================

    @Test
    fun `normalizeNumber strips non-digit characters`() {
        val policy = createPolicy()
        assertEquals("+1234567890", policy.normalizeNumber("+1 (234) 567-890"))
    }

    @Test
    fun `normalizeNumber preserves plus sign`() {
        val policy = createPolicy()
        assertEquals("+41791234567", policy.normalizeNumber("+41 79 123 45 67"))
    }

    @Test
    fun `normalizeNumber returns null for null`() {
        val policy = createPolicy()
        assertNull(policy.normalizeNumber(null))
    }

    @Test
    fun `normalizeNumber returns null for empty after stripping`() {
        val policy = createPolicy()
        assertNull(policy.normalizeNumber("abc"))
    }

    // ========================================================================
    // Edge Cases & Combinations
    // ========================================================================

    @Test
    fun `block unknown + DND priority + repeat caller - blocked by unknown filter`() {
        // blockUnknown takes precedence - even if repeat caller, unknown is blocked
        val policy = createPolicy(blockUnknown = true)
        val state = dndPriority(callsEnabled = false)

        // First call
        policy.shouldRing("+9999999999", state)
        currentTime += 5 * 60 * 1000L

        // Second call (repeat) — but unknown, so blocked by unknown filter first
        val result = policy.shouldRing("+9999999999", state)
        assertFalse("Unknown should be blocked even if repeat", result.shouldRing)
    }

    @Test
    fun `block unknown + DND priority + known contact repeat caller - rings`() {
        knownContacts["+1234567890"] = aContact(number = "+1234567890")
        val policy = createPolicy(blockUnknown = true)
        val state = dndPriority(callsEnabled = false)

        // First call
        policy.shouldRing("+1234567890", state)
        currentTime += 5 * 60 * 1000L

        // Second call (repeat + known) → should ring
        val result = policy.shouldRing("+1234567890", state)
        assertTrue("Known repeat caller should ring", result.shouldRing)
    }

    @Test
    fun `number with special chars matches clean number in contacts`() {
        knownContacts["+1234567890"] = aContact(number = "+1234567890")
        val policy = createPolicy(blockUnknown = true)
        val result = policy.shouldRing("+1 (234) 567-890", dndOff())
        assertTrue("Formatted number should match clean contact", result.shouldRing)
    }

    @Test
    fun `old recent callers are cleaned up`() {
        val policy = createPolicy()
        val state = dndPriority(callsEnabled = false)

        // Add an old entry
        recentCallers["+1111111111"] = currentTime - 20 * 60 * 1000L

        // New call triggers cleanup
        policy.shouldRing("+2222222222", state)

        assertFalse("Old entry should have been cleaned up",
            recentCallers.containsKey("+1111111111"))
    }

    @Test
    fun `repeat callers category set but not a repeat - blocks`() {
        val policy = createPolicy()
        val state = dndPriority(
            callsEnabled = false,
            repeatCallers = true
        )
        val result = policy.shouldRing("+1234567890", state)
        assertFalse(result.shouldRing)
    }
}
