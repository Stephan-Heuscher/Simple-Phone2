package com.example.simplephone.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplephone.ui.theme.GreenCall
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun DialerScreen(
    onCallClick: (String) -> Unit,
    useHapticFeedback: Boolean = false
) {
    var phoneNumber by remember { mutableStateOf("") }
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        // Rotary Dialer
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RotaryDialer(
                onDigitDialed = { digit ->
                    if (phoneNumber.length < 15) {
                        phoneNumber += digit
                    }
                    if (useHapticFeedback) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
        }

        // Actions (Call and Backspace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Spacer(modifier = Modifier.size(80.dp)) // Placeholder to balance backspace

            // Call Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GreenCall)
                    .clickable {
                        if (phoneNumber.isNotEmpty()) {
                            onCallClick(phoneNumber)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Backspace Button
            IconButton(
                onClick = {
                    if (useHapticFeedback) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    if (phoneNumber.isNotEmpty()) {
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RotaryDialer(
    onDigitDialed: (String) -> Unit
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragStartAngle by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var currentDragAngle by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Constants for layout
    // 0 is at roughly 4 o'clock position, 1 is at roughly 2 o'clock
    // We distribute them around the circle.
    // Let's say the "stop" is at 45 degrees (bottom right).
    // 0 needs to travel the furthest? No, 1 travels furthest in a real dialer?
    // In a real dialer:
    // 1 is at top right (approx 60 deg)
    // 2 is at ...
    // 0 is at bottom (approx 0 deg or 360)
    // The stop is at approx 45 deg.
    // Let's simplify:
    // Place numbers in a circle.
    // 1 at 60 deg, 2 at 90, 3 at 120...
    // Stop is at 30 deg.
    
    val startAngle = 60f
    val anglePerDigit = 30f
    val stopAngle = 45f // Where the finger stop is visually

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val size = minOf(maxWidth, maxHeight)
        val radius = with(LocalDensity.current) { (size / 2).toPx() }
        val center = Offset(radius, radius)
        
        // Static background (numbers)
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Draw numbers at their resting positions
            // We don't draw them on the canvas because we want Text composables for better quality
        }
        
        // Draw numbers (Static layer underneath)
        Box(modifier = Modifier.size(size)) {
            numbers.forEachIndexed { index, number ->
                val angle = startAngle + (index * anglePerDigit)
                val rad = angle * (PI / 180)
                // Position numbers slightly inside the edge
                val numRadius = size.value / 2 * 0.85f
                
                // Convert polar to cartesian, center is (size/2, size/2)
                // Y grows downwards
                val x = (size.value / 2) + numRadius * cos(rad).toFloat()
                val y = (size.value / 2) + numRadius * sin(rad).toFloat()
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(
                            x = (x - 20).dp, // Center the box
                            y = (y - 20).dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Rotating Disk (The part you drag)
        Box(
            modifier = Modifier
                .size(size)
                .rotate(rotation.value)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Calculate angle of touch
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            dragStartAngle = (atan2(dy, dx) * 180 / PI).toFloat()
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            // Snap back
                            scope.launch {
                                // Check if we rotated enough to dial
                                // For simplicity, if we rotated significantly, we assume a dial
                                // In a real app, we'd check if a specific hole reached the stop
                                
                                // Find which number was closest to the stop?
                                // This is a simplified simulation.
                                // Let's just snap back.
                                rotation.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val currentPos = change.position
                            val dx = currentPos.x - center.x
                            val dy = currentPos.y - center.y
                            val angle = (atan2(dy, dx) * 180 / PI).toFloat()
                            
                            val delta = angle - dragStartAngle
                            
                            // Update rotation
                            // Limit rotation?
                            // In a real dialer, you can only rotate clockwise until the stop
                            // And only counter-clockwise back to start
                            
                            // Simplified: just rotate
                            scope.launch {
                                rotation.snapTo(rotation.value + delta)
                            }
                            
                            dragStartAngle = angle
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw the disk with holes
                drawCircle(
                    color = Color.Transparent, // Transparent center
                    radius = radius
                )
                
                // Draw the outer ring (the disk itself)
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.5f),
                    radius = radius,
                    style = Stroke(width = size.toPx() * 0.15f) // Thick ring
                )
                
                // Draw holes
                // Holes should align with numbers when rotation is 0
                val holeRadius = size.toPx() * 0.06f
                
                numbers.forEachIndexed { index, _ ->
                    val angle = startAngle + (index * anglePerDigit)
                    val rad = angle * (PI / 180)
                    val dist = radius * 0.85f
                    
                    val x = center.x + dist * cos(rad).toFloat()
                    val y = center.y + dist * sin(rad).toFloat()
                    
                    // Draw "hole" (clear circle or white circle)
                    drawCircle(
                        color = Color.White, // Background color to simulate hole
                        radius = holeRadius,
                        center = Offset(x, y)
                    )
                    
                    // Draw border for hole
                    drawCircle(
                        color = Color.DarkGray,
                        radius = holeRadius,
                        center = Offset(x, y),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        // Finger Stop (Static, on top)
        // Positioned at stopAngle
        val stopRad = stopAngle * (PI / 180)
        val stopDist = radius * 0.85f
        val stopX = (size.value / 2) + stopDist * cos(stopRad).toFloat()
        val stopY = (size.value / 2) + stopDist * sin(stopRad).toFloat()
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .offset(
                    x = (stopX - 20).dp,
                    y = (stopY - 20).dp
                )
                .background(Color.Black, CircleShape) // The metal stop
        )
        
        // Center decoration
        Box(
            modifier = Modifier
                .size(size * 0.3f)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { 
                    // Center button could be a quick dial or just decoration
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Simple", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
