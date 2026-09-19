package com.example.myexpenditureapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_category_rules",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["keyword"], unique = true)
    ]
)
data class AutoCategoryRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String, // e.g., "SWIGGY", "UBER", "STARBUCKS", "AMAZON"
    val categoryId: Long,
    val matchType: String = "CONTAINS" // "CONTAINS", "EXACT", "REGEX"
)
