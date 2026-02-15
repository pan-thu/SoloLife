package dev.panthu.sololife.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class ExpenseCategory {
    FOOD, GROCERIES, TRANSPORT, ENTERTAINMENT, HEALTH, UTILITIES, OTHER
}

@Serializable
@Entity(
    tableName = "expenses",
    indices = [Index(value = ["date"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val amount: Double,
    val category: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
