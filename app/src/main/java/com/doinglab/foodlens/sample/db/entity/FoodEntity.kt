package com.doinglab.foodlens.sample.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_table")
data class FoodEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String? = "UnKnown",
    val carbohydrate: Double,
    val protein: Double,
    val fat: Double,
    val energy: Double,
    val imagePath: String // 이미지 경로를 저장
)