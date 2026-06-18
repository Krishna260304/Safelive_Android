package com.safelive.app.data.websocket

sealed class SocketState {
    data object Disconnected : SocketState()
    data object Connecting : SocketState()
    data object Connected : SocketState()
    data class Reconnecting(val attempt: Int) : SocketState()
    data class Failed(val reason: String) : SocketState()
}
