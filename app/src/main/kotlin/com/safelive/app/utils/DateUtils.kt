package com.safelive.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DateUtils {

    private val UTC = TimeZone.getTimeZone("UTC")
    private val ISO_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { timeZone = UTC },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = UTC },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).apply { timeZone = UTC },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).apply { timeZone = UTC },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply { timeZone = UTC },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = UTC },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = UTC }
    ).onEach { it.isLenient = false }

    private val DISPLAY_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    private val DATE_ONLY_FORMAT = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val TIME_ONLY_FORMAT = SimpleDateFormat("hh:mm a", Locale.US)
    private val EXACT_FORMAT = SimpleDateFormat("dd/MM/yyyy, h:mm:ss a", Locale.US)
    private val ISO_OUTPUT_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = UTC
        isLenient = false
    }

    fun parseIso(isoString: String?): Date? {
        val raw = isoString?.trim().orEmpty()
        if (raw.isBlank()) return null

        val normalized = raw
            .replace(Regex("\\.(\\d{3})\\d+"), ".$1")
            .replace("Z", "+00:00")

        for (candidate in listOf(raw, normalized)) {
            for (format in ISO_FORMATS) {
                try {
                    val parsed = format.parse(candidate)
                    if (parsed != null) return parsed
                } catch (_: Exception) {
                    // Try the next parser shape.
                }
            }
        }
        return null
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

    fun formatExact(isoString: String?): String {
        val date = parseIso(isoString) ?: return "N/A"
        return EXACT_FORMAT.format(date).replace("AM", "am").replace("PM", "pm")
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

    fun toIsoString(date: Date): String = ISO_OUTPUT_FORMAT.format(date)

    fun currentIsoString(): String = ISO_OUTPUT_FORMAT.format(Date())
}
