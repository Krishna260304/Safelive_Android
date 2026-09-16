package com.safelive.app.utils

import com.safelive.app.BuildConfig

object Constants {

    const val BASE_URL = BuildConfig.BASE_URL
    // The authenticated backend endpoint is /ws/incidents. Keep this in one place
    // so all clients use the same route.
    val WS_URL = BuildConfig.WS_URL.trimEnd('/') + "/incidents"

    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    const val WS_RECONNECT_DELAY_MS = 3000L
    const val WS_MAX_RECONNECT_DELAY_MS = 60000L
    const val WS_HEARTBEAT_INTERVAL_MS = 30000L
    const val WS_MAX_RETRY_COUNT = 0 // 0 means keep retrying while the session is active

    const val PREF_ACCESS_TOKEN = "access_token"
    const val PREF_REFRESH_TOKEN = "refresh_token"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_TYPE = "user_type"
    const val PREF_USER_NAME = "user_name"
    const val PREF_USER_EMAIL = "user_email"
    const val PREF_IS_LOGGED_IN = "is_logged_in"

    const val USER_TYPE_CITIZEN = "citizen"
    const val USER_TYPE_OFFICIAL = "official"

    val INCIDENT_CATEGORIES = listOf(
        "Pothole", "Garbage", "Waterlogging", "Fire", "Electricity",
        "Streetlight", "Drainage", "Public Safety", "Water Leakage",
        "Road Damage", "Other"
    )

    val PRIORITY_LEVELS = listOf("Low", "Medium", "High", "Critical")

    val INCIDENT_STATUSES = listOf(
        "Open", "Assigned", "Pending", "In Progress", "Resolved", "Verified", "Rejected"
    )

    const val EVENT_INCIDENT_CREATED = "incident_created"
    const val EVENT_INCIDENT_UPDATED = "incident_updated"
    const val EVENT_INCIDENT_RESOLVED = "incident_resolved"
    const val EVENT_STATUS_CHANGE = "status_change"
    const val EVENT_OFFICIAL_ASSIGNMENT = "official_assignment"
    const val EVENT_TICKET_MESSAGE = "ticket_message"
    const val EVENT_NOTIFICATION = "notification"
    const val EVENT_EMERGENCY_ALERT = "emergency_alert"
    const val EVENT_TYPING = "typing"
    const val EVENT_PING = "ping"
    const val EVENT_PONG = "pong"

    const val WORK_UPLOAD = "upload_work"
    const val WORK_SYNC = "sync_work"
    const val WORK_NOTIFICATION = "notification_work"

    const val DATABASE_NAME = "safelive.db"
    const val DATABASE_VERSION = 1

    const val MAX_IMAGE_SIZE_MB = 5
    const val MAX_IMAGES_PER_INCIDENT = 5
    const val IMAGE_QUALITY = 80

    const val PAGE_SIZE = 20
    const val INITIAL_PAGE = 1

    const val CACHE_DURATION_MINUTES = 5L
}
