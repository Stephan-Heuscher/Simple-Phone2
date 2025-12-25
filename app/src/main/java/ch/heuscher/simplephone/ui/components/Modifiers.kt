package ch.heuscher.simplephone.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Custom modifier to handle "press to click" behavior (trigger on ACTION_DOWN)
 * and track press state.
 * 
 * Replaces deprecated pointerInteropFilter for this specific use case.
 */
fun Modifier.pressClickEffect(
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onPressedChange: (Boolean) -> Unit = {}
): Modifier = this.pointerInput(enabled) {
    if (enabled) {
        detectTapGestures(
            onPress = {
                onPressedChange(true)
                onClick()
                try {
                    tryAwaitRelease()
                } finally {
                    onPressedChange(false)
                }
            }
        )
    }
}
