package com.example.starodubovpavel

import com.example.starodubovpavel.data.CityEntity
import com.example.starodubovpavel.data.ForecastApi
import com.example.starodubovpavel.data.GeocodingApi
import com.example.starodubovpavel.data.LoadResult
import com.example.starodubovpavel.data.WeatherDao
import com.example.starodubovpavel.data.WeatherRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryOfflineTest {
    @Test
    fun networkFailureReturnsRoomSearchResults() = runTest {
        val dao = mockk<WeatherDao>(relaxed = true)
        val geocoding = mockk<GeocodingApi>()
        val city = CityEntity(1, "Владивосток", "Россия", "Приморский край", 43.1, 131.9)
        coEvery { geocoding.search(any(), any(), any()) } throws IllegalStateException("offline")
        coEvery { dao.cachedSearch("Владивосток") } returns listOf(city)

        val repository = WeatherRepository(dao, geocoding, mockk<ForecastApi>())
        val result = repository.search("Владивосток")

        assertTrue(result is LoadResult.Success)
        result as LoadResult.Success
        assertTrue(result.fromCache)
        assertEquals(city, result.data.single())
    }
}
