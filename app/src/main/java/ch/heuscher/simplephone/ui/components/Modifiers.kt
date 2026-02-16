package ch.heuscher.simplephone.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Custom modifier to handle "press to click" behavior (trigger on ACTION_DOWN)
 * and track press state.
 * 
 * Replaces deprecated pointerInteropFilter for this specific use case.
 * Refactored to use standard clickable for better reliability and accessibility.
 */
fun Modifier.pressClickEffect(
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onPressedChange: (Boolean) -> Unit = {}
): Modifier = this.composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(isPressed) {
        onPressedChange(isPressed)
    }

    this.clickable(
        interactionSource = interactionSource,
        indication = null, // Visual feedback is handled by the caller
        enabled = enabled,
        onClick = onClick
    )
}
