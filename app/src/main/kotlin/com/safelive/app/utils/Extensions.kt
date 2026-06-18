package com.safelive.app.utils

import androidx.compose.ui.graphics.Color
import com.safelive.app.ui.theme.*

fun String.toStatusColor(): Color {
    return when (this.lowercase()) {
        "open" -> StatusOpen
        "assigned" -> com.safelive.app.ui.theme.StatusInProgress
        "pending" -> com.safelive.app.ui.theme.StatusPending
        "resolved" -> com.safelive.app.ui.theme.StatusResolved
        "closed" -> com.safelive.app.ui.theme.StatusResolved
        "verified" -> com.safelive.app.ui.theme.StatusResolved
        "rejected" -> com.safelive.app.ui.theme.DangerRed
        else -> StatusOpen
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
