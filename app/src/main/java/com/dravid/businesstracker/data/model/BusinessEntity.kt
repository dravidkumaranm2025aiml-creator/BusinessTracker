package com.dravid.businesstracker.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an insurance business record.
 * Premium is stored as an exact Long integer representing paise (1 Rupee = 100 paise)
 * to eliminate floating-point rounding errors.
 */
@Entity(tableName = "business_records")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "policy_number")
    val policyNumber: String,

    @ColumnInfo(name = "premium_amount_paise")
    val premiumAmountPaise: Long,

    @ColumnInfo(name = "status")
    val status: BusinessStatus,

    @ColumnInfo(name = "business_date")
    val businessDate: Long,

    @ColumnInfo(name = "issue_date")
    val issueDate: Long? = null,

    @ColumnInfo(name = "source_id")
    val sourceId: Long? = null,
)
