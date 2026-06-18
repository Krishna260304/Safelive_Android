package com.safelive.app.data.local.dao

import androidx.room.*
import com.safelive.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY createdAt DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    fun getIncidentById(id: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE status = :status ORDER BY createdAt DESC")
    fun getIncidentsByStatus(status: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE isSynced = 0")
    suspend fun getUnsyncedIncidents(): List<IncidentEntity>

    @Query("""
        SELECT * FROM incidents 
        WHERE (:status IS NULL OR status = :status)
        AND (:category IS NULL OR category = :category)
        AND (:search IS NULL OR title LIKE '%' || :search || '%' OR description LIKE '%' || :search || '%')
        ORDER BY createdAt DESC
    """)
    fun getFilteredIncidents(
        status: String? = null,
        category: String? = null,
        search: String? = null
    ): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<IncidentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: String)

    @Query("DELETE FROM incidents")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM incidents WHERE status = 'Open' OR status = 'In Progress'")
    fun getActiveIncidentCount(): Flow<Int>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE incidentId = :chatId ORDER BY timestamp ASC")
    fun getMessagesByChatId(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE incidentId = :chatId")
    suspend fun markAllReadInChat(chatId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE incidentId = :chatId AND isRead = 0 AND senderId != :currentUserId")
    fun getUnreadCountInChat(chatId: String, currentUserId: String): Flow<Int>

    @Query("DELETE FROM messages WHERE incidentId = :chatId")
    suspend fun clearChatMessages(chatId: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface DraftIncidentDao {
    @Query("SELECT * FROM draft_incidents ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<DraftIncidentEntity>>

    @Query("SELECT * FROM draft_incidents WHERE id = :id")
    suspend fun getDraftById(id: Int): DraftIncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftIncidentEntity): Long

    @Update
    suspend fun updateDraft(draft: DraftIncidentEntity)

    @Query("DELETE FROM draft_incidents WHERE id = :id")
    suspend fun deleteDraftById(id: Int)

    @Query("DELETE FROM draft_incidents")
    suspend fun clearAll()
}
