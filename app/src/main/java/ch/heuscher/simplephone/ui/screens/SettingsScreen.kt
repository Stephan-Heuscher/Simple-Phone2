package ch.heuscher.simplephone.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import ch.heuscher.simplephone.R


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.components.pressClickEffect
import ch.heuscher.simplephone.ui.theme.AlertRed
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.HighContrastBlue

@Composable
fun SettingsScreen(
    displayMode: Int = 0, // 0=Standard, 1=LargeText, 2=BigPhotos, 3=Grid
    onDisplayModeChange: (Int) -> Unit = {},
    missedCallsHours: Int = 24,
    onMissedCallsHoursChange: (Int) -> Unit = {},
    darkModeOption: Int = 0, // 0=System, 1=Light, 2=Dark
    onDarkModeOptionChange: (Int) -> Unit = {},
    blockUnknownCallers: Boolean = false,
    onBlockUnknownCallersChange: (Boolean) -> Unit = {},
    lastBlockedNumber: String? = null,

    confirmBeforeCall: Boolean = false,
    onConfirmBeforeCallChange: (Boolean) -> Unit = {},
    useHapticFeedback: Boolean = true,
    onHapticFeedbackChange: (Boolean) -> Unit = {},
    useVoiceAnnouncements: Boolean = false,
    onVoiceAnnouncementsChange: (Boolean) -> Unit = {},
    favorites: List<Contact> = emptyList(),
    onFavoritesReorder: (List<Contact>) -> Unit = {},
    isDefaultDialer: Boolean = false,
    onSetDefaultDialer: () -> Unit = {},
    onBackClick: () -> Unit = {},
    // New parameters for zoom
    currentZoomFactor: Float = 1.0f,
    onZoomChange: (Float) -> Unit = {},
    currentWidthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass = androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact,
    onShowOnboarding: () -> Unit = {},
    simplifiedContactCallScreen: Boolean = false,
    onSimplifiedContactCallScreenChange: (Boolean) -> Unit = {},
    silenceCallOnTouch: Boolean = false,
    onSilenceCallOnTouchChange: (Boolean) -> Unit = {},
    ringtoneSilenceTimeout: Int = 0,
    onRingtoneSilenceTimeoutChange: (Int) -> Unit = {},
    // Gentle Phone Specific
    pairingCode: String? = null,
    showPairingCode: Boolean = false
) {
    // Remember favorites order for reordering
    val mutableFavorites = remember { mutableStateListOf<Contact>() }
    var activeDraggingId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(favorites) {
        if (activeDraggingId == null) {
            mutableFavorites.clear()
            mutableFavorites.addAll(favorites)
        }
    }

    val context = LocalContext.current
    fun vibrate(force: Boolean = false) {
        if (useHapticFeedback || force) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
    }

    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll logic
    var draggingPointerY by remember { mutableFloatStateOf(0f) }
    var listTopY by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(activeDraggingId) {
        if (activeDraggingId != null) {
            while (activeDraggingId != null) {
                val viewportHeight = listState.layoutInfo.viewportSize.height
                // Relative to LazyColumn top
                val relativeY = draggingPointerY - listTopY
                val topThreshold = viewportHeight * 0.15f
                val bottomThreshold = viewportHeight * 0.85f
                
                if (relativeY < topThreshold && listState.firstVisibleItemIndex > 0 || 
                    (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 0)) {
                    listState.scrollBy(-20f)
                } else if (relativeY > bottomThreshold) {
                    listState.scrollBy(20f)
                }
                delay(10)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .onGloballyPositioned { coords ->
                listTopY = coords.localToRoot(Offset.Zero).y
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (activeDraggingId != null) {
                            val change = event.changes.first()
                            draggingPointerY = change.position.y
                        }
                    }
                }
            }
    ) {
        // --- Pairing Code Section (Gentle Phone Only) ---
        if (showPairingCode && pairingCode != null) {
            item {
                Text(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.gentle_phone_pairing_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.gentle_phone_pairing_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val context = LocalContext.current
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(pairingCode))
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                // Android 13+ automatically shows a toast for clipboard copy
                                android.widget.Toast.makeText(context, R.string.toast_copied_to_clipboard, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pairingCode,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                           text = stringResource(R.string.click_to_copy),
                           style = MaterialTheme.typography.labelMedium,
                           color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Share Button
                    androidx.compose.material3.IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_pairing_code_message, pairingCode))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_pairing_code_title)))
                        },
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_pairing_code_title),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                     Text(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.gentle_phone_web_portal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.gentle_phone_web_portal_url),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 24.dp))
            }
        }

        // --- Zoom Factor Section (First Item) ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_zoom_factor),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Show which screen type we are editing
            val screenTypeLabel = when(currentWidthSizeClass) {
                androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact -> 
                    stringResource(R.string.zoom_screen_compact)
                androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> 
                    stringResource(R.string.zoom_screen_medium)
                androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded -> 
                    stringResource(R.string.zoom_screen_expanded)
                else -> ""
            }
            
            Text(
                text = stringResource(R.string.zoom_factor_current_screen, screenTypeLabel),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                BigIconButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_zoom),
                    onClick = { 
                        vibrate()
                        val newZoom = (currentZoomFactor - 0.05f).coerceAtLeast(0.5f)
                        onZoomChange(newZoom)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Allow horizontal scroll for the percentage text when it gets too big
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.zoom_value_format, (currentZoomFactor * 100).toInt()),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                BigIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_increase_zoom),
                    onClick = { 
                        vibrate()
                        val newZoom = (currentZoomFactor + 0.05f).coerceAtMost(2.0f)
                        onZoomChange(newZoom)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 24.dp))
        }

        item {
            // Highlight the button when zoom is not at default (100%)
            val isZoomNonDefault = currentZoomFactor != 1.0f
            
            // Get current density to reset it for the button text
            val currentDensity = LocalDensity.current
            val baseDensity = remember(currentDensity, currentZoomFactor) {
                androidx.compose.ui.unit.Density(
                    density = currentDensity.density / currentZoomFactor,
                    fontScale = currentDensity.fontScale / currentZoomFactor
                )
            }
            
            Button(
                onClick = {
                    vibrate()
                    onZoomChange(1.0f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isZoomNonDefault) AlertRed else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isZoomNonDefault) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                // Use CompositionLocalProvider to reset density so text stays constant size
                androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides baseDensity) {
                    Text(
                        text = stringResource(R.string.reset_zoom),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Default Dialer Section ---
        if (!isDefaultDialer) {
            item {
                Text(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.default_phone_app),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.set_default_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 32.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SettingsButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.set_default_btn),
                        onClick = { vibrate(); onSetDefaultDialer() }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
            }
        }

        // --- Display Mode Section (unified layout selector) ---
        item {
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_appearance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_appearance_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // 2x2 Grid of Display Mode Options (for accessibility - bigger touch targets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Standard + Large Text
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_standard),
                        isSelected = displayMode == 0,
                        onClick = { vibrate(); onDisplayModeChange(0) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_large_text),
                        isSelected = displayMode == 1,
                        onClick = { vibrate(); onDisplayModeChange(1) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Row 2: Big Photos + Grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_big_photos),
                        isSelected = displayMode == 2,
                        onClick = { vibrate(); onDisplayModeChange(2) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_grid),
                        isSelected = displayMode == 3,
                        onClick = { vibrate(); onDisplayModeChange(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }


        // --- Missed Calls Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.missed_calls_display),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.missed_calls_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                BigIconButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_hours),
                    onClick = { vibrate(); if (missedCallsHours > 1) onMissedCallsHoursChange(missedCallsHours - 1) },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.hours_format, missedCallsHours),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BigIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_increase_hours),
                    onClick = { vibrate(); onMissedCallsHoursChange(missedCallsHours + 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Favorites Order Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.favorites_order),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.favorites_order_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        itemsIndexed(mutableFavorites, key = { _, contact -> contact.id }) { index, contact ->
            var isDragging by remember(contact.id) { mutableStateOf(false) }
            var offsetY by remember(contact.id) { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = offsetY
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                    }
                    .onGloballyPositioned { coordinates ->
                        if (itemHeightPx == 0f) itemHeightPx = coordinates.size.height.toFloat()
                    }
            ) {
                FavoriteReorderRow(
                    contact = contact,
                    canMoveUp = index > 0,
                    canMoveDown = index < mutableFavorites.lastIndex,
                    useHugeText = displayMode == 1,
                    onMoveUp = {
                        vibrate()
                        if (index > 0) {
                            val item = mutableFavorites.removeAt(index)
                            mutableFavorites.add(index - 1, item)
                            onFavoritesReorder(mutableFavorites.toList())
                        }
                    },
                    onMoveDown = {
                        vibrate()
                        if (index < mutableFavorites.lastIndex) {
                            val item = mutableFavorites.removeAt(index)
                            mutableFavorites.add(index + 1, item)
                            onFavoritesReorder(mutableFavorites.toList())
                        }
                    },
                    itemHeightPx = itemHeightPx,
                    isDragging = isDragging,
                    onDraggingChange = { dragging ->
                        isDragging = dragging
                        activeDraggingId = if (dragging) contact.id else null
                    },
                    offsetY = offsetY,
                    onOffsetYChange = { offsetY = it },
                    vibrateFunc = { force -> vibrate(force) }
                )
            }
            if (index < mutableFavorites.size - 1) {
                HorizontalDivider()
            }
        }

        // --- Advanced Options Header ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.advanced_settings),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Block Unknown Numbers Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.block_unknown),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            val description = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.block_unknown_desc)
            val fullDescription = if (lastBlockedNumber != null) {
                "$description\n${androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_last_blocked, lastBlockedNumber)}"
            } else {
                description
            }
            
            Text(
                text = fullDescription,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BigToggleButton(
                    isEnabled = blockUnknownCallers,
                    onToggle = { vibrate(); onBlockUnknownCallersChange(!blockUnknownCallers) },
                    label = if (blockUnknownCallers) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Dark Mode Section (Moved to Advanced) ---
        item {
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.dark_mode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.dark_mode_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System Default
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_system),
                        isSelected = darkModeOption == 0,
                        onClick = { vibrate(); onDarkModeOptionChange(0) },
                        modifier = Modifier.weight(1f)
                    )

                    // Light
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_light),
                        isSelected = darkModeOption == 1,
                        onClick = { vibrate(); onDarkModeOptionChange(1) },
                        modifier = Modifier.weight(1f)
                    )

                    // Dark
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_dark),
                        isSelected = darkModeOption == 2,
                        onClick = { vibrate(); onDarkModeOptionChange(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Ringtone Silence timeout Section ---
        item {
            val timeoutSteps = listOf(0, 1, 2, 3, 5, 8, 10, 15)
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_ringtone_silence),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_ringtone_silence_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                BigIconButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_seconds),
                    onClick = { 
                        vibrate()
                        val currentIndex = timeoutSteps.indexOf(ringtoneSilenceTimeout).coerceAtLeast(0)
                        if (currentIndex > 0) {
                            onRingtoneSilenceTimeoutChange(timeoutSteps[currentIndex - 1])
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    if (ringtoneSilenceTimeout == 0) stringResource(R.string.off_label) else stringResource(R.string.seconds_format, ringtoneSilenceTimeout),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BigIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_increase_seconds),
                    onClick = { 
                        vibrate()
                        val currentIndex = timeoutSteps.indexOf(ringtoneSilenceTimeout)
                        if (currentIndex < timeoutSteps.lastIndex) {
                            if (currentIndex == -1) {
                                // If current value is not in steps, find first step > current
                                val nextStep = timeoutSteps.firstOrNull { it > ringtoneSilenceTimeout } ?: timeoutSteps.last()
                                onRingtoneSilenceTimeoutChange(nextStep)
                            } else {
                                onRingtoneSilenceTimeoutChange(timeoutSteps[currentIndex + 1])
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }



        // --- Simplified Contact Call Screen ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_simplified_contact_screen),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_simplified_contact_screen_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BigToggleButton(
                    isEnabled = simplifiedContactCallScreen,
                    onToggle = { vibrate(); onSimplifiedContactCallScreenChange(!simplifiedContactCallScreen) },
                    label = if (simplifiedContactCallScreen) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Silence on Touch Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_silence_on_touch_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_silence_on_touch_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BigToggleButton(
                    isEnabled = silenceCallOnTouch,
                    onToggle = { vibrate(); onSilenceCallOnTouchChange(!silenceCallOnTouch) },
                    label = if (silenceCallOnTouch) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }



        // --- Call Confirmation Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_confirm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_confirm_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BigToggleButton(
                    isEnabled = confirmBeforeCall,
                    onToggle = { vibrate(); onConfirmBeforeCallChange(!confirmBeforeCall) },
                    label = if (confirmBeforeCall) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Voice Announcements Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.voice_announcements),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.voice_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BigToggleButton(
                    isEnabled = useVoiceAnnouncements,
                    onToggle = { vibrate(); onVoiceAnnouncementsChange(!useVoiceAnnouncements) },
                    label = if (useVoiceAnnouncements) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Haptic Feedback Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.vibration_feedback),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.vibration_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BigToggleButton(
                    isEnabled = useHapticFeedback,
                    onToggle = {
                        vibrate(force = true)
                        onHapticFeedbackChange(!useHapticFeedback)
                    },
                    label = if (useHapticFeedback) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
        }
        


        // --- About & Legal Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.about_legal),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Privacy Policy
                SettingsButton(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.privacy_policy),
                    onClick = {
                        vibrate()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://raw.githubusercontent.com/Stephan-Heuscher/Simple-Phone2/master/PRIVACY_POLICY.md"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Terms of Service
                SettingsButton(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.terms_of_service),
                    onClick = {
                        vibrate()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://raw.githubusercontent.com/Stephan-Heuscher/Simple-Phone2/master/TERMS_OF_SERVICE.md"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // How to Use / Tutorial
                SettingsButton(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.how_to_use),
                    onClick = {
                        vibrate()
                        onShowOnboarding()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Send Feedback
                SettingsButton(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.send_feedback),
                    onClick = {
                        vibrate()
                        // Collect device and app information
                        val packageInfo = try {
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        } catch (e: Exception) {
                            null
                        }
                        val versionName = packageInfo?.versionName ?: "Unknown"
                        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo?.longVersionCode?.toInt() ?: 0
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo?.versionCode ?: 0
                        }
                        val androidVersion = Build.VERSION.RELEASE
                        val apiLevel = Build.VERSION.SDK_INT
                        val manufacturer = Build.MANUFACTURER
                        val model = Build.MODEL
                        val language = java.util.Locale.getDefault().displayLanguage
                        
                        val emailBody = context.getString(
                            ch.heuscher.simplephone.R.string.feedback_body,
                            versionName,
                            versionCode,
                            androidVersion,
                            apiLevel,
                            manufacturer,
                            model,
                            language
                        )
                        
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:") // Only email apps
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("stv.heuscher@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(ch.heuscher.simplephone.R.string.feedback_subject))
                            putExtra(Intent.EXTRA_TEXT, emailBody)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // No email app?
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HorizontalDivider()
        }

        // --- Back Button ---
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { vibrate(); onBackClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.back),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

    }
}

/**
 * Row for reordering favorites with big up/down arrows
 */
@Composable
fun FavoriteReorderRow(
    contact: Contact,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    useHugeText: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    itemHeightPx: Float,
    isDragging: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    offsetY: Float,
    onOffsetYChange: (Float) -> Unit,
    vibrateFunc: (Boolean) -> Unit
) {
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentOnOffsetYChange by rememberUpdatedState(onOffsetYChange)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)
    val currentVibrateFunc by rememberUpdatedState(vibrateFunc)
    val currentOnMoveUp by rememberUpdatedState(onMoveUp)
    val currentOnMoveDown by rememberUpdatedState(onMoveDown)
    val currentCanMoveUp by rememberUpdatedState(canMoveUp)
    val currentCanMoveDown by rememberUpdatedState(canMoveDown)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ContactAvatar(
                contact = contact,
                size = 56.dp,
                showFavoriteStar = true,
                modifier = Modifier.pointerInput(contact.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            currentOnDraggingChange(true)
                            currentVibrateFunc(true)
                        },
                        onDragEnd = {
                            currentOnDraggingChange(false)
                            currentOnOffsetYChange(0f)
                        },
                        onDragCancel = {
                            currentOnDraggingChange(false)
                            currentOnOffsetYChange(0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffsetY = currentOffsetY + dragAmount.y
                            currentOnOffsetYChange(newOffsetY)

                            val threshold = itemHeightPx * 0.7f
                            if (itemHeightPx > 0) {
                                if (newOffsetY > threshold && currentCanMoveDown) {
                                    currentVibrateFunc(false)
                                    currentOnMoveDown()
                                    currentOnOffsetYChange(newOffsetY - itemHeightPx)
                                } else if (newOffsetY < -threshold && currentCanMoveUp) {
                                    currentVibrateFunc(false)
                                    currentOnMoveUp()
                                    currentOnOffsetYChange(newOffsetY + itemHeightPx)
                                }
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            val textStyle = if (useHugeText) {
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.headlineMedium
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                val scrollState = rememberScrollState()
                Text(
                    text = contact.name,
                    style = textStyle,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                )
            }
        }

        // Right side: Big Up/Down arrows with 16dp spacing for better accessibility
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BigArrowButton(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.cd_move_up, contact.name),
                onClick = onMoveUp,
                enabled = canMoveUp
            )

            BigArrowButton(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_move_down, contact.name),
                onClick = onMoveDown,
                enabled = canMoveDown
            )
        }
    }
}

/**
 * Large arrow button for reordering - triggers on press
 */
@Composable
fun BigArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.3f)
        isPressed -> HighContrastBlue.copy(alpha = 0.7f)
        else -> HighContrastBlue
    }

    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .then(
                 if(enabled) {
                    Modifier.pressClickEffect(
                        onClick = onClick,
                        onPressedChange = { isPressed = it }
                    )
                 } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.Gray,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Large icon button for filter controls - triggers on press
 */
@Composable
fun BigIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (isPressed) HighContrastBlue.copy(alpha = 0.7f) else HighContrastBlue)
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
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Option button for settings (like Dark Mode)
 */
@Composable
fun SettingsOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPressed -> if (isSelected) GreenCall.copy(alpha = 0.7f) else Color.Gray
        isSelected -> GreenCall
        else -> Color.LightGray
    }

    val textColor = if (isSelected) Color.White else Color.Black

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .semantics {
                role = Role.RadioButton
                this.selected = isSelected
            }
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Large toggle button for huge text/picture settings - triggers on press
 */
@Composable
fun BigToggleButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPressed -> if (isEnabled) GreenCall.copy(alpha = 0.7f) else HighContrastBlue.copy(alpha = 0.7f)
        isEnabled -> GreenCall
        else -> HighContrastBlue
    }

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                contentDescription = "$label toggle"
                role = Role.Switch
            }
            .pressClickEffect(
                onClick = onToggle,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Standard large button for settings actions
 */
@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) HighContrastBlue.copy(alpha = 0.7f) else HighContrastBlue)
            .semantics {
                contentDescription = "$text button"
                role = Role.Button
            }
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
