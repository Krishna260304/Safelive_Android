package com.safelive.app.data.repository

import com.safelive.app.data.local.dao.MessageDao
import com.safelive.app.data.local.entity.MessageEntity
import com.safelive.app.data.remote.api.ChatApi
import com.safelive.app.domain.model.Message
import com.safelive.app.domain.repository.ChatRepository
import com.safelive.app.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getMessages(incidentId: String): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading)
        try {
            val response = chatApi.getIncidentMessages(incidentId)
            if (response.success && response.data != null) {
                val msgs = response.data.map { it.toDomain() }
                messageDao.insertMessages(msgs.map { it.toEntity() })
                emit(Resource.Success(msgs))
            } else {
                emit(Resource.Error(response.error ?: "Failed to load messages"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun sendMessage(incidentId: String, content: String): Resource<Message> {
        return try {
            val response = chatApi.sendIncidentMessage(incidentId, mapOf("content" to content))
            if (response.success && response.data != null) {
                val msg = response.data.toDomain()
                messageDao.insertMessage(msg.toEntity())
                Resource.Success(msg)
            } else {
                Resource.Error(response.error ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    private fun Message.toEntity() = MessageEntity(
        id = id,
        incidentId = incidentId,
        senderId = senderId,
        senderName = senderName,
        senderType = senderType,
        content = content,
        timestamp = timestamp,
        isRead = isRead,
        isSent = true
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        incidentId = incidentId,
        senderId = senderId,
        senderName = senderName ?: "",
        senderType = senderType,
        content = content,
        timestamp = timestamp ?: "",
        isRead = isRead
    )
}
