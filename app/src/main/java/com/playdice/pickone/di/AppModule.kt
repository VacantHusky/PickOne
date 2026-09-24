package com.playdice.pickone.di

import android.content.Context
import androidx.room.Room
import com.playdice.pickone.data.AppDatabase
import com.playdice.pickone.data.SceneDao
import com.playdice.pickone.data.UpdateChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideUpdateChecker(): UpdateChecker = UpdateChecker()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pickone.db")
            .build()

    @Provides
    fun provideSceneDao(database: AppDatabase): SceneDao = database.sceneDao()
}
