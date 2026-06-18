package com.safelive.app.domain.usecase.notification

import com.safelive.app.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(private val repo: NotificationRepository) {
    operator fun invoke() = repo.getNotifications()
}

class GetUnreadCountUseCase @Inject constructor(private val repo: NotificationRepository) {
    operator fun invoke() = repo.getUnreadCount()
}

class MarkNotificationReadUseCase @Inject constructor(private val repo: NotificationRepository) {
    suspend operator fun invoke(id: String) = repo.markRead(id)
}

class MarkAllNotificationsReadUseCase @Inject constructor(private val repo: NotificationRepository) {
    suspend operator fun invoke() = repo.markAllRead()
}

class DeleteNotificationUseCase @Inject constructor(private val repo: NotificationRepository) {
    suspend operator fun invoke(id: String) = repo.deleteNotification(id)
}
