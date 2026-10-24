package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

class AppStateRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("heat_loss_calc_prefs", Context.MODE_PRIVATE)

    fun loadState(): AppState {
        val jsonStr = prefs.getString("saved_state_v10", null) ?: return DefaultStateFactory.createDefault()
        return try {
            deserialize(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            DefaultStateFactory.createDefault()
        }
    }

    fun saveState(state: AppState) {
        try {
            val jsonStr = serialize(state)
            prefs.edit().putString("saved_state_v10", jsonStr).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serialize(state: AppState): String {
        val root = JSONObject()
        root.put("version", state.version)
        root.put("isDarkMode", state.isDarkMode)
        root.put("activeTabId", state.activeTabId)
        root.put("bomWastePct", state.bomWastePct)
        root.put("rafterWidthCm", state.rafterWidthCm)
        root.put("rafterSpacingCm", state.rafterSpacingCm)
        root.put("roiFuelId", state.roiFuelId)

        // Boundary
        val b = JSONObject()
        b.put("ti", state.boundary.ti)
        b.put("te", state.boundary.te)
        b.put("phiI", state.boundary.phiI)
        b.put("phiE", state.boundary.phiE)
        b.put("heatedArea", state.boundary.heatedArea)
        b.put("hours", state.boundary.hours)
        root.put("boundary", b)

        fun serializeElements(list: List<EnvelopeElement>): JSONArray {
            val arr = JSONArray()
            list.forEach { el ->
                val obj = JSONObject()
                obj.put("id", el.id)
                obj.put("name", el.name)
                obj.put("area", el.area)
                obj.put("fx", el.fx)
                obj.put("rsi", el.rsi)
                obj.put("rse", el.rse)
                obj.put("targetU", el.targetU)
                obj.put("rafterTargetLayer", el.rafterTargetLayer)
                val layersArr = JSONArray()
                el.layers.forEach { l ->
                    val lObj = JSONObject()
                    lObj.put("matId", l.matId)
                    lObj.put("name", l.name)
                    lObj.put("d", l.d)
                    lObj.put("lambda", l.lambda)
                    lObj.put("mu", l.mu)
                    lObj.put("rho", l.rho)
                    lObj.put("c", l.c)
                    lObj.put("woodPct", l.woodPct)
                    layersArr.put(lObj)
                }
                obj.put("layers", layersArr)
                arr.put(obj)
            }
            return arr
        }

        root.put("walls", serializeElements(state.walls))
        root.put("roofs", serializeElements(state.roofs))
        root.put("floors", serializeElements(state.floors))

        // Windows
        val winArr = JSONArray()
        state.windows.forEach { w ->
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("name", w.name)
            obj.put("w", w.w)
            obj.put("h", w.h)
            obj.put("count", w.count)
            obj.put("uw", w.uw)
            obj.put("g", w.g)
            obj.put("orient", w.orient)
            obj.put("fx", w.fx)
            winArr.put(obj)
        }
        root.put("windows", winArr)

        // Doors
        val doorsArr = JSONArray()
        state.doors.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("name", d.name)
            obj.put("area", d.area)
            obj.put("ud", d.ud)
            obj.put("fx", d.fx)
            doorsArr.put(obj)
        }
        root.put("doors", doorsArr)

        // Bridges
        val brObj = JSONObject()
        brObj.put("enabled", state.bridges.enabled)
        brObj.put("useFlatRate", state.bridges.useFlatRate)
        brObj.put("flatDeltaU", state.bridges.flatDeltaU)
        val brItems = JSONArray()
        state.bridges.items.forEach { itm ->
            val o = JSONObject()
            o.put("id", itm.id)
            o.put("name", itm.name)
            o.put("length", itm.length)
            o.put("psi", itm.psi)
            o.put("fx", itm.fx)
            brItems.put(o)
        }
        brObj.put("items", brItems)
        root.put("bridges", brObj)

        // Ventilation
        val vObj = JSONObject()
        vObj.put("enabled", state.ventilation.enabled)
        vObj.put("volumeArea", state.ventilation.volumeArea)
        vObj.put("ceilingHeight", state.ventilation.ceilingHeight)
        vObj.put("airChanges", state.ventilation.airChanges)
        vObj.put("heatRecoveryEff", state.ventilation.heatRecoveryEff)
        vObj.put("selectedUnitId", state.ventilation.selectedUnitId)
        root.put("ventilation", vObj)

        // DHW
        val dwObj = JSONObject()
        dwObj.put("dhwEnabled", state.dhwAndWell.dhwEnabled)
        dwObj.put("dhwSourceMode", state.dhwAndWell.dhwSourceMode)
        dwObj.put("mainHpDhwCop", state.dhwAndWell.mainHpDhwCop)
        dwObj.put("sanitaryHpCop", state.dhwAndWell.sanitaryHpCop)
        dwObj.put("persons", state.dhwAndWell.persons)
        dwObj.put("litersPerPerson", state.dhwAndWell.litersPerPerson)
        dwObj.put("tempCold", state.dhwAndWell.tempCold)
        dwObj.put("tempHot", state.dhwAndWell.tempHot)
        dwObj.put("tankLossPct", state.dhwAndWell.tankLossPct)
        dwObj.put("wellPumpAutoSync", state.dhwAndWell.wellPumpAutoSync)
        dwObj.put("hpThermalPowerKw", state.dhwAndWell.hpThermalPowerKw)
        dwObj.put("hpMachineCop", state.dhwAndWell.hpMachineCop)
        dwObj.put("wellPumpWatts", state.dhwAndWell.wellPumpWatts)
        dwObj.put("wellWaterDeltaT", state.dhwAndWell.wellWaterDeltaT)
        dwObj.put("summerIndoorTemp", state.dhwAndWell.summerIndoorTemp)
        dwObj.put("summerIndoorRh", state.dhwAndWell.summerIndoorRh)
        root.put("dhwAndWell", dwObj)

        // HP Settings
        val hpObj = JSONObject()
        hpObj.put("selectedHpFuelId", state.hpSettings.selectedHpFuelId)
        hpObj.put("nominalThermalKw", state.hpSettings.nominalThermalKw)
        hpObj.put("minModulationKw", state.hpSettings.minModulationKw)
        hpObj.put("maxThermalKwAtPeak", state.hpSettings.maxThermalKwAtPeak)
        hpObj.put("peakElMode", state.hpSettings.peakElMode)
        hpObj.put("manualPeakElKw", state.hpSettings.manualPeakElKw)
        hpObj.put("maxCompressorElKw", state.hpSettings.maxCompressorElKw)
        hpObj.put("copAtDesignTe", state.hpSettings.copAtDesignTe)
        hpObj.put("auxPumpsWatts", state.hpSettings.auxPumpsWatts)
        hpObj.put("bivalentTemp", state.hpSettings.bivalentTemp)
        hpObj.put("backupHeaterKw", state.hpSettings.backupHeaterKw)
        hpObj.put("fuseAmps", state.hpSettings.fuseAmps)
        hpObj.put("baseHousePeakKw", state.hpSettings.baseHousePeakKw)
        root.put("hpSettings", hpObj)

        // Fuels
        val fuelArr = JSONArray()
        state.fuels.forEach { f ->
            val fo = JSONObject()
            fo.put("id", f.id)
            fo.put("name", f.name)
            fo.put("unit", f.unit)
            fo.put("price", f.price)
            fo.put("eff", f.eff)
            fo.put("energyPerUnit", f.energyPerUnit)
            fo.put("invest", f.invest)
            fuelArr.put(fo)
        }
        root.put("fuels", fuelArr)

        // Rooms
        val rmArr = JSONArray()
        state.rooms.forEach { rm ->
            val ro = JSONObject()
            ro.put("id", rm.id)
            ro.put("name", rm.name)
            ro.put("area", rm.area)
            ro.put("ti", rm.ti)
            ro.put("expFactor", rm.expFactor)
            ro.put("spacing", rm.spacing)
            rmArr.put(ro)
        }
        root.put("rooms", rmArr)

        // Scenario A
        state.scenarioA?.let { sa ->
            val sObj = JSONObject()
            sObj.put("totalW", sa.totalW)
            sObj.put("annualKwh", sa.annualKwh)
            sObj.put("eur", sa.eur)
            root.put("scenarioA", sObj)
        }

        return root.toString()
    }

    private fun deserialize(jsonStr: String): AppState {
        val def = DefaultStateFactory.createDefault()
        val root = JSONObject(jsonStr)

        val isDarkMode = root.optBoolean("isDarkMode", def.isDarkMode)
        val activeTabId = root.optString("activeTabId", def.activeTabId)
        val bomWastePct = root.optDouble("bomWastePct", def.bomWastePct)
        val rafterWidthCm = root.optDouble("rafterWidthCm", def.rafterWidthCm)
        val rafterSpacingCm = root.optDouble("rafterSpacingCm", def.rafterSpacingCm)
        val roiFuelId = root.optString("roiFuelId", def.roiFuelId)

        val bObj = root.optJSONObject("boundary")
        val boundary = if (bObj != null) {
            BoundaryCondition(
                ti = bObj.optDouble("ti", def.boundary.ti),
                te = bObj.optDouble("te", def.boundary.te),
                phiI = bObj.optDouble("phiI", def.boundary.phiI),
                phiE = bObj.optDouble("phiE", def.boundary.phiE),
                heatedArea = bObj.optDouble("heatedArea", def.boundary.heatedArea),
                hours = bObj.optDouble("hours", def.boundary.hours)
            )
        } else def.boundary

        fun parseElements(key: String, fallback: List<EnvelopeElement>): List<EnvelopeElement> {
            val arr = root.optJSONArray(key) ?: return fallback
            val list = mutableListOf<EnvelopeElement>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val layersArr = o.optJSONArray("layers") ?: JSONArray()
                val layers = mutableListOf<Layer>()
                for (j in 0 until layersArr.length()) {
                    val lo = layersArr.getJSONObject(j)
                    layers.add(
                        Layer(
                            matId = lo.optString("matId", "custom"),
                            name = lo.optString("name", "Sloj"),
                            d = lo.optDouble("d", 5.0),
                            lambda = lo.optDouble("lambda", 0.04),
                            mu = lo.optDouble("mu", 1.0),
                            rho = lo.optDouble("rho", 500.0),
                            c = lo.optDouble("c", 1000.0),
                            woodPct = lo.optDouble("woodPct", 0.0)
                        )
                    )
                }
                list.add(
                    EnvelopeElement(
                        id = o.optString("id", "el_$i"),
                        name = o.optString("name", "Sklop"),
                        area = o.optDouble("area", 10.0),
                        fx = o.optDouble("fx", 1.0),
                        rsi = o.optDouble("rsi", 0.13),
                        rse = o.optDouble("rse", 0.04),
                        targetU = o.optDouble("targetU", 0.18),
                        layers = layers,
                        rafterTargetLayer = o.optInt("rafterTargetLayer", 0)
                    )
                )
            }
            return if (list.isNotEmpty()) list else fallback
        }

        val walls = parseElements("walls", def.walls)
        val roofs = parseElements("roofs", def.roofs)
        val floors = parseElements("floors", def.floors)

        val scenarioA = root.optJSONObject("scenarioA")?.let {
            ScenarioSnapshot(
                totalW = it.optDouble("totalW", 0.0),
                annualKwh = it.optDouble("annualKwh", 0.0),
                eur = it.optDouble("eur", 0.0)
            )
        }

        return def.copy(
            isDarkMode = isDarkMode,
            activeTabId = activeTabId,
            bomWastePct = bomWastePct,
            rafterWidthCm = rafterWidthCm,
            rafterSpacingCm = rafterSpacingCm,
            roiFuelId = roiFuelId,
            boundary = boundary,
            walls = walls,
            roofs = roofs,
            floors = floors,
            scenarioA = scenarioA
        )
    }
}
