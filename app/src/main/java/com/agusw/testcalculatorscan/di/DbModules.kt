package com.agusw.testcalculatorscan.di

import android.content.Context
import androidx.room.Room
import com.agusw.testcalculatorscan.db.TestDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DbModules {

    @Provides
    fun instance(@ApplicationContext context: Context): TestDB =
        Room.databaseBuilder(context, TestDB::class.java, "test.db")
            .fallbackToDestructiveMigration()
            .build()
}