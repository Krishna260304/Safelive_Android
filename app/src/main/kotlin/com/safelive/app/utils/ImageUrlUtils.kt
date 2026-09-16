package com.safelive.app.utils

/** Converts the relative image paths returned by the API into loadable URLs. */
object ImageUrlUtils {
    fun normalize(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        if (raw.matches(Regex("^[A-Za-z0-9+/=\\r\\n]{200,}$"))) {
            return "data:image/jpeg;base64,${raw.replace(Regex("\\s+"), "")}"
        }
        if (raw.startsWith("http://") || raw.startsWith("https://") ||
            raw.startsWith("content://") || raw.startsWith("file://") ||
            raw.startsWith("android.resource://") || raw.startsWith("data:")) {
            return if (raw.startsWith("http://api.safelive.in", ignoreCase = true)) {
                "https://${raw.substringAfter("://")}"
            } else raw
        }

        val apiBase = Constants.BASE_URL.trimEnd('/')
        val publicBase = apiBase.substringBefore("/api", apiBase).trimEnd('/')
        val path = raw.removePrefix("./").let {
            when {
                it.startsWith("/api/") -> it.removePrefix("/api")
                it.startsWith("api/") -> it.removePrefix("api/")
                else -> it
            }
        }
        return if (path.startsWith('/')) "$publicBase$path" else "$publicBase/$path"
    }
}
