package com.agusw.testcalculatorscan.di

import android.content.Context
import com.agusw.testcalculatorscan.App
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModules {

    @Provides
    fun instance(@ApplicationContext app: Context): App = app as App
}