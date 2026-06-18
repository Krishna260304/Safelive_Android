package com.safelive.app.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.safelive.app.navigation.Screen
import com.safelive.app.ui.theme.PrimaryBlue
import com.safelive.app.ui.theme.SecondaryTeal

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        alpha.animateTo(1f, animationSpec = tween(600))
    }

    LaunchedEffect(logoScale) {
        while (true) {
            logoScale.animateTo(1.05f, animationSpec = tween(1000))
            logoScale.animateTo(0.95f, animationSpec = tween(1000))
        }
    }

    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Login -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is SplashDestination.CitizenDashboard -> {
                navController.navigate(Screen.CitizenDashboard.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            is SplashDestination.OfficialDashboard -> {
                navController.navigate(Screen.OfficialDashboard.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryBlue, SecondaryTeal.copy(alpha = 0.7f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.extraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 60.sp,
                    modifier = Modifier.scale(logoScale.value)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SafeLive",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Report. Track. Resolve.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(64.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(36.dp)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Powered by Government of India",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
