package com.agusw.testcalculatorscan.di

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MoshiModules {

    @Provides
    fun instance(): Moshi = Moshi.Builder().build()
}