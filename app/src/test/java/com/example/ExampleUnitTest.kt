package com.example

import com.example.data.DefaultStateFactory
import com.example.model.CalculationEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPhysicsFormulas() {
        // Test saturation vapor pressure at 20 °C
        val psat20 = CalculationEngine.psat(20.0)
        assertTrue("Psat at 20°C should be ~2338 Pa", psat20 in 2300.0..2400.0)

        // Test dew point inversion
        val tdew = CalculationEngine.tDewFromP(psat20)
        assertEquals("Dew point of saturated air at 20°C must be 20°C", 20.0, tdew, 0.1)

        // Test initial calculation
        val state = DefaultStateFactory.createDefault()
        val totals = CalculationEngine.calculateAllTotals(
            boundary = state.boundary,
            walls = state.walls,
            roofs = state.roofs,
            floors = state.floors,
            windows = state.windows,
            doors = state.doors,
            bridges = state.bridges,
            ventilation = state.ventilation,
            dhwAndWell = state.dhwAndWell,
            hpSettings = state.hpSettings,
            solarAndGrid = state.solarAndGrid,
            meterData = state.meterData,
            fuels = state.fuels,
            materials = CalculationEngine.BASE_MATERIALS
        )

        assertTrue("Total heat loss should be > 0", totals.totalKw > 0.0)
        assertTrue("Annual thermal kWh should be > 0", totals.annualKwh > 0.0)
        assertTrue("Walls loss should be > 0", totals.wallsQ > 0.0)
    }
}
