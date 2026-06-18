package com.safelive.app.utils

import androidx.compose.ui.graphics.Color
import com.safelive.app.ui.theme.*

fun String.toStatusColor(): Color {
    return when (this.lowercase()) {
        "open" -> com.safelive.app.ui.theme.StatusOpen
        "assigned" -> com.safelive.app.ui.theme.StatusInProgress
        "inspection pending" -> com.safelive.app.ui.theme.StatusPending
        "inspection completed" -> com.safelive.app.ui.theme.StatusPending
        "work assigned" -> com.safelive.app.ui.theme.StatusInProgress
        "in progress" -> com.safelive.app.ui.theme.StatusInProgress
        "awaiting verification" -> com.safelive.app.ui.theme.StatusPending
        "ready for closure" -> com.safelive.app.ui.theme.StatusResolved
        "closed" -> com.safelive.app.ui.theme.StatusResolved
        "reopened" -> com.safelive.app.ui.theme.StatusPending
        "resolved" -> com.safelive.app.ui.theme.StatusResolved
        "rejected" -> com.safelive.app.ui.theme.DangerRed
        else -> com.safelive.app.ui.theme.StatusOpen
    }
}

fun String.toPriorityColor(): Color {
    return when (this.lowercase()) {
        "low" -> PriorityLow
        "medium" -> PriorityMedium
        "high" -> PriorityHigh
        "critical" -> PriorityCritical
        else -> PriorityMedium
    }
}

fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase() }
    }
}

fun String?.orNA(): String = if (this.isNullOrBlank()) "N/A" else this

fun Int?.orZero(): Int = this ?: 0

fun Double?.orZero(): Double = this ?: 0.0

fun Boolean?.orFalse(): Boolean = this ?: false

fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()
