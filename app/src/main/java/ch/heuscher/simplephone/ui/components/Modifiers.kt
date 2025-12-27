package ch.heuscher.simplephone.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
): Modifier = this.composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnPressedChange by rememberUpdatedState(onPressedChange)
    
    this.pointerInput(enabled) {
        if (enabled) {
            detectTapGestures(
                onPress = {
                    currentOnPressedChange(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        currentOnPressedChange(false)
                    }
                },
                onTap = { currentOnClick() }
            )
        }
    }
}
