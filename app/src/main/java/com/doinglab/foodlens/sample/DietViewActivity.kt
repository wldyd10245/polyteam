package com.doinglab.foodlens.sample

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.doinglab.foodlens.sample.db.FoodDatabase
import com.doinglab.foodlens.sample.db.entity.FoodEntity
import com.doinglab.foodlens.sample.db.repository.FoodRepository
import com.doinglab.foodlens.sample.db.repository.FoodViewModel
import com.doinglab.foodlens.sample.db.repository.FoodViewModelFactory

class DietViewActivity : AppCompatActivity() {
    private lateinit var viewModel: FoodViewModel
    private lateinit var repository: FoodRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var dietAdapter: DietAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diet_view)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Repository 초기화
        val dao = FoodDatabase.getInstance(application).foodDao()
        repository = FoodRepository(dao)

        // ViewModelFactory 초기화 (Application과 Repository 전달)
        val factory = FoodViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[FoodViewModel::class.java]

        // 어댑터 초기화
        dietAdapter = DietAdapter(mutableListOf()) { food ->
            // 항목을 길게 누르면 호출되는 콜백
            showDeleteConfirmationDialog(food)
        }
        recyclerView.adapter = dietAdapter

        // 데이터 관찰
        viewModel.getAllFoods().observe(this, { foodList ->
            dietAdapter.updateData(foodList)
        })

        // 전체 삭제 버튼 설정
        val deleteAllButton: Button = findViewById(R.id.btn_all_delete)
        deleteAllButton.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }

    }

    private fun showDeleteConfirmationDialog(food: FoodEntity) {
        AlertDialog.Builder(this)
            .setTitle("삭제 확인")
            .setMessage("${food.name}을(를) 삭제하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                viewModel.deleteFood(food)
                Toast.makeText(this, "${food.name}이(가) 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    private fun showDeleteAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("전체 삭제 확인")
            .setMessage("모든 식단 데이터를 삭제하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                viewModel.deleteAllFoods()
                Toast.makeText(this, "모든 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("아니오", null)
            .show()
    }
}
