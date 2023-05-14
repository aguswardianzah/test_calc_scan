package com.agusw.testcalculatorscan.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agusw.testcalculatorscan.db.entities.Result

@Dao
interface ResultDao {

    @Query("select * from result order by id desc")
    suspend fun get(): List<Result>

    @Query("select * from result where id = :id")
    suspend fun get(id: Int): Result

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: Result)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: List<Result>)

    @Query("delete from result")
    suspend fun nuke()
}