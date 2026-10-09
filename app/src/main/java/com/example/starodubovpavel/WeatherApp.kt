package com.example.starodubovpavel

import android.app.Application
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.starodubovpavel.data.AppDatabase
import com.example.starodubovpavel.data.ForecastApi
import com.example.starodubovpavel.data.GeocodingApi
import com.example.starodubovpavel.data.SettingsRepository
import com.example.starodubovpavel.data.WeatherRepository
import com.example.starodubovpavel.worker.WeatherSyncWorker
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherApp : Application() {
    lateinit var repository: WeatherRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, AppDatabase::class.java, "weather-companion.db").build()
        val geocoding = Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)
        val yandexClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Yandex-Weather-Key", BuildConfig.YANDEX_WEATHER_API_KEY)
                    .build()
                chain.proceed(request)
            }
            .build()
        val forecast = Retrofit.Builder()
            .baseUrl("https://api.weather.yandex.ru/")
            .client(yandexClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ForecastApi::class.java)
        repository = WeatherRepository(database.weatherDao(), geocoding, forecast)
        settingsRepository = SettingsRepository(this)

        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WeatherSyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
