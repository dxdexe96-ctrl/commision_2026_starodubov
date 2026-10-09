package com.example.starodubovpavel

import com.example.starodubovpavel.data.PlanStatus
import java.util.Locale

object CachePolicy {
    fun isFresh(updatedAt: Long, ttlHours: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (ttlHours <= 0) return false
        return now - updatedAt < ttlHours * 60L * 60L * 1000L
    }
}

object TemperatureFormatter {
    fun format(celsius: Double, fahrenheit: Boolean): String {
        val value = if (fahrenheit) celsius * 9 / 5 + 32 else celsius
        val unit = if (fahrenheit) "°F" else "°C"
        return "${String.format(Locale.ROOT, "%.1f", value)} $unit"
    }
}

object PlanRules {
    fun nextStatus(current: String): String = when (current) {
        PlanStatus.PLANNED.name -> PlanStatus.DONE.name
        PlanStatus.DONE.name -> PlanStatus.CANCELLED.name
        else -> PlanStatus.PLANNED.name
    }
}
