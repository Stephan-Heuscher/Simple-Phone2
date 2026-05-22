package ch.heuscher.simplephone.ui.components

import androidx.compose.ui.res.stringResource
import ch.heuscher.simplephone.R


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.theme.AvatarBlue
import ch.heuscher.simplephone.ui.theme.AvatarGreen
import ch.heuscher.simplephone.ui.theme.AvatarOrange
import ch.heuscher.simplephone.ui.theme.AvatarPurple
import ch.heuscher.simplephone.ui.theme.AvatarTeal
import ch.heuscher.simplephone.ui.theme.FavoriteGold
import ch.heuscher.simplephone.ui.theme.HighContrastBlue

/**
 * Generates a consistent background color deterministically based on the hash code of the contact's name.
 * This ensures that a given contact always displays with the same background color.
 *
 * @param name The contact's full name.
 * @return A color selected from a predefined senior-accessible palette.
 */
private fun getAvatarColor(name: String): Color {
    val colors = listOf(AvatarBlue, AvatarTeal, AvatarPurple, AvatarGreen, AvatarOrange)
    val index = name.hashCode().let { if (it < 0) -it else it } % colors.size
    return colors[index]
}

/**
 * A large, accessible contact avatar component featuring a fallback initials display,
 * custom color coding, and an optional gold star overlay for favorite/starred contacts.
 *
 * @param contact The contact model instance whose avatar is being rendered.
 * @param modifier Modifier applied to the outer avatar box container.
 * @param size The size dimension of the avatar. Defaults to 72.dp.
 * @param showFavoriteStar If true, renders a gold star badge in the corner if [contact.isFavorite] is true.
 */
@Composable
fun ContactAvatar(
    contact: Contact,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    showFavoriteStar: Boolean = true
) {
    // Slightly rounded square shape
    val avatarShape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main avatar - show photo if available, otherwise initials
        // Only show border if it's a favorite
        val borderModifier = if (contact.isFavorite) {
            Modifier.border(BorderStroke(4.dp, FavoriteGold), avatarShape)
        } else {
            Modifier
        }

        if (contact.imageUri != null) {
            AsyncImage(
                model = contact.imageUri,
                contentDescription = stringResource(R.string.cd_contact_photo, contact.name),
                modifier = Modifier
                    .size(size)
                    .clip(avatarShape)
                    .then(borderModifier),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback to initials
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(avatarShape)
                    .background(getAvatarColor(contact.name))
                    .then(borderModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.initial.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45f).sp
                )
            }
        }

        // Favorite star overlay (top-right corner) - minimized collision
        if (showFavoriteStar && contact.isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp) // Push it out a bit
                    .size(size * 0.4f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, FavoriteGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = FavoriteGold,
                    modifier = Modifier.size(size * 0.3f)
                )
            }
        }
    }
}

/**
 * A smaller contact avatar optimized for compact lists or dense detail rows.
 * Automatically wraps [ContactAvatar] with a preconfigured default size of 56.dp.
 *
 * @param contact The contact model instance whose avatar is being rendered.
 * @param modifier Modifier applied to the outer layout.
 * @param showFavoriteStar If true, renders the favorite badge in the corner if applicable.
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
 * An oversized contact avatar optimized for full-screen calls or active caller displays.
 * Automatically wraps [ContactAvatar] with a preconfigured default size of 160.dp.
 * Favorite star is omitted to maximize clarity and visual focus on caller details.
 *
 * @param contact The contact model instance whose avatar is being rendered.
 * @param modifier Modifier applied to the outer layout.
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
