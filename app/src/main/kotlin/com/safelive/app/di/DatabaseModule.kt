package com.safelive.app.di

import android.content.Context
import androidx.room.Room
import com.safelive.app.data.local.dao.*
import com.safelive.app.data.local.db.SafeLiveDatabase
import com.safelive.app.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSafeLiveDatabase(
        @ApplicationContext context: Context
    ): SafeLiveDatabase {
        return Room.databaseBuilder(
            context,
            SafeLiveDatabase::class.java,
            Constants.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(db: SafeLiveDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideIncidentDao(db: SafeLiveDatabase): IncidentDao = db.incidentDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: SafeLiveDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideNotificationDao(db: SafeLiveDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    fun provideDraftIncidentDao(db: SafeLiveDatabase): DraftIncidentDao = db.draftIncidentDao()
}
