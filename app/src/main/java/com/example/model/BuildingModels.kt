package com.example.model

data class Material(
    val id: String,
    val group: String,
    val name: String,
    val defD: Double,
    val lambda: Double,
    val mu: Double,
    val rho: Double,
    val c: Double,
    val colorHex: String,
    val ventilated: Boolean = false,
    val isModifiedBase: Boolean = false
)

data class Layer(
    val matId: String,
    val name: String,
    val d: Double, // in cm
    val lambda: Double, // W/mK
    val mu: Double,
    val rho: Double, // kg/m3
    val c: Double, // J/kgK
    val woodPct: Double = 0.0 // percentage of wood / rafters
)

data class ComputedLayer(
    val matId: String,
    val name: String,
    val d: Double,
    val lambda: Double,
    val mu: Double,
    val rho: Double,
    val c: Double,
    val woodPct: Double,
    val lamEff: Double,
    val r: Double,
    val sd: Double,
    val dM: Double,
    val layerPhi: Double,
    val isVentAir: Boolean,
    val bypassedByVent: Boolean
)

data class GlaserNode(
    val label: String,
    val xCm: Double,
    val temperature: Double,
    val vaporPressure: Double,
    val satVaporPressure: Double,
    val dewPoint: Double,
    val condenses: Boolean,
    val bypassedByVent: Boolean = false
)

data class AssemblyResult(
    val computedLayers: List<ComputedLayer>,
    val sumD: Double,
    val sumR: Double,
    val sumSd: Double,
    val rTot: Double,
    val uVal: Double,
    val qLoss: Double,
    val tsi: Double,
    val fRsi: Double,
    val phaseShiftHours: Double,
    val decrementFactor: Double,
    val sumHeatCapKj: Double,
    val nodes: List<GlaserNode>,
    val condensationZones: List<String>,
    val hasVentilatedLayer: Boolean
)

data class EnvelopeElement(
    val id: String,
    val name: String,
    val area: Double,
    val fx: Double = 1.0,
    val rsi: Double = 0.13,
    val rse: Double = 0.04,
    val targetU: Double = 0.18,
    val layers: List<Layer>,
    val rafterTargetLayer: Int = 0
)

data class WindowElement(
    val id: String,
    val name: String,
    val w: Double, // width in m
    val h: Double, // height in m
    val count: Int,
    val uw: Double,
    val g: Double = 0.50,
    val orient: String = "J",
    val fx: Double = 1.0
)

data class DoorElement(
    val id: String,
    val name: String,
    val area: Double,
    val ud: Double,
    val fx: Double = 1.0
)

data class BridgeItem(
    val id: String,
    val name: String,
    val length: Double,
    val psi: Double,
    val fx: Double = 1.0
)

data class BridgeSettings(
    val enabled: Boolean = true,
    val useFlatRate: Boolean = false,
    val flatDeltaU: Double = 0.05,
    val items: List<BridgeItem> = emptyList()
)

data class VentilationSettings(
    val enabled: Boolean = true,
    val volumeArea: Double = 180.0,
    val ceilingHeight: Double = 2.55,
    val airChanges: Double = 0.40,
    val heatRecoveryEff: Double = 85.0,
    val selectedUnitId: String = "zehnder_q350"
)

data class DhwAndWellSettings(
    val dhwEnabled: Boolean = true,
    val dhwSourceMode: String = "main", // "main", "sanitary_hp", "el_boiler", "combo"
    val mainHpDhwCop: Double = 3.1,
    val sanitaryHpCop: Double = 2.8,
    val persons: Int = 4,
    val litersPerPerson: Double = 45.0,
    val tempCold: Double = 10.0,
    val tempHot: Double = 50.0,
    val tankLossPct: Double = 15.0,
    val wellPumpAutoSync: Boolean = true,
    val hpThermalPowerKw: Double = 8.0,
    val hpMachineCop: Double = 5.4,
    val wellPumpWatts: Double = 350.0,
    val wellWaterDeltaT: Double = 3.5,
    val summerIndoorTemp: Double = 25.0,
    val summerIndoorRh: Double = 60.0
)

data class HpSettings(
    val selectedHpFuelId: String = "tc_voda",
    val nominalThermalKw: Double = 8.0,
    val minModulationKw: Double = 2.2,
    val maxThermalKwAtPeak: Double = 8.5,
    val peakElMode: String = "manual", // "manual" or "auto"
    val manualPeakElKw: Double = 1.95,
    val maxCompressorElKw: Double = 2.60,
    val copAtDesignTe: Double = 4.30,
    val auxPumpsWatts: Double = 390.0,
    val bivalentTemp: Double = -7.0,
    val backupHeaterKw: Double = 0.0,
    val fuseAmps: Int = 20,
    val baseHousePeakKw: Double = 3.5
)

data class MeterData(
    val enabled: Boolean = true,
    val totalMeterKwh: Double = 4880.0,
    val vtKwh: Double = 2247.0,
    val mtKwh: Double = 2633.0,
    val measuredHpElKwh: Double = 1985.0,
    val measuredAcElKwh: Double = 1031.0,
    val measuredHouseholdKwh: Double = 1864.0,
    val annualBillEur: Double = 871.0,
    val monthlyTotalKwh: List<Double> = listOf(680.0, 560.0, 460.0, 340.0, 270.0, 290.0, 350.0, 340.0, 280.0, 350.0, 440.0, 520.0)
)

data class SolarAndGridSettings(
    val pvKwp: Double = 13.0,
    val specificYield: Double = 1100.0,
    val simLoadSource: String = "meter", // "meter" or "model"
    val batteryCapKwh: Double = 15.0,
    val batteryDodPct: Double = 90.0,
    val batteryEffPct: Double = 90.0,
    val directSelfConsPct: Double = 38.0,
    val exportMode: String = "sell", // "sell" or "zero"
    val exportPriceEur: Double = 0.055,
    val gridImportPriceEur: Double = 0.178,
    val batteryInvestEur: Double = 3500.0
)

data class HydraulicsSettings(
    val floorDeltaT: Double = 5.0
)

data class Fuel(
    val id: String,
    val name: String,
    val unit: String,
    val price: Double,
    val eff: Double,
    val energyPerUnit: Double,
    val invest: Double = 0.0
)

data class RoomElement(
    val id: String,
    val name: String,
    val area: Double,
    val ti: Double = 21.0,
    val expFactor: Double = 1.0,
    val spacing: Double = 15.0 // cm
)

data class RoiMeasure(
    val id: String,
    val name: String,
    val area: Double,
    val uBefore: Double,
    val uAfter: Double,
    val invest: Double,
    val subsidy: Double
)

data class BoundaryCondition(
    val ti: Double = 21.0,
    val te: Double = -13.0,
    val phiI: Double = 50.0,
    val phiE: Double = 80.0,
    val heatedArea: Double = 180.0,
    val hours: Double = 1900.0
)

data class ScenarioSnapshot(
    val totalW: Double,
    val annualKwh: Double,
    val eur: Double
)

data class MonthlySimRow(
    val monthName: String,
    val meterM: Double,
    val hpModelM: Double,
    val loadM: Double,
    val pvM: Double,
    val directM: Double,
    val batM: Double,
    val gridWithBatM: Double,
    val exportWithBatM: Double,
    val selfSuffPct: Double,
    val selfSuffNoBatPct: Double
)

data class SolarBatterySimResult(
    val months: List<MonthlySimRow>,
    val usableBatKwh: Double,
    val sumLoad: Double,
    val sumPv: Double,
    val sumDirect: Double,
    val sumBatDischarged: Double,
    val sumGridWithBat: Double,
    val sumGridNoBat: Double,
    val sumExportWithBat: Double,
    val sumExportNoBat: Double,
    val selfSuffNoBatAnnualPct: Double,
    val selfSuffWithBatAnnualPct: Double,
    val eqFullCycles: Double,
    val costNoPvEur: Double,
    val costPvOnlyEur: Double,
    val costPvWithBatEur: Double,
    val batteryExtraSavingsEur: Double,
    val totalPvAndBatSavingsEur: Double,
    val batPaybackYears: Double
)

data class DhwAndWellResult(
    val dailyDhwKwh: Double,
    val annualDhwKwh: Double,
    val totalThermalKwh: Double,
    val runHours: Double,
    val compressorElKwh: Double,
    val wellPumpElKwh: Double,
    val totalHpWaterElKwh: Double,
    val systemScop: Double,
    val evaporatorKw: Double,
    val requiredWaterFlowM3h: Double,
    val requiredWaterFlowLmin: Double,
    val floorSummerDewPoint: Double,
    val minSafeCoolingWaterTemp: Double
)

data class FuelCostResult(
    val heatingEur: Double,
    val dhwEur: Double,
    val annualEur: Double,
    val costPerEffectiveKwh: Double
)

data class AllTotals(
    val wallsQ: Double,
    val roofsQ: Double,
    val floorsQ: Double,
    val windowsQ: Double,
    val doorsQ: Double,
    val bridgesQ: Double,
    val ventQ: Double,
    val ventNaturalQ: Double,
    val totalW: Double,
    val totalKw: Double,
    val grossHeatingKwh: Double,
    val solarGainKwh: Double,
    val netHeatingKwh: Double,
    val annualDhwKwh: Double,
    val annualKwh: Double,
    val specificKwh: Double,
    val specificW: Double,
    val totalEnvArea: Double,
    val dhwWell: DhwAndWellResult,
    val annualHeatElKwh: Double,
    val annualDhwElKwh: Double,
    val modelHpElKwh: Double,
    val autoHpElAtTeKw: Double,
    val effectiveHpElAtTeKw: Double,
    val solarBatSim: SolarBatterySimResult,
    val envelopeMeetsPures: Boolean,
    val envelopeMeetsPassive: Boolean
)
