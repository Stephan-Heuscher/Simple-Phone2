package ch.heuscher.simplephone.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout

@Composable
fun VerticalScrollbar(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    width: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
) {
    val firstVisibleItemIndex = listState.firstVisibleItemIndex
    val visibleItemCount = listState.layoutInfo.visibleItemsInfo.size
    val totalItemCount = listState.layoutInfo.totalItemsCount

    if (totalItemCount == 0 || visibleItemCount == 0 || visibleItemCount >= totalItemCount) return

    val scrollbarHeightRatio = visibleItemCount.toFloat() / totalItemCount.toFloat()
    val scrollbarOffsetRatio = firstVisibleItemIndex.toFloat() / totalItemCount.toFloat()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight(scrollbarHeightRatio)
                .fillMaxWidth()
                .padding(top = 0.dp) // We'll use a custom layout or offset, but simple Box with bias is easier
        )
    }
    
    // Simplified approach using bias alignment
    // Since we can't easily set absolute offset in a simple Box without complex calculations involving pixel density,
    // let's use a custom layout approach or a simpler visual approximation.
    
    // Better approach:
    val elementHeight = 1f / totalItemCount
    val scrollbarHeight = maxOf(0.1f, visibleItemCount.toFloat() / totalItemCount)
    val scrollbarOffset = firstVisibleItemIndex.toFloat() / totalItemCount
    
    // We need to map [0, 1-height] range for offset
    
    androidx.compose.ui.layout.Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color, CircleShape)
            )
        },
        modifier = modifier
            .fillMaxHeight()
            .width(width)
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(
                minHeight = 0,
                maxHeight = (constraints.maxHeight * scrollbarHeight).toInt()
            )
        )
        
        val scrollbarY = (constraints.maxHeight * scrollbarOffset).toInt()
        
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(0, scrollbarY)
        }
    }
}

@Composable
fun HorizontalScrollbar(
    modifier: Modifier = Modifier,
    scrollState: androidx.compose.foundation.ScrollState,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
) {
    if (scrollState.maxValue == 0) return

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = constraints.maxWidth.toFloat()
        val viewportWidth = width
        val totalContentWidth = scrollState.maxValue + viewportWidth
        
        val scrollbarWidth = (viewportWidth / totalContentWidth) * width
        val scrollbarOffset = (scrollState.value / totalContentWidth) * width
        
        Box(
            modifier = Modifier
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { scrollbarWidth.toDp() })
                .height(height)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(scrollbarOffset.toInt(), 0)
                    }
                }
                .background(color, CircleShape)
        )
    }
}
