package com.example.starodubovpavel.data

import retrofit2.http.GET
import retrofit2.http.Query

data class GeocodingResponse(val results: List<CityDto>? = null)

data class CityDto(
    val id: Int,
    val name: String,
    val country: String? = null,
    val admin1: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    fun toEntity() = CityEntity(
        id = id,
        name = name,
        country = country.orEmpty(),
        region = admin1.orEmpty(),
        latitude = latitude,
        longitude = longitude
    )
}

data class ForecastResponse(
    val fact: CurrentWeatherDto,
    val now_dt: String
)

data class CurrentWeatherDto(
    val temp: Double,
    val wind_speed: Double,
    val condition: String
)

fun String.toWeatherCode(): Int = when (this) {
    "clear" -> 0
    "partly-cloudy" -> 1
    "cloudy" -> 2
    "overcast" -> 3
    "drizzle", "light-rain" -> 51
    "rain", "moderate-rain" -> 61
    "heavy-rain", "continuous-heavy-rain", "showers" -> 65
    "wet-snow", "light-snow" -> 71
    "snow", "snow-showers" -> 73
    "hail" -> 77
    "thunderstorm", "thunderstorm-with-rain", "thunderstorm-with-hail" -> 95
    else -> -1
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") query: String,
        @Query("count") count: Int = 12,
        @Query("language") language: String = "ru"
    ): GeocodingResponse
}

interface ForecastApi {
    @GET("v2/forecast")
    suspend fun weather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("lang") language: String = "ru_RU",
        @Query("limit") days: Int = 1,
        @Query("hours") hours: Boolean = false
    ): ForecastResponse
}
