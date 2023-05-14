package com.agusw.testcalculatorscan.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agusw.testcalculatorscan.db.dao.ResultDao
import com.agusw.testcalculatorscan.db.entities.Result

@Database(
    version = 2,
    exportSchema = false,
    entities = [Result::class]
)
abstract class TestDB : RoomDatabase() {
    abstract fun result(): ResultDao
}