package ch.heuscher.simplephone.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Determines if the current screen configuration should use a two-pane layout.
 *
 * Uses PHYSICAL pixel width (not dp!) to avoid issues with high accessibility
 * DPI settings. Seniors often increase display size for readability, which
 * reduces screenWidthDp and would prevent the two-pane layout from activating.
 *
 * Criteria: physical width > 1200px AND (landscape OR near-square aspect ratio >= 0.9).
 * - 1200px threshold: Fold 6 inner display is ~2176px wide (easily exceeds)
 *   while folded outer display is ~1080px (correctly excluded)
 *   Normal phones are typically 1080px or less (correctly excluded)
 * - Aspect ratio check prevents false triggers on tall portrait screens
 */
@Composable
fun isTabletLayout(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val physicalWidthPx = displayMetrics.widthPixels
    val physicalHeightPx = displayMetrics.heightPixels

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val aspectRatio = if (physicalHeightPx > 0) {
        physicalWidthPx.toFloat() / physicalHeightPx.toFloat()
    } else 0f

    return physicalWidthPx > 1200 && (isLandscape || aspectRatio >= 0.9f)
}

/**
 * Two-pane adaptive layout container.
 * On tablet/foldable: shows left and right panes side by side (50/50 split).
 * On phone: renders only the singlePaneContent (typically the original stacked layout).
 */
@Composable
fun TwoPaneLayout(
    modifier: Modifier = Modifier,
    leftPane: @Composable (Modifier) -> Unit,
    rightPane: @Composable (Modifier) -> Unit,
    singlePaneContent: @Composable () -> Unit
) {
    if (isTabletLayout()) {
        Row(modifier = modifier.fillMaxSize()) {
            leftPane(Modifier.weight(1f).fillMaxHeight())
            VerticalDivider()
            rightPane(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        singlePaneContent()
    }
}
