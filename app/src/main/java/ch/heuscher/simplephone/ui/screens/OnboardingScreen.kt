package ch.heuscher.simplephone.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.ui.theme.GreenCall
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import ch.heuscher.simplephone.R

data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

val onboardingPages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_welcome_title,
        descriptionRes = R.string.onboarding_welcome_desc,
        icon = Icons.Default.Phone
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_favorites_title,
        descriptionRes = R.string.onboarding_favorites_desc,
        icon = Icons.Default.Star
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_search_title,
        descriptionRes = R.string.onboarding_search_desc,
        icon = Icons.Default.Search
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_missed_title,
        descriptionRes = R.string.onboarding_missed_desc,
        icon = Icons.Default.History
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_audio_title,
        descriptionRes = R.string.onboarding_audio_desc,
        icon = Icons.AutoMirrored.Filled.VolumeUp
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_settings_title,
        descriptionRes = R.string.onboarding_settings_desc,
        icon = Icons.Default.Settings
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_default_title,
        descriptionRes = R.string.onboarding_default_desc,
        icon = Icons.Default.PhoneEnabled
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    useHapticFeedback: Boolean = true
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    fun vibrate() {
        if (useHapticFeedback) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { vibrate(); onComplete() }) {
                Text(
                    text = stringResource(R.string.skip),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }
        
        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(onboardingPages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                )
            }
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedButton(
                    onClick = {
                        vibrate()
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .width(120.dp)
                ) {
                    Text(
                        text = stringResource(R.string.back),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            if (pagerState.currentPage == 0) {
                Spacer(modifier = Modifier.width(120.dp))
            }
            
            // Next/Get Started button
            Button(
                onClick = {
                    vibrate()
                    if (pagerState.currentPage == onboardingPages.size - 1) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .width(if (pagerState.currentPage == onboardingPages.size - 1) 160.dp else 120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenCall
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == onboardingPages.size - 1) 
                        stringResource(R.string.get_started) 
                    else 
                        stringResource(R.string.next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Description
        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
