package com.safelive.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safelive.app.data.local.dao.*
import com.safelive.app.data.local.entity.*
import com.safelive.app.utils.Constants

@Database(
    entities = [
        UserEntity::class,
        IncidentEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        DraftIncidentEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class SafeLiveDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun incidentDao(): IncidentDao
    abstract fun messageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun draftIncidentDao(): DraftIncidentDao
}
