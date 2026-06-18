package com.safelive.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DateUtils {

    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val DISPLAY_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val DATE_ONLY_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val TIME_ONLY_FORMAT = SimpleDateFormat("hh:mm a", Locale.US)

    fun parseIso(isoString: String?): Date? {
        if (isoString.isNullOrBlank()) return null
        return try {
            ISO_FORMAT.parse(isoString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatDisplay(isoString: String?): String {
        val date = parseIso(isoString) ?: return "N/A"
        return DISPLAY_FORMAT.format(date)
    }

    fun formatDateOnly(isoString: String?): String {
        val date = parseIso(isoString) ?: return "N/A"
        return DATE_ONLY_FORMAT.format(date)
    }

    fun formatTimeOnly(isoString: String?): String {
        val date = parseIso(isoString) ?: return "N/A"
        return TIME_ONLY_FORMAT.format(date)
    }

    fun formatTime(isoString: String?): String = formatTimeOnly(isoString)

    fun getTimeAgo(isoString: String?): String {
        val date = parseIso(isoString) ?: return "N/A"
        val diffMs = System.currentTimeMillis() - date.time
        return when {
            diffMs < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diffMs < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diffMs)}m ago"
            diffMs < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}h ago"
            diffMs < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d ago"
            else -> DATE_ONLY_FORMAT.format(date)
        }
    }

    fun toIsoString(date: Date): String = ISO_FORMAT.format(date)

    fun currentIsoString(): String = ISO_FORMAT.format(Date())
}
