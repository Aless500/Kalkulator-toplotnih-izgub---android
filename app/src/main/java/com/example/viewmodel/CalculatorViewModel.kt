package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.AppState
import com.example.data.AppStateRepository
import com.example.data.DefaultStateFactory
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppStateRepository(application)

    private val _state = MutableStateFlow(repo.loadState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _materials = MutableStateFlow(computeAllMaterials(_state.value))
    val materials: StateFlow<List<Material>> = _materials.asStateFlow()

    private val _totals = MutableStateFlow(computeTotals(_state.value, _materials.value))
    val totals: StateFlow<AllTotals> = _totals.asStateFlow()

    private fun mutate(block: (AppState) -> AppState) {
        val newState = block(_state.value)
        _state.value = newState
        val allMats = computeAllMaterials(newState)
        _materials.value = allMats
        _totals.value = computeTotals(newState, allMats)
        repo.saveState(newState)
    }

    private fun computeAllMaterials(s: AppState): List<Material> {
        return s.customMaterials + CalculationEngine.BASE_MATERIALS
    }

    private fun computeTotals(s: AppState, mats: List<Material>): AllTotals {
        return CalculationEngine.calculateAllTotals(
            boundary = s.boundary,
            walls = s.walls,
            roofs = s.roofs,
            floors = s.floors,
            windows = s.windows,
            doors = s.doors,
            bridges = s.bridges,
            ventilation = s.ventilation,
            dhwAndWell = s.dhwAndWell,
            hpSettings = s.hpSettings,
            solarAndGrid = s.solarAndGrid,
            meterData = s.meterData,
            fuels = s.fuels,
            materials = mats
        )
    }

    fun switchTab(tabId: String) = mutate { it.copy(activeTabId = tabId) }
    fun toggleDarkMode() = mutate { it.copy(isDarkMode = !it.isDarkMode) }

    fun exportJson(): String {
        return repo.exportToJson(_state.value)
    }

    fun importJson(jsonStr: String): Boolean {
        return try {
            val newState = repo.importFromJson(jsonStr)
            _state.value = newState
            val allMats = computeAllMaterials(newState)
            _materials.value = allMats
            _totals.value = computeTotals(newState, allMats)
            repo.saveState(newState)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getSavedProjects(): List<Pair<String, Long>> = repo.getSavedProjects()

    fun saveNamedProject(name: String) {
        repo.saveNamedProject(name, _state.value)
    }

    fun loadNamedProject(name: String): Boolean {
        val loaded = repo.loadNamedProject(name) ?: return false
        _state.value = loaded
        val allMats = computeAllMaterials(loaded)
        _materials.value = allMats
        _totals.value = computeTotals(loaded, allMats)
        repo.saveState(loaded)
        return true
    }

    fun deleteNamedProject(name: String) {
        repo.deleteNamedProject(name)
    }

    fun updateBoundary(
        ti: Double = _state.value.boundary.ti,
        te: Double = _state.value.boundary.te,
        phiI: Double = _state.value.boundary.phiI,
        phiE: Double = _state.value.boundary.phiE,
        heatedArea: Double = _state.value.boundary.heatedArea,
        hours: Double = _state.value.boundary.hours
    ) = mutate {
        it.copy(
            boundary = it.boundary.copy(
                ti = ti,
                te = te,
                phiI = phiI,
                phiE = phiE,
                heatedArea = heatedArea,
                hours = hours
            )
        )
    }

    fun applyCityPreset(te: Double) = updateBoundary(te = te)

    fun saveScenarioA() {
        val t = _totals.value
        val curEur = CalculationEngine.calcFuelAnnualCost(
            fuel = _state.value.fuels.firstOrNull() ?: Fuel("tc_voda", "TČ", "€", 0.17, 4.5, 1.0),
            dhwSettings = _state.value.dhwAndWell,
            primaryElFuel = _state.value.fuels.firstOrNull() ?: Fuel("tc_voda", "TČ", "€", 0.17, 4.5, 1.0),
            heatingKwh = t.netHeatingKwh,
            dhwKwh = t.annualDhwKwh
        ).annualEur

        mutate {
            it.copy(
                scenarioA = ScenarioSnapshot(
                    totalW = t.totalW,
                    annualKwh = t.annualKwh,
                    eur = curEur
                )
            )
        }
    }

    fun clearScenarioA() = mutate { it.copy(scenarioA = null) }

    // Envelope Assemblies
    fun updateElementField(group: String, elIdx: Int, name: String? = null, area: Double? = null, fx: Double? = null) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices) {
            val el = list[elIdx]
            list[elIdx] = el.copy(
                name = name ?: el.name,
                area = area ?: el.area,
                fx = fx ?: el.fx
            )
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun addElement(group: String) = mutate { s ->
        val newEl = EnvelopeElement(
            id = "el_${System.currentTimeMillis()}",
            name = "Nov konstrukcijski sklop",
            area = 50.0,
            fx = 1.0,
            rsi = if (group == "roofs") 0.10 else if (group == "floors") 0.17 else 0.13,
            rse = if (group == "floors") 0.00 else 0.04,
            targetU = 0.18,
            layers = listOf(
                Layer("opeka_votla", "Votla opeka / modularni blok", 29.0, 0.480, 8.0, 900.0, 1000.0),
                Layer("eps_graf_70", "Grafitni EPS 70/80", 16.0, 0.031, 30.0, 16.0, 1450.0)
            )
        )
        when (group) {
            "walls" -> s.copy(walls = s.walls + newEl)
            "roofs" -> s.copy(roofs = s.roofs + newEl)
            else -> s.copy(floors = s.floors + newEl)
        }
    }

    fun removeElement(group: String, elIdx: Int) = mutate { s ->
        when (group) {
            "walls" -> s.copy(walls = s.walls.filterIndexed { i, _ -> i != elIdx })
            "roofs" -> s.copy(roofs = s.roofs.filterIndexed { i, _ -> i != elIdx })
            else -> s.copy(floors = s.floors.filterIndexed { i, _ -> i != elIdx })
        }
    }

    fun cloneElement(group: String, elIdx: Int) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices) {
            val orig = list[elIdx]
            val copy = orig.copy(
                id = "el_${System.currentTimeMillis()}",
                name = "${orig.name} (kopija)"
            )
            list.add(elIdx + 1, copy)
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun addLayer(group: String, elIdx: Int) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices) {
            val el = list[elIdx]
            val newLayer = Layer("eps_graf_70", "Grafitni EPS 70/80", 10.0, 0.031, 30.0, 16.0, 1450.0)
            list[elIdx] = el.copy(layers = el.layers + newLayer)
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun removeLayer(group: String, elIdx: Int, layerIdx: Int) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices && list[elIdx].layers.size > 1) {
            val el = list[elIdx]
            list[elIdx] = el.copy(layers = el.layers.filterIndexed { i, _ -> i != layerIdx })
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun moveLayer(group: String, elIdx: Int, layerIdx: Int, direction: Int) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices) {
            val el = list[elIdx]
            val layers = el.layers.toMutableList()
            val newIdx = layerIdx + direction
            if (newIdx in layers.indices) {
                val temp = layers[layerIdx]
                layers[layerIdx] = layers[newIdx]
                layers[newIdx] = temp
                list[elIdx] = el.copy(layers = layers)
            }
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun updateLayer(
        group: String,
        elIdx: Int,
        layerIdx: Int,
        d: Double? = null,
        lambda: Double? = null,
        woodPct: Double? = null,
        mu: Double? = null,
        rho: Double? = null,
        c: Double? = null
    ) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices && layerIdx in list[elIdx].layers.indices) {
            val el = list[elIdx]
            val layers = el.layers.toMutableList()
            val l = layers[layerIdx]
            layers[layerIdx] = l.copy(
                d = d ?: l.d,
                lambda = lambda ?: l.lambda,
                woodPct = woodPct ?: l.woodPct,
                mu = mu ?: l.mu,
                rho = rho ?: l.rho,
                c = c ?: l.c
            )
            list[elIdx] = el.copy(layers = layers)
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun selectMaterialForLayer(group: String, elIdx: Int, layerIdx: Int, matId: String) = mutate { s ->
        val mat = (_materials.value).find { it.id == matId } ?: return@mutate s
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices && layerIdx in list[elIdx].layers.indices) {
            val el = list[elIdx]
            val layers = el.layers.toMutableList()
            val l = layers[layerIdx]
            layers[layerIdx] = l.copy(
                matId = mat.id,
                name = mat.name,
                lambda = mat.lambda,
                mu = mat.mu,
                rho = mat.rho,
                c = mat.c,
                d = if (mat.defD < 0.8 || l.d < 0.8) mat.defD else l.d
            )
            list[elIdx] = el.copy(layers = layers)
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    fun updateRafterHelper(widthCm: Double? = null, spacingCm: Double? = null) = mutate { s ->
        s.copy(
            rafterWidthCm = widthCm ?: s.rafterWidthCm,
            rafterSpacingCm = spacingCm ?: s.rafterSpacingCm
        )
    }

    fun applyRafterPctToLayer(group: String, elIdx: Int, targetLayerIdx: Int, pct: Double) = mutate { s ->
        val list = when (group) {
            "walls" -> s.walls
            "roofs" -> s.roofs
            else -> s.floors
        }.toMutableList()

        if (elIdx in list.indices && targetLayerIdx in list[elIdx].layers.indices) {
            val el = list[elIdx]
            val layers = el.layers.toMutableList()
            layers[targetLayerIdx] = layers[targetLayerIdx].copy(woodPct = pct)
            list[elIdx] = el.copy(layers = layers, rafterTargetLayer = targetLayerIdx)
        }

        when (group) {
            "walls" -> s.copy(walls = list)
            "roofs" -> s.copy(roofs = list)
            else -> s.copy(floors = list)
        }
    }

    // Windows
    fun updateWindow(idx: Int, name: String? = null, w: Double? = null, h: Double? = null, count: Int? = null, uw: Double? = null, g: Double? = null, orient: String? = null) = mutate { s ->
        val list = s.windows.toMutableList()
        if (idx in list.indices) {
            val win = list[idx]
            list[idx] = win.copy(
                name = name ?: win.name,
                w = w ?: win.w,
                h = h ?: win.h,
                count = count ?: win.count,
                uw = uw ?: win.uw,
                g = g ?: win.g,
                orient = orient ?: win.orient
            )
        }
        s.copy(windows = list)
    }

    fun addWindow() = mutate { s ->
        s.copy(
            windows = s.windows + WindowElement(
                id = "win_${System.currentTimeMillis()}",
                name = "Novo okno",
                w = 1.2,
                h = 1.4,
                count = 1,
                uw = 0.85,
                g = 0.50,
                orient = "J"
            )
        )
    }

    fun removeWindow(idx: Int) = mutate { s ->
        s.copy(windows = s.windows.filterIndexed { i, _ -> i != idx })
    }

    // Doors
    fun updateDoor(idx: Int, name: String? = null, area: Double? = null, ud: Double? = null, fx: Double? = null) = mutate { s ->
        val list = s.doors.toMutableList()
        if (idx in list.indices) {
            val d = list[idx]
            list[idx] = d.copy(
                name = name ?: d.name,
                area = area ?: d.area,
                ud = ud ?: d.ud,
                fx = fx ?: d.fx
            )
        }
        s.copy(doors = list)
    }

    fun addDoor() = mutate { s ->
        s.copy(
            doors = s.doors + DoorElement(
                id = "d_${System.currentTimeMillis()}",
                name = "Nova vrata",
                area = 2.0,
                ud = 1.10,
                fx = 1.0
            )
        )
    }

    fun removeDoor(idx: Int) = mutate { s ->
        s.copy(doors = s.doors.filterIndexed { i, _ -> i != idx })
    }

    // Bridges
    fun toggleBridgesEnabled(enabled: Boolean) = mutate { s ->
        s.copy(bridges = s.bridges.copy(enabled = enabled))
    }

    fun toggleBridgeMode(useFlatRate: Boolean) = mutate { s ->
        s.copy(bridges = s.bridges.copy(useFlatRate = useFlatRate))
    }

    fun updateFlatDeltaU(deltaU: Double) = mutate { s ->
        s.copy(bridges = s.bridges.copy(flatDeltaU = deltaU))
    }

    fun updateBridgeItem(idx: Int, name: String? = null, length: Double? = null, psi: Double? = null) = mutate { s ->
        val items = s.bridges.items.toMutableList()
        if (idx in items.indices) {
            val itm = items[idx]
            items[idx] = itm.copy(
                name = name ?: itm.name,
                length = length ?: itm.length,
                psi = psi ?: itm.psi
            )
        }
        s.copy(bridges = s.bridges.copy(items = items))
    }

    fun addBridge(name: String = "Nov toplotni most", length: Double = 15.0, psi: Double = 0.10) = mutate { s ->
        s.copy(
            bridges = s.bridges.copy(
                items = s.bridges.items + BridgeItem(
                    id = "tb_${System.currentTimeMillis()}",
                    name = name,
                    length = length,
                    psi = psi,
                    fx = 1.0
                )
            )
        )
    }

    fun removeBridge(idx: Int) = mutate { s ->
        s.copy(bridges = s.bridges.copy(items = s.bridges.items.filterIndexed { i, _ -> i != idx }))
    }

    // Ventilation
    fun updateVentilation(
        enabled: Boolean? = null,
        volumeArea: Double? = null,
        ceilingHeight: Double? = null,
        airChanges: Double? = null,
        heatRecoveryEff: Double? = null,
        selectedUnitId: String? = null
    ) = mutate { s ->
        s.copy(
            ventilation = s.ventilation.copy(
                enabled = enabled ?: s.ventilation.enabled,
                volumeArea = volumeArea ?: s.ventilation.volumeArea,
                ceilingHeight = ceilingHeight ?: s.ventilation.ceilingHeight,
                airChanges = airChanges ?: s.ventilation.airChanges,
                heatRecoveryEff = heatRecoveryEff ?: s.ventilation.heatRecoveryEff,
                selectedUnitId = selectedUnitId ?: s.ventilation.selectedUnitId
            )
        )
    }

    fun applyVentUnit(unitId: String) {
        val u = CalculationEngine.HRV_DEVICES_DB.find { it.id == unitId } ?: return
        updateVentilation(
            enabled = true,
            selectedUnitId = unitId,
            heatRecoveryEff = u.eff
        )
    }

    // DHW & Well
    fun updateDhw(
        dhwEnabled: Boolean? = null,
        dhwSourceMode: String? = null,
        mainHpDhwCop: Double? = null,
        sanitaryHpCop: Double? = null,
        persons: Int? = null,
        litersPerPerson: Double? = null,
        tempCold: Double? = null,
        tempHot: Double? = null,
        tankLossPct: Double? = null,
        wellPumpAutoSync: Boolean? = null,
        hpThermalPowerKw: Double? = null,
        hpMachineCop: Double? = null,
        wellPumpWatts: Double? = null,
        wellWaterDeltaT: Double? = null,
        summerIndoorTemp: Double? = null,
        summerIndoorRh: Double? = null
    ) = mutate { s ->
        s.copy(
            dhwAndWell = s.dhwAndWell.copy(
                dhwEnabled = dhwEnabled ?: s.dhwAndWell.dhwEnabled,
                dhwSourceMode = dhwSourceMode ?: s.dhwAndWell.dhwSourceMode,
                mainHpDhwCop = mainHpDhwCop ?: s.dhwAndWell.mainHpDhwCop,
                sanitaryHpCop = sanitaryHpCop ?: s.dhwAndWell.sanitaryHpCop,
                persons = persons ?: s.dhwAndWell.persons,
                litersPerPerson = litersPerPerson ?: s.dhwAndWell.litersPerPerson,
                tempCold = tempCold ?: s.dhwAndWell.tempCold,
                tempHot = tempHot ?: s.dhwAndWell.tempHot,
                tankLossPct = tankLossPct ?: s.dhwAndWell.tankLossPct,
                wellPumpAutoSync = wellPumpAutoSync ?: s.dhwAndWell.wellPumpAutoSync,
                hpThermalPowerKw = hpThermalPowerKw ?: s.dhwAndWell.hpThermalPowerKw,
                hpMachineCop = hpMachineCop ?: s.dhwAndWell.hpMachineCop,
                wellPumpWatts = wellPumpWatts ?: s.dhwAndWell.wellPumpWatts,
                wellWaterDeltaT = wellWaterDeltaT ?: s.dhwAndWell.wellWaterDeltaT,
                summerIndoorTemp = summerIndoorTemp ?: s.dhwAndWell.summerIndoorTemp,
                summerIndoorRh = summerIndoorRh ?: s.dhwAndWell.summerIndoorRh
            )
        )
    }

    // HP & Fuses
    fun updateHpSettings(
        selectedHpFuelId: String? = null,
        nominalThermalKw: Double? = null,
        minModulationKw: Double? = null,
        maxThermalKwAtPeak: Double? = null,
        peakElMode: String? = null,
        manualPeakElKw: Double? = null,
        maxCompressorElKw: Double? = null,
        copAtDesignTe: Double? = null,
        auxPumpsWatts: Double? = null,
        bivalentTemp: Double? = null,
        backupHeaterKw: Double? = null,
        fuseAmps: Int? = null,
        baseHousePeakKw: Double? = null
    ) = mutate { s ->
        s.copy(
            hpSettings = s.hpSettings.copy(
                selectedHpFuelId = selectedHpFuelId ?: s.hpSettings.selectedHpFuelId,
                nominalThermalKw = nominalThermalKw ?: s.hpSettings.nominalThermalKw,
                minModulationKw = minModulationKw ?: s.hpSettings.minModulationKw,
                maxThermalKwAtPeak = maxThermalKwAtPeak ?: s.hpSettings.maxThermalKwAtPeak,
                peakElMode = peakElMode ?: s.hpSettings.peakElMode,
                manualPeakElKw = manualPeakElKw ?: s.hpSettings.manualPeakElKw,
                maxCompressorElKw = maxCompressorElKw ?: s.hpSettings.maxCompressorElKw,
                copAtDesignTe = copAtDesignTe ?: s.hpSettings.copAtDesignTe,
                auxPumpsWatts = auxPumpsWatts ?: s.hpSettings.auxPumpsWatts,
                bivalentTemp = bivalentTemp ?: s.hpSettings.bivalentTemp,
                backupHeaterKw = backupHeaterKw ?: s.hpSettings.backupHeaterKw,
                fuseAmps = fuseAmps ?: s.hpSettings.fuseAmps,
                baseHousePeakKw = baseHousePeakKw ?: s.hpSettings.baseHousePeakKw
            )
        )
    }

    // Meter & Solar
    fun updateMeterData(
        totalMeterKwh: Double? = null,
        vtKwh: Double? = null,
        mtKwh: Double? = null,
        measuredHpElKwh: Double? = null,
        measuredAcElKwh: Double? = null,
        measuredHouseholdKwh: Double? = null,
        annualBillEur: Double? = null
    ) = mutate { s ->
        s.copy(
            meterData = s.meterData.copy(
                totalMeterKwh = totalMeterKwh ?: s.meterData.totalMeterKwh,
                vtKwh = vtKwh ?: s.meterData.vtKwh,
                mtKwh = mtKwh ?: s.meterData.mtKwh,
                measuredHpElKwh = measuredHpElKwh ?: s.meterData.measuredHpElKwh,
                measuredAcElKwh = measuredAcElKwh ?: s.meterData.measuredAcElKwh,
                measuredHouseholdKwh = measuredHouseholdKwh ?: s.meterData.measuredHouseholdKwh,
                annualBillEur = annualBillEur ?: s.meterData.annualBillEur
            )
        )
    }

    fun updateMonthlyMeter(idx: Int, kwh: Double) = mutate { s ->
        val list = s.meterData.monthlyTotalKwh.toMutableList()
        if (idx in list.indices) {
            list[idx] = kwh
        }
        val sum = list.sum()
        s.copy(
            meterData = s.meterData.copy(
                monthlyTotalKwh = list,
                totalMeterKwh = sum
            )
        )
    }

    fun calibrateModelToMeter() {
        val t = _totals.value
        val targetHpEl = _state.value.meterData.measuredHpElKwh
        if (targetHpEl <= 100.0) return
        val isWaterHp = _state.value.hpSettings.selectedHpFuelId == "tc_voda"
        val hpScop = if (isWaterHp) t.dhwWell.systemScop else (_state.value.fuels.find { it.id == "tc_zrak" }?.eff ?: 3.5)
        val dhwCop = if (isWaterHp) _state.value.dhwAndWell.mainHpDhwCop else 2.6
        val remainingHeatEl = max(200.0, targetHpEl - (t.annualDhwKwh / dhwCop))
        val neededGrossHeatingKwh = (remainingHeatEl * hpScop) + t.solarGainKwh
        val newHours = min(4000.0, max(400.0, neededGrossHeatingKwh / max(0.5, t.totalKw)))
        updateBoundary(hours = newHours)
    }

    fun updateSolarAndGrid(
        pvKwp: Double? = null,
        specificYield: Double? = null,
        simLoadSource: String? = null,
        batteryCapKwh: Double? = null,
        batteryDodPct: Double? = null,
        batteryEffPct: Double? = null,
        directSelfConsPct: Double? = null,
        exportMode: String? = null,
        exportPriceEur: Double? = null,
        gridImportPriceEur: Double? = null,
        batteryInvestEur: Double? = null
    ) = mutate { s ->
        s.copy(
            solarAndGrid = s.solarAndGrid.copy(
                pvKwp = pvKwp ?: s.solarAndGrid.pvKwp,
                specificYield = specificYield ?: s.solarAndGrid.specificYield,
                simLoadSource = simLoadSource ?: s.solarAndGrid.simLoadSource,
                batteryCapKwh = batteryCapKwh ?: s.solarAndGrid.batteryCapKwh,
                batteryDodPct = batteryDodPct ?: s.solarAndGrid.batteryDodPct,
                batteryEffPct = batteryEffPct ?: s.solarAndGrid.batteryEffPct,
                directSelfConsPct = directSelfConsPct ?: s.solarAndGrid.directSelfConsPct,
                exportMode = exportMode ?: s.solarAndGrid.exportMode,
                exportPriceEur = exportPriceEur ?: s.solarAndGrid.exportPriceEur,
                gridImportPriceEur = gridImportPriceEur ?: s.solarAndGrid.gridImportPriceEur,
                batteryInvestEur = batteryInvestEur ?: s.solarAndGrid.batteryInvestEur
            )
        )
    }

    // Fuels
    fun updateFuel(idx: Int, price: Double? = null, eff: Double? = null, invest: Double? = null) = mutate { s ->
        val list = s.fuels.toMutableList()
        if (idx in list.indices) {
            val f = list[idx]
            list[idx] = f.copy(
                price = price ?: f.price,
                eff = eff ?: f.eff,
                invest = invest ?: f.invest
            )
        }
        s.copy(fuels = list)
    }

    // Rooms
    fun updateRoom(idx: Int, name: String? = null, area: Double? = null, ti: Double? = null, expFactor: Double? = null, spacing: Double? = null) = mutate { s ->
        val list = s.rooms.toMutableList()
        if (idx in list.indices) {
            val r = list[idx]
            list[idx] = r.copy(
                name = name ?: r.name,
                area = area ?: r.area,
                ti = ti ?: r.ti,
                expFactor = expFactor ?: r.expFactor,
                spacing = spacing ?: r.spacing
            )
        }
        s.copy(rooms = list)
    }

    fun addRoom() = mutate { s ->
        s.copy(
            rooms = s.rooms + RoomElement(
                id = "rm_${System.currentTimeMillis()}",
                name = "Nov prostor",
                area = 15.0,
                ti = 21.0,
                expFactor = 1.0,
                spacing = 15.0
            )
        )
    }

    fun removeRoom(idx: Int) = mutate { s ->
        s.copy(rooms = s.rooms.filterIndexed { i, _ -> i != idx })
    }

    // ROI
    fun updateRoiMeasure(idx: Int, name: String? = null, area: Double? = null, uBefore: Double? = null, uAfter: Double? = null, invest: Double? = null, subsidy: Double? = null) = mutate { s ->
        val list = s.roiMeasures.toMutableList()
        if (idx in list.indices) {
            val m = list[idx]
            list[idx] = m.copy(
                name = name ?: m.name,
                area = area ?: m.area,
                uBefore = uBefore ?: m.uBefore,
                uAfter = uAfter ?: m.uAfter,
                invest = invest ?: m.invest,
                subsidy = subsidy ?: m.subsidy
            )
        }
        s.copy(roiMeasures = list)
    }

    fun addRoiMeasure() = mutate { s ->
        s.copy(
            roiMeasures = s.roiMeasures + RoiMeasure(
                id = "m_${System.currentTimeMillis()}",
                name = "Nov energetski ukrep",
                area = 100.0,
                uBefore = 1.0,
                uAfter = 0.20,
                invest = 5000.0,
                subsidy = 0.0
            )
        )
    }

    fun removeRoiMeasure(idx: Int) = mutate { s ->
        s.copy(roiMeasures = s.roiMeasures.filterIndexed { i, _ -> i != idx })
    }

    fun updateRoiFuelId(fuelId: String) = mutate { s ->
        s.copy(roiFuelId = fuelId)
    }

    // Custom Material
    fun addCustomMaterial(mat: Material) = mutate { s ->
        s.copy(customMaterials = listOf(mat) + s.customMaterials)
    }

    fun deleteCustomMaterial(id: String) = mutate { s ->
        s.copy(customMaterials = s.customMaterials.filter { it.id != id })
    }

    fun updateBomWaste(pct: Double) = mutate { s ->
        s.copy(bomWastePct = max(0.0, pct))
    }

    fun resetToDefault() = mutate {
        DefaultStateFactory.createDefault().copy(customMaterials = it.customMaterials)
    }
}
