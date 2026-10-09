package com.example.starodubovpavel

import com.example.starodubovpavel.data.PlanStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanRulesTest {
    @Test
    fun statusCycleDrivesPlanScreenState() {
        val done = PlanRules.nextStatus(PlanStatus.PLANNED.name)
        val cancelled = PlanRules.nextStatus(done)
        val plannedAgain = PlanRules.nextStatus(cancelled)

        assertEquals(PlanStatus.DONE.name, done)
        assertEquals(PlanStatus.CANCELLED.name, cancelled)
        assertEquals(PlanStatus.PLANNED.name, plannedAgain)
    }

    @Test
    fun temperaturePreferenceChangesDisplayedValue() {
        assertEquals("0.0 °C", TemperatureFormatter.format(0.0, fahrenheit = false))
        assertEquals("32.0 °F", TemperatureFormatter.format(0.0, fahrenheit = true))
    }
}
