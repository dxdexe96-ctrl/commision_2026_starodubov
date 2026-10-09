package com.example.starodubovpavel.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.starodubovpavel.WeatherApp

class WeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as WeatherApp).repository
        return if (repository.refreshPlannedCities()) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "planned-cities-weather-sync"
    }
}
