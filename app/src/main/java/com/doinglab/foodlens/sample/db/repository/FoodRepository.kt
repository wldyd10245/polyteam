package com.doinglab.foodlens.sample.db.repository

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.doinglab.foodlens.sample.db.dao.FoodDao
import com.doinglab.foodlens.sample.db.entity.FoodEntity
import kotlinx.coroutines.launch

class FoodRepository(private val foodDao: FoodDao) {
    suspend fun insertFood(food: FoodEntity) {
        foodDao.insertFood(food)
    }

    fun getAllFoods(): LiveData<List<FoodEntity>> {
        return foodDao.getAllFoods()
    }

    suspend fun deleteFood(food: FoodEntity) {
        foodDao.deleteFood(food) // 특정 항목 삭제
    }

    suspend fun deleteAllFoods() {
        foodDao.deleteAllFoods() // 모든 항목 삭제
    }
}

class FoodViewModel(application: Application, private val repository: FoodRepository) : AndroidViewModel(application) {

    fun insertFood(food: FoodEntity) = viewModelScope.launch {
        repository.insertFood(food)
    }

    fun getAllFoods(): LiveData<List<FoodEntity>> {
        return repository.getAllFoods()
    }

    fun deleteFood(food: FoodEntity) = viewModelScope.launch {
        repository.deleteFood(food)
    }

    fun deleteAllFoods() = viewModelScope.launch {
        repository.deleteAllFoods()
    }
}


class FoodViewModelFactory(private val application: Application, private val repository: FoodRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoodViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

