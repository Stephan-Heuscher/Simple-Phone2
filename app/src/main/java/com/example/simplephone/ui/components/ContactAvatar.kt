package com.example.simplephone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplephone.model.Contact
import com.example.simplephone.ui.theme.AvatarBlue
import com.example.simplephone.ui.theme.AvatarGreen
import com.example.simplephone.ui.theme.AvatarOrange
import com.example.simplephone.ui.theme.AvatarPurple
import com.example.simplephone.ui.theme.AvatarTeal
import com.example.simplephone.ui.theme.FavoriteGold

/**
 * Generates a consistent color based on the contact's name
 */
private fun getAvatarColor(name: String): Color {
    val colors = listOf(AvatarBlue, AvatarTeal, AvatarPurple, AvatarGreen, AvatarOrange)
    val index = name.hashCode().let { if (it < 0) -it else it } % colors.size
    return colors[index]
}

/**
 * A large, accessible contact avatar with optional favorite star overlay.
 * Shows initials on a colored background.
 */
@Composable
fun ContactAvatar(
    contact: Contact,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    showFavoriteStar: Boolean = true
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main avatar circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(getAvatarColor(contact.name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.initial.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp
            )
        }

        // Favorite star overlay (bottom-right corner)
        if (showFavoriteStar && contact.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(size * 0.4f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = FavoriteGold,
                    modifier = Modifier.size(size * 0.32f)
                )
            }
        }
    }
}

/**
 * A smaller avatar for compact list views
 */
@Composable
fun ContactAvatarSmall(
    contact: Contact,
    modifier: Modifier = Modifier,
    showFavoriteStar: Boolean = true
) {
    ContactAvatar(
        contact = contact,
        modifier = modifier,
        size = 56.dp,
        showFavoriteStar = showFavoriteStar
    )
}

/**
 * A large avatar for in-call screen
 */
@Composable
fun ContactAvatarLarge(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    ContactAvatar(
        contact = contact,
        modifier = modifier,
        size = 160.dp,
        showFavoriteStar = false
    )
}
