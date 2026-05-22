package ch.heuscher.simplephone.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.BottomAppBar 
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import ch.heuscher.simplephone.R

import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Representation of the application's screens/destinations for Jetpack Compose Navigation.
 * Each screen defines a unique [route] for routing, a [titleRes] string resource for the title bar,
 * and an [icon] representing the destination.
 */
sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.screen_home, Icons.Filled.Home)
    object Recents : Screen("recents", R.string.screen_recents, Icons.Filled.History)
    object Favorites : Screen("favorites", R.string.favorites, Icons.Filled.Star)

    object Settings : Screen("settings", R.string.settings_title, Icons.Filled.Settings)
    object InCall : Screen("incall", R.string.screen_home, Icons.Filled.Home) // InCall title is usually dynamic or "Call"
    object Dialer : Screen("dialer", R.string.screen_dialer, Icons.Filled.Call)
    object CallLog : Screen("calllog", R.string.screen_call_log, Icons.Filled.History)

}

/**
 * Main layout scaffold for the application, featuring a large, highly visible top app bar.
 * It supports standard navigation elements such as an optional back arrow and a settings button,
 * both of which are styled to trigger actions on immediate press instead of release.
 *
 * @param navController The navigation controller used to manage backstack and screen routing.
 * @param currentScreenTitle The title text displayed prominently in the top bar.
 * @param onTitleClick Optional callback when the top bar title text is clicked.
 * @param content Composable slot representing the main body content of the active screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppScaffold(
    navController: NavController,
    currentScreenTitle: String,
    onTitleClick: () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBackArrow = currentRoute == Screen.Settings.route || currentRoute == Screen.Dialer.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentScreenTitle,
                        style = MaterialTheme.typography.headlineLarge, // Huge title
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onTitleClick() }
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    if (showBackArrow) {
                        // Large back arrow button - triggers on press
                        AccessibleIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_go_back),
                            onClick = { navController.popBackStack() },
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (!showBackArrow) {
                        // Settings button - triggers on press
                        AccessibleIconButton(
                            icon = Screen.Settings.icon,
                            contentDescription = stringResource(R.string.cd_open_settings),
                            onClick = { navController.navigate(Screen.Settings.route) },
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        // Bottom bar removed as per request to have everything on one page
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content(Modifier)
        }
    }
}

/**
 * Accessible icon button that triggers its callback on immediate press (ACTION_DOWN) instead of release (ACTION_UP).
 * This design prevents accidental double-clicks, button slippage, or missed clicks common among seniors
 * and users with motor control impairments or hand tremors.
 *
 * @param icon The vector asset representing the action of the button.
 * @param contentDescription Screen reader text description.
 * @param onClick Callback executed immediately when the button area is pressed.
 * @param tint Color applied to the icon vector.
 * @param modifier Modifier applied to the outer layout wrapper.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AccessibleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onPrimary,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(8.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isPressed) Color.White.copy(alpha = 0.3f) else Color.Transparent)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onClick()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp)
        )
    }
}
