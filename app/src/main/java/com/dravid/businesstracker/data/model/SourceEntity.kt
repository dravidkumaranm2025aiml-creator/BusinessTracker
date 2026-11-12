package com.dravid.businesstracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a lead or referral source channel.
 * Source names must be unique (ignoring case).
 */
@Entity(
    tableName = "sources",
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    val name: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,
)
