package com.example.myexpenditureapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"])
    ]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: BigDecimal,
    val billingCycle: String = "Monthly", // "Monthly", "Quarterly", "Yearly", "Weekly"
    val dueDayOfMonth: Int = 1, // 1..31
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val autoDetectEnabled: Boolean = true,
    val lastPaidDate: Long? = null,
    val notes: String? = null,
    val isActive: Boolean = true
)
