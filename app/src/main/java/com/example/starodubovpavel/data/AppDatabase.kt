package com.example.starodubovpavel.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCities(cities: List<CityEntity>)

    @Query("SELECT * FROM cities WHERE id = :id")
    suspend fun city(id: Int): CityEntity?

    @Query("SELECT * FROM cities WHERE name LIKE '%' || :query || '%' OR country LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT 20")
    suspend fun cachedSearch(query: String): List<CityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeather(weather: WeatherCacheEntity)

    @Query("SELECT * FROM weather_cache WHERE cityId = :cityId")
    suspend fun cachedWeather(cityId: Int): WeatherCacheEntity?

    @Query("SELECT c.* FROM cities c INNER JOIN city_plans p ON p.cityId = c.id GROUP BY c.id")
    suspend fun citiesWithPlans(): List<CityEntity>

    @Insert
    suspend fun addHistory(item: SearchHistoryEntity)

    @Transaction
    @Query("SELECT * FROM search_history ORDER BY openedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<HistoryWithCity>>

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()

    @Query("DELETE FROM search_history WHERE id = :historyId")
    suspend fun deleteHistory(historyId: Long)

    @Insert
    suspend fun addPlan(plan: CityPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: CityPlanEntity)

    @Delete
    suspend fun deletePlan(plan: CityPlanEntity)

    @Transaction
    @Query("SELECT * FROM city_plans ORDER BY CASE status WHEN 'PLANNED' THEN 0 ELSE 1 END, targetDate, createdAt DESC")
    fun observePlans(): Flow<List<PlanWithCity>>
}

@Database(
    entities = [CityEntity::class, WeatherCacheEntity::class, SearchHistoryEntity::class, CityPlanEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
