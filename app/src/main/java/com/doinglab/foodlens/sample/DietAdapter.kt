package com.doinglab.foodlens.sample

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.doinglab.foodlens.sample.db.entity.FoodEntity

class DietAdapter(private var foodList: MutableList<FoodEntity>,
                  private val onItemLongClick: (FoodEntity) -> Unit) : RecyclerView.Adapter<DietAdapter.DietViewHolder>() {

    inner class DietViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodName: TextView = itemView.findViewById(R.id.food_name)
        val foodImage: ImageView = itemView.findViewById(R.id.food_image)
        val foodNutrition: TextView = itemView.findViewById(R.id.food_nutrition)
        val foodEnergy: TextView = itemView.findViewById(R.id.food_energy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return DietViewHolder(view)
    }

    override fun onBindViewHolder(holder: DietViewHolder, position: Int) {
        val food = foodList[position]
        holder.foodName.text = food.name
        holder.foodNutrition.text = "Carbohydrate: ${food.carbohydrate}g, Protein: ${food.protein}g, Fat: ${food.fat}g"
        holder.foodEnergy.text = "Energy: ${food.energy} kcal"

        // 이미지 로딩
        val bitmap = BitmapFactory.decodeFile(food.imagePath)
        holder.foodImage.setImageBitmap(bitmap)

        // 항목을 길게 누르면 삭제
        holder.itemView.setOnLongClickListener {
            onItemLongClick(food)
            true
        }
    }

    override fun getItemCount(): Int = foodList.size

    // 데이터 업데이트를 위한 메소드 추가
    fun updateData(newFoodList: List<FoodEntity>) {
        foodList.clear()
        foodList.addAll(newFoodList)
        notifyDataSetChanged()
    }
}
