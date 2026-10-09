package com.example.starodubovpavel.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weather_cache",
    foreignKeys = [ForeignKey(
        entity = CityEntity::class,
        parentColumns = ["id"],
        childColumns = ["cityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cityId")]
)
data class WeatherCacheEntity(
    @PrimaryKey val cityId: Int,
    val temperature: Double,
    val windSpeed: Double,
    val weatherCode: Int,
    val observedAt: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "search_history",
    foreignKeys = [ForeignKey(
        entity = CityEntity::class,
        parentColumns = ["id"],
        childColumns = ["cityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cityId")]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cityId: Int,
    val openedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "city_plans",
    foreignKeys = [ForeignKey(
        entity = CityEntity::class,
        parentColumns = ["id"],
        childColumns = ["cityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cityId")]
)
data class CityPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cityId: Int,
    val title: String,
    val note: String,
    val targetDate: String,
    val status: String = PlanStatus.PLANNED.name,
    val createdAt: Long = System.currentTimeMillis()
)

data class HistoryWithCity(
    @Embedded val history: SearchHistoryEntity,
    @Relation(parentColumn = "cityId", entityColumn = "id")
    val city: CityEntity
)

data class PlanWithCity(
    @Embedded val plan: CityPlanEntity,
    @Relation(parentColumn = "cityId", entityColumn = "id")
    val city: CityEntity
)

enum class PlanStatus { PLANNED, DONE, CANCELLED }

data class Weather(
    val temperature: Double,
    val windSpeed: Double,
    val weatherCode: Int,
    val observedAt: String,
    val isFromCache: Boolean
)

data class AppSettings(
    val darkTheme: Boolean = false,
    val useFahrenheit: Boolean = false,
    val cacheHours: Int = 24
)

sealed interface LoadResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : LoadResult<T>
    data class Error(val message: String) : LoadResult<Nothing>
}
