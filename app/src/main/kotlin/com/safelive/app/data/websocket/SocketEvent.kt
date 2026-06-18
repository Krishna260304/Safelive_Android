package com.safelive.app.data.websocket

sealed class SocketEvent {
    data class IncidentCreated(val data: Map<String, Any>) : SocketEvent()
    data class IncidentUpdated(val data: Map<String, Any>) : SocketEvent()
    data class IncidentResolved(val data: Map<String, Any>) : SocketEvent()
    data class StatusChange(val incidentId: String, val status: String) : SocketEvent()
    data class OfficialAssignment(val incidentId: String, val officialId: String) : SocketEvent()
    data class TicketMessage(val chatId: String, val messageId: String, val content: String) : SocketEvent()
    data class NotificationReceived(val id: String, val title: String, val body: String, val type: String) : SocketEvent()
    data class EmergencyAlert(val message: String, val severity: String) : SocketEvent()
    data class TypingIndicator(val chatId: String, val userId: String, val isTyping: Boolean) : SocketEvent()
    /**
     * Structured warning from the backend AI service.
     * Severity is one of: "LOW", "MEDIUM", "HIGH"
     * Android MUST NOT determine severity — it is set exclusively by the backend.
     */
    data class BackendWarning(
        val warningId: String,
        val title: String,
        val message: String,
        val severity: String,  // "LOW" | "MEDIUM" | "HIGH"
        val timestamp: Long = System.currentTimeMillis(),
        val actionLabel: String? = null,
        val actionRoute: String? = null
    ) : SocketEvent()
    data object Ping : SocketEvent()
    data class Unknown(val type: String, val data: Map<String, Any>) : SocketEvent()
}
