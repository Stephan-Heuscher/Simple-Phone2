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
 * Criteria: width > 600dp AND (landscape orientation OR near-square aspect ratio >= 0.9).
 * This covers:
 * - Standard tablets in landscape
 * - Samsung Fold 6 inner display (nearly 1:1) in either orientation
 * - Avoids false triggers on tall portrait phones
 */
@Composable
fun isTabletLayout(): Boolean {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val aspectRatio = if (screenHeightDp > 0) {
        screenWidthDp.toFloat() / screenHeightDp.toFloat()
    } else 0f

    return screenWidthDp > 600 && (isLandscape || aspectRatio >= 0.9f)
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
