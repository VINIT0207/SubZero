package com.example.subzero.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class AssetCategory(val displayName: String, val iconName: String) {
    SAAS("SaaS", "cloud"),
    STREAMING("Streaming", "play_circle"),
    GAMING_PASS("Gaming Pass", "sports_esports"),
    GIFT_CARD("Gift Card", "card_giftcard"),
    GYM("Gym", "fitness_center"),
    BUNDLE("Bundle", "layers"),
    CURATED_BOX("Curated Box", "inventory_2"),
    ACCESS_MEMBERSHIP("Access Membership", "card_membership")
}

@Entity(tableName = "assets")
@JsonClass(generateAdapter = true)
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: AssetCategory,
    val cost: Double,
    val isVariable: Boolean = false,
    val billingCycle: String = "Monthly", // "Monthly", "Yearly", "Weekly", "Quarterly", "One-time"
    val startDate: Long = System.currentTimeMillis(),
    val nextBillingDate: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val isTrial: Boolean = false,
    val trialLengthDays: Int = 0,
    val isCanceled: Boolean = false,
    val remainingBalance: Double = 0.0, // For gift cards
    val notes: String = "",
    val serviceTag: String = "", // e.g., "Music", "Video", "Cloud", "Storage", "Gaming"
    
    // Category-specific properties
    val seats: List<String> = emptyList(), // Account profiles or seats
    val gamingTag: String = "",
    val location: String = "", // Gym/Car wash location
    val deliveryFrequencyDays: Int = 30, // For curated boxes
    val nestedServices: List<String> = emptyList(), // Ecosystem bundle services (e.g. "Music", "Video", "Cloud")
    
    // Alarms / Nudges configuration
    val nudgesBeforeCharge: List<Int> = listOf(1, 3, 7), // Days before (e.g. 1, 3, 7 days prior)
    val lastRenewedTimestamp: Long = 0L
)

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val listStringAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val listIntAdapter = moshi.adapter<List<Int>>(
        Types.newParameterizedType(List::class.java, Integer::class.java)
    )

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { listStringAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { listStringAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { listIntAdapter.toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return value?.let { listIntAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromCategory(value: AssetCategory): String {
        return value.name
    }

    @TypeConverter
    fun toCategory(value: String): AssetCategory {
        return try {
            AssetCategory.valueOf(value)
        } catch (e: Exception) {
            AssetCategory.SAAS
        }
    }
}
