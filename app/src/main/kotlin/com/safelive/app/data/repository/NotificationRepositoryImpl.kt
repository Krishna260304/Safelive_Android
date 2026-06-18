package com.safelive.app.data.repository

import com.safelive.app.data.local.dao.NotificationDao
import com.safelive.app.data.local.entity.NotificationEntity
import com.safelive.app.data.remote.api.NotificationApi
import com.safelive.app.domain.model.Notification
import com.safelive.app.domain.repository.NotificationRepository
import com.safelive.app.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi,
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getNotifications(): Flow<Resource<List<Notification>>> = flow {
        emit(Resource.Loading)
        try {
            val response = notificationApi.getNotifications()
            if (response.success && response.data != null) {
                val notifications = response.data.map { it.toDomain() }
                notificationDao.insertNotifications(notifications.map { it.toEntity() })
                emit(Resource.Success(notifications))
            } else {
                emit(Resource.Error(response.error ?: "Failed to load notifications"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override fun getUnreadCount(): Flow<Int> = notificationDao.getUnreadCount()

    override suspend fun markRead(id: String): Resource<Unit> {
        return try {
            val response = notificationApi.markAsRead(id)
            if (response.success) {
                notificationDao.markRead(id)
                Resource.Success(Unit)
            } else {
                Resource.Error(response.error ?: "Failed to mark notification as read")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun markAllRead(): Resource<Unit> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.success) {
                notificationDao.markAllRead()
                Resource.Success(Unit)
            } else {
                Resource.Error(response.error ?: "Failed to mark notifications as read")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun deleteNotification(id: String): Resource<Unit> {
        return try {
            val response = notificationApi.deleteNotification(id)
            if (response.success) {
                notificationDao.deleteById(id)
                Resource.Success(Unit)
            } else {
                Resource.Error(response.error ?: "Failed to delete notification")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun syncNotifications() {
        val response = notificationApi.getNotifications()
        if (response.success && response.data != null) {
            notificationDao.insertNotifications(response.data.map { it.toDomain().toEntity() })
        }
    }

    private fun Notification.toEntity() = NotificationEntity(
        id = id,
        title = title,
        body = body,
        type = type,
        referenceId = referenceId,
        isRead = isRead,
        createdAt = createdAt
    )
}
