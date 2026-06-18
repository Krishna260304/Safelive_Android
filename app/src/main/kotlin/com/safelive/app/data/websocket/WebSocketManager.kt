package com.safelive.app.data.websocket

import com.google.gson.Gson
import com.safelive.app.data.local.datastore.UserPreferencesDataStore
import com.safelive.app.di.WebSocketClient
import com.safelive.app.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    @WebSocketClient private val okHttpClient: OkHttpClient,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val gson: Gson
) {

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var retryCount = 0

    private val _socketState = MutableStateFlow<SocketState>(SocketState.Disconnected)
    val socketState: StateFlow<SocketState> = _socketState.asStateFlow()

    private val _socketEvents = MutableSharedFlow<SocketEvent>(
        replay = 0,
        extraBufferCapacity = 50
    )
    val socketEvents: SharedFlow<SocketEvent> = _socketEvents.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect() {
        if (_socketState.value is SocketState.Connected || _socketState.value is SocketState.Connecting) {
            Timber.d("WebSocket already connected or connecting")
            return
        }

        coroutineScope.launch {
            val token = userPreferencesDataStore.accessToken.first()
            if (token.isNullOrBlank()) {
                Timber.w("No token available, skipping WebSocket connection")
                return@launch
            }
            establishConnection(token)
        }
    }

    private fun establishConnection(token: String) {
        _socketState.value = SocketState.Connecting

        val wsUrl = "${Constants.WS_URL}?token=$token"
        // Only pass the token as a query parameter.
        // Do NOT add Authorization/Content-Type/Accept headers here —
        // those break the WebSocket HTTP 101 Upgrade handshake (Cloudflare rejects them).
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
        Timber.d("WebSocket connecting to $wsUrl")
    }

    private fun createWebSocketListener() = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timber.d("WebSocket connected")
            retryCount = 0
            _socketState.value = SocketState.Connected
            startHeartbeat()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Timber.d("WebSocket message: $text")
            parseAndEmitEvent(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.d("WebSocket closing: $code $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.d("WebSocket closed: $code $reason")
            _socketState.value = SocketState.Disconnected
            stopHeartbeat()
            if (code != 1000) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.e(t, "WebSocket failure")
            _socketState.value = SocketState.Failed(t.message ?: "Unknown error")
            stopHeartbeat()
            scheduleReconnect()
        }
    }

    private fun parseAndEmitEvent(text: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val json = gson.fromJson(text, Map::class.java) as Map<String, Any>
            val type = json["type"] as? String ?: return
            @Suppress("UNCHECKED_CAST")
            val data = json["data"] as? Map<String, Any> ?: emptyMap()

            val event = when (type) {
                Constants.EVENT_INCIDENT_CREATED -> SocketEvent.IncidentCreated(data)
                Constants.EVENT_INCIDENT_UPDATED -> SocketEvent.IncidentUpdated(data)
                Constants.EVENT_INCIDENT_RESOLVED -> SocketEvent.IncidentResolved(data)
                Constants.EVENT_STATUS_CHANGE -> SocketEvent.StatusChange(
                    incidentId = data["incident_id"] as? String ?: "",
                    status = data["status"] as? String ?: ""
                )
                Constants.EVENT_OFFICIAL_ASSIGNMENT -> SocketEvent.OfficialAssignment(
                    incidentId = data["incident_id"] as? String ?: "",
                    officialId = data["official_id"] as? String ?: ""
                )
                Constants.EVENT_TICKET_MESSAGE -> SocketEvent.TicketMessage(
                    chatId = data["chat_id"] as? String ?: "",
                    messageId = data["message_id"] as? String ?: "",
                    content = data["content"] as? String ?: ""
                )
                Constants.EVENT_NOTIFICATION -> SocketEvent.NotificationReceived(
                    id = data["id"] as? String ?: "",
                    title = data["title"] as? String ?: "",
                    body = data["body"] as? String ?: "",
                    type = data["type"] as? String ?: ""
                )
                Constants.EVENT_EMERGENCY_ALERT -> SocketEvent.EmergencyAlert(
                    message = data["message"] as? String ?: "",
                    severity = data["severity"] as? String ?: "high"
                )
                Constants.EVENT_TYPING -> SocketEvent.TypingIndicator(
                    chatId = data["chat_id"] as? String ?: "",
                    userId = data["user_id"] as? String ?: "",
                    isTyping = data["is_typing"] as? Boolean ?: false
                )
                Constants.EVENT_PONG, Constants.EVENT_PING -> SocketEvent.Ping
                else -> SocketEvent.Unknown(type, data)
            }

            coroutineScope.launch {
                _socketEvents.emit(event)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebSocket message: $text")
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = coroutineScope.launch {
            while (isActive && _socketState.value is SocketState.Connected) {
                delay(Constants.WS_HEARTBEAT_INTERVAL_MS)
                sendPing()
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendPing() {
        try {
            val pingPayload = gson.toJson(mapOf("type" to Constants.EVENT_PING))
            webSocket?.send(pingPayload)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send ping")
        }
    }

    fun sendMessage(type: String, data: Map<String, Any>) {
        try {
            val payload = gson.toJson(mapOf("type" to type, "data" to data))
            val sent = webSocket?.send(payload) ?: false
            if (!sent) {
                Timber.w("Failed to send WebSocket message: $payload")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending WebSocket message")
        }
    }

    private fun scheduleReconnect() {
        if (retryCount >= Constants.WS_MAX_RETRY_COUNT) {
            Timber.w("Max reconnect attempts reached")
            _socketState.value = SocketState.Failed("Max reconnect attempts reached")
            return
        }

        reconnectJob?.cancel()
        retryCount++
        val delay = minOf(
            Constants.WS_RECONNECT_DELAY_MS * (1L shl (retryCount - 1)),
            Constants.WS_MAX_RECONNECT_DELAY_MS
        )

        Timber.d("Scheduling reconnect attempt $retryCount in ${delay}ms")
        _socketState.value = SocketState.Reconnecting(retryCount)

        reconnectJob = coroutineScope.launch {
            delay(delay)
            connect()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        stopHeartbeat()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _socketState.value = SocketState.Disconnected
        retryCount = 0
    }

    fun isConnected(): Boolean = _socketState.value is SocketState.Connected
}
