package com.safelive.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val PrimaryTeal = Color(0xFF0F766E)
val PrimaryTealLight = Color(0xFF14B8A6)
val PrimaryTealDark = Color(0xFF0F766E) // keeping it same for dark branding
val PrimaryTealContainer = Color(0xFFCCFBF1)
val OnPrimaryTealContainer = Color(0xFF134E4A)

// Status Colors
val SuccessGreen = Color(0xFF22C55E)
val SuccessContainer = Color(0xFFDCFCE7)
val OnSuccessContainer = Color(0xFF166534)

val WarningOrange = Color(0xFFF59E0B)
val WarningContainer = Color(0xFFFEF3C7)
val OnWarningContainer = Color(0xFF92400E)

val DangerRed = Color(0xFFEF4444)
val DangerContainer = Color(0xFFFEE2E2)
val OnDangerContainer = Color(0xFF991B1B)

// Neutral Colors
val NeutralGrey50 = Color(0xFFF0FAF8)
val NeutralGrey100 = Color(0xFFF1F5F9)
val NeutralGrey200 = Color(0xFFE2E8F0)
val NeutralGrey300 = Color(0xFFCBD5E1)
val NeutralGrey400 = Color(0xFF94A3B8)
val NeutralGrey500 = Color(0xFF64748B)
val NeutralGrey600 = Color(0xFF475569)
val NeutralGrey800 = Color(0xFF1E293B)
val NeutralGrey900 = Color(0xFF0F172A)

// Background & Surface
val BackgroundLight = Color(0xFFF0FAF8)
val SurfaceLight = Color(0xFFFFFFFF)

// Semantic Status Updates
val StatusOpen = Color(0xFF3B82F6) // Blue
val StatusPending = WarningOrange // Orange
val StatusInProgress = Color(0xFFA855F7) // Purple
val StatusResolved = SuccessGreen // Green

val PriorityLow = SuccessGreen
val PriorityMedium = WarningOrange
val PriorityHigh = DangerRed
val PriorityCritical = Color(0xFF991B1B)

// Light Theme Scheme Tokens
val md_theme_light_primary = PrimaryTeal
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = PrimaryTealContainer
val md_theme_light_onPrimaryContainer = OnPrimaryTealContainer
val md_theme_light_secondary = PrimaryTealLight
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = PrimaryTealContainer
val md_theme_light_onSecondaryContainer = OnPrimaryTealContainer
val md_theme_light_tertiary = SuccessGreen
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = SuccessContainer
val md_theme_light_onTertiaryContainer = OnSuccessContainer
val md_theme_light_error = DangerRed
val md_theme_light_errorContainer = DangerContainer
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = OnDangerContainer
val md_theme_light_background = BackgroundLight
val md_theme_light_onBackground = NeutralGrey900
val md_theme_light_surface = SurfaceLight
val md_theme_light_onSurface = NeutralGrey900
val md_theme_light_surfaceVariant = NeutralGrey50
val md_theme_light_onSurfaceVariant = NeutralGrey500
val md_theme_light_outline = NeutralGrey200
val md_theme_light_inverseOnSurface = NeutralGrey50
val md_theme_light_inverseSurface = NeutralGrey800
val md_theme_light_inversePrimary = PrimaryTealLight
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_surfaceTint = PrimaryTeal
val md_theme_light_outlineVariant = NeutralGrey200
val md_theme_light_scrim = Color(0xFF000000)

// Dark Theme Scheme Tokens (Aligned to similar contrast as light theme but inverted surfaces)
val md_theme_dark_primary = PrimaryTealLight
val md_theme_dark_onPrimary = Color(0xFF042F2E)
val md_theme_dark_primaryContainer = PrimaryTealDark
val md_theme_dark_onPrimaryContainer = PrimaryTealContainer
val md_theme_dark_secondary = PrimaryTealLight
val md_theme_dark_onSecondary = Color(0xFF042F2E)
val md_theme_dark_secondaryContainer = PrimaryTealDark
val md_theme_dark_onSecondaryContainer = PrimaryTealContainer
val md_theme_dark_tertiary = SuccessGreen
val md_theme_dark_onTertiary = Color(0xFF064E3B)
val md_theme_dark_tertiaryContainer = Color(0xFF047857)
val md_theme_dark_onTertiaryContainer = SuccessContainer
val md_theme_dark_error = Color(0xFFF87171)
val md_theme_dark_errorContainer = Color(0xFF991B1B)
val md_theme_dark_onError = Color(0xFF450A0A)
val md_theme_dark_onErrorContainer = DangerContainer
val md_theme_dark_background = NeutralGrey900
val md_theme_dark_onBackground = NeutralGrey100
val md_theme_dark_surface = NeutralGrey800
val md_theme_dark_onSurface = NeutralGrey100
val md_theme_dark_surfaceVariant = NeutralGrey900
val md_theme_dark_onSurfaceVariant = NeutralGrey400
val md_theme_dark_outline = NeutralGrey600
val md_theme_dark_inverseOnSurface = NeutralGrey900
val md_theme_dark_inverseSurface = NeutralGrey200
val md_theme_dark_inversePrimary = PrimaryTeal
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = PrimaryTealLight
val md_theme_dark_outlineVariant = NeutralGrey600
val md_theme_dark_scrim = Color(0xFF000000)
