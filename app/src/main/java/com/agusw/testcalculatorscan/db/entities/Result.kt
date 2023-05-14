package com.agusw.testcalculatorscan.db.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
@Entity(tableName = "result", indices = [Index(value = ["id"], unique = true)])
data class Result(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val input: String,
    val output: String
)