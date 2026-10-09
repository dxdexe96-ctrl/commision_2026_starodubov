package com.example.starodubovpavel.data

import com.example.starodubovpavel.CachePolicy
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val dao: WeatherDao,
    private val geocodingApi: GeocodingApi,
    private val forecastApi: ForecastApi,
    private val clock: () -> Long = System::currentTimeMillis
) {
    val history: Flow<List<HistoryWithCity>> = dao.observeHistory()
    val plans: Flow<List<PlanWithCity>> = dao.observePlans()

    suspend fun search(query: String): LoadResult<List<CityEntity>> {
        val normalized = query.trim()
        if (normalized.length < 2) return LoadResult.Success(emptyList())
        return try {
            val cities = geocodingApi.search(normalized).results.orEmpty().map { it.toEntity() }
            dao.upsertCities(cities)
            LoadResult.Success(cities)
        } catch (error: Exception) {
            val cached = dao.cachedSearch(normalized)
            if (cached.isNotEmpty()) LoadResult.Success(cached, fromCache = true)
            else LoadResult.Error(error.message ?: "Нет сети и подходящих данных в кэше")
        }
    }

    suspend fun openCity(city: CityEntity) {
        dao.upsertCities(listOf(city.copy(cachedAt = clock())))
        dao.addHistory(SearchHistoryEntity(cityId = city.id, openedAt = clock()))
    }

    suspend fun weather(city: CityEntity, ttlHours: Int, force: Boolean = false): LoadResult<Weather> {
        val cached = dao.cachedWeather(city.id)
        if (!force && cached != null && CachePolicy.isFresh(cached.updatedAt, ttlHours, clock())) {
            return LoadResult.Success(cached.toWeather(fromCache = true), fromCache = true)
        }
        return try {
            val response = forecastApi.weather(city.latitude, city.longitude)
            val remote = response.fact
            val entity = WeatherCacheEntity(
                cityId = city.id,
                temperature = remote.temp,
                windSpeed = remote.wind_speed,
                weatherCode = remote.condition.toWeatherCode(),
                observedAt = response.now_dt,
                updatedAt = clock()
            )
            dao.upsertWeather(entity)
            LoadResult.Success(entity.toWeather(fromCache = false))
        } catch (error: Exception) {
            cached?.let { LoadResult.Success(it.toWeather(fromCache = true), fromCache = true) }
                ?: LoadResult.Error(error.message ?: "Погода пока недоступна")
        }
    }

    suspend fun addPlan(city: CityEntity, title: String, note: String, date: String) {
        dao.upsertCities(listOf(city))
        dao.addPlan(
            CityPlanEntity(
                cityId = city.id,
                title = title.trim(),
                note = note.trim(),
                targetDate = date.trim().ifBlank { "Без даты" }
            )
        )
    }

    suspend fun updatePlan(plan: CityPlanEntity) = dao.updatePlan(plan)
    suspend fun deletePlan(plan: CityPlanEntity) = dao.deletePlan(plan)
    suspend fun deleteHistory(historyId: Long) = dao.deleteHistory(historyId)
    suspend fun clearHistory() = dao.clearHistory()

    suspend fun refreshPlannedCities(): Boolean {
        var allSucceeded = true
        dao.citiesWithPlans().forEach { city ->
            if (weather(city, ttlHours = 0, force = true) is LoadResult.Error) allSucceeded = false
        }
        return allSucceeded
    }
}

private fun WeatherCacheEntity.toWeather(fromCache: Boolean) = Weather(
    temperature = temperature,
    windSpeed = windSpeed,
    weatherCode = weatherCode,
    observedAt = observedAt,
    isFromCache = fromCache
)
