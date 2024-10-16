package com.doinglab.foodlens.sample.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doinglab.foodlens.sample.db.entity.FoodEntity

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Query("SELECT * FROM food_table")
    fun getAllFoods(): LiveData<List<FoodEntity>>

    @Delete
    suspend fun deleteFood(food: FoodEntity) // 특정 FoodEntity 삭제

    @Query("DELETE FROM food_table")
    suspend fun deleteAllFoods() // 모든 데이터 삭제

}