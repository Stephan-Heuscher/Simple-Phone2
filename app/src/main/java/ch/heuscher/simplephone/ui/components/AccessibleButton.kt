package ch.heuscher.simplephone.ui.components

import androidx.compose.ui.res.stringResource
import ch.heuscher.simplephone.R



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ch.heuscher.simplephone.ui.components.pressClickEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.RedHangup

/**
 * Accessible button that triggers on ACTION_DOWN (press) instead of release.
 * This is crucial for users with motor disabilities (such as tremors, arthritis, or spasticity)
 * who may have difficulty holding, maintaining contact, and releasing a button precisely.
 *
 * @param onClick Callback triggered immediately upon pointer contact/press.
 * @param modifier Modifier applied to the outer container.
 * @param backgroundColor Background color of the button in its default state.
 * @param contentColor Text/Icon color to be displayed inside the button content.
 * @param contentDescription Screen reader announcement text for accessibility.
 * @param content Composable slot representing the button's inner visual layout.
 */

@Composable
fun AccessiblePressButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (isPressed) backgroundColor.copy(alpha = 0.7f) else backgroundColor)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Large circular button specifically optimized for key telephony actions (e.g., initiating or ending calls).
 * Like other accessible buttons, this triggers its callback on press rather than release, ensuring
 * high reliability for users with physical/motor impairment.
 *
 * @param icon The vector asset representing the button's action.
 * @param contentDescription Localized description used by screen readers (TalkBack) to describe the action.
 * @param onClick Callback triggered immediately when the button is pressed down.
 * @param modifier Modifier applied to the button layout.
 * @param backgroundColor Color of the circular background. Defaults to green.
 * @param iconColor Tint color applied to the icon vector.
 * @param size Dimensions of the button. Defaults to a large, senior-friendly size of 96.dp.
 */

@Composable
fun CallActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GreenCall,
    iconColor: Color = Color.White,
    size: Dp = 96.dp
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) backgroundColor.copy(alpha = 0.7f) else backgroundColor)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Pre-styled green action button specifically configured to initiate a phone call.
 * Leverages [CallActionButton] with standard dial icons and size presets.
 *
 * @param onClick Callback executed immediately when the button is pressed down.
 * @param modifier Modifier applied to the button layout.
 * @param size Custom dimension for the button, defaulting to a highly visible 64.dp.
 */
@Composable
fun GreenCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    CallActionButton(
        icon = Icons.Filled.Call,
        contentDescription = stringResource(R.string.cd_call_button),
        onClick = onClick,
        backgroundColor = GreenCall,
        size = size,
        modifier = modifier
    )
}

/**
 * Pre-styled red action button specifically configured to end or reject a phone call.
 * Leverages [CallActionButton] with standard call-end icons and size presets.
 *
 * @param onClick Callback executed immediately when the button is pressed down.
 * @param modifier Modifier applied to the button layout.
 * @param size Custom dimension for the button, defaulting to an extra large 96.dp for easy emergency access.
 */
@Composable
fun RedHangupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    CallActionButton(
        icon = Icons.Filled.CallEnd,
        contentDescription = stringResource(R.string.cd_end_call),
        onClick = onClick,
        backgroundColor = RedHangup,
        size = size,
        modifier = modifier
    )
}
