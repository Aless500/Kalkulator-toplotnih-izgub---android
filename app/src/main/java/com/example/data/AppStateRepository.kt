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

    fun exportToJson(state: AppState): String {
        return serialize(state, indent = 2)
    }

    fun importFromJson(jsonStr: String): AppState {
        return deserialize(jsonStr)
    }

    // Local Named Projects Storage
    fun getSavedProjects(): List<Pair<String, Long>> {
        val json = prefs.getString("named_projects_meta", "{}") ?: "{}"
        val obj = JSONObject(json)
        val list = mutableListOf<Pair<String, Long>>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            list.add(Pair(k, obj.optLong(k, 0L)))
        }
        return list.sortedByDescending { it.second }
    }

    fun saveNamedProject(name: String, state: AppState) {
        val jsonStr = serialize(state)
        prefs.edit().putString("proj_$name", jsonStr).apply()

        // Update meta
        val metaJson = prefs.getString("named_projects_meta", "{}") ?: "{}"
        val meta = JSONObject(metaJson)
        meta.put(name, System.currentTimeMillis())
        prefs.edit().putString("named_projects_meta", meta.toString()).apply()
    }

    fun loadNamedProject(name: String): AppState? {
        val jsonStr = prefs.getString("proj_$name", null) ?: return null
        return try {
            deserialize(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteNamedProject(name: String) {
        prefs.edit().remove("proj_$name").apply()
        val metaJson = prefs.getString("named_projects_meta", "{}") ?: "{}"
        val meta = JSONObject(metaJson)
        meta.remove(name)
        prefs.edit().putString("named_projects_meta", meta.toString()).apply()
    }

    private fun serialize(state: AppState, indent: Int = 0): String {
        val root = JSONObject()
        root.put("version", state.version)
        root.put("isDarkMode", state.isDarkMode)
        root.put("activeTabId", state.activeTabId)
        root.put("bomWastePct", state.bomWastePct)
        root.put("roiFuelId", state.roiFuelId)

        val rafterObj = JSONObject()
        rafterObj.put("widthCm", state.rafterWidthCm)
        rafterObj.put("spacingCm", state.rafterSpacingCm)
        root.put("rafterHelper", rafterObj)

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

        // DHW & Well
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

        // Meter Data
        val mdObj = JSONObject()
        mdObj.put("enabled", state.meterData.enabled)
        mdObj.put("totalMeterKwh", state.meterData.totalMeterKwh)
        mdObj.put("vtKwh", state.meterData.vtKwh)
        mdObj.put("mtKwh", state.meterData.mtKwh)
        mdObj.put("measuredHpElKwh", state.meterData.measuredHpElKwh)
        mdObj.put("measuredAcElKwh", state.meterData.measuredAcElKwh)
        mdObj.put("measuredHouseholdKwh", state.meterData.measuredHouseholdKwh)
        mdObj.put("annualBillEur", state.meterData.annualBillEur)
        val monthlyArr = JSONArray()
        state.meterData.monthlyTotalKwh.forEach { monthlyArr.put(it) }
        mdObj.put("monthlyTotalKwh", monthlyArr)
        root.put("meterData", mdObj)

        // Solar and Grid
        val sgObj = JSONObject()
        sgObj.put("pvKwp", state.solarAndGrid.pvKwp)
        sgObj.put("specificYield", state.solarAndGrid.specificYield)
        sgObj.put("simLoadSource", state.solarAndGrid.simLoadSource)
        sgObj.put("batteryCapKwh", state.solarAndGrid.batteryCapKwh)
        sgObj.put("batteryDodPct", state.solarAndGrid.batteryDodPct)
        sgObj.put("batteryEffPct", state.solarAndGrid.batteryEffPct)
        sgObj.put("directSelfConsPct", state.solarAndGrid.directSelfConsPct)
        sgObj.put("exportMode", state.solarAndGrid.exportMode)
        sgObj.put("exportPriceEur", state.solarAndGrid.exportPriceEur)
        sgObj.put("gridImportPriceEur", state.solarAndGrid.gridImportPriceEur)
        sgObj.put("batteryInvestEur", state.solarAndGrid.batteryInvestEur)
        root.put("solarAndGrid", sgObj)

        // Hydraulics
        val hydObj = JSONObject()
        hydObj.put("floorDeltaT", state.hydraulics.floorDeltaT)
        root.put("hydraulics", hydObj)

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

        // ROI
        val roiObj = JSONObject()
        roiObj.put("selectedFuelId", state.roiFuelId)
        val roiArr = JSONArray()
        state.roiMeasures.forEach { m ->
            val mo = JSONObject()
            mo.put("id", m.id)
            mo.put("name", m.name)
            mo.put("area", m.area)
            mo.put("uBefore", m.uBefore)
            mo.put("uAfter", m.uAfter)
            mo.put("invest", m.invest)
            mo.put("subsidy", m.subsidy)
            roiArr.put(mo)
        }
        roiObj.put("measures", roiArr)
        root.put("roi", roiObj)

        // Custom Materials
        val matsArr = JSONArray()
        state.customMaterials.forEach { cm ->
            val mo = JSONObject()
            mo.put("id", cm.id)
            mo.put("group", cm.group)
            mo.put("name", cm.name)
            mo.put("defD", cm.defD)
            mo.put("lambda", cm.lambda)
            mo.put("mu", cm.mu)
            mo.put("rho", cm.rho)
            mo.put("c", cm.c)
            mo.put("color", cm.colorHex)
            mo.put("ventilated", cm.ventilated)
            matsArr.put(mo)
        }
        root.put("customMaterials", matsArr)

        // Scenario A
        state.scenarioA?.let { sa ->
            val sObj = JSONObject()
            sObj.put("totalW", sa.totalW)
            sObj.put("annualKwh", sa.annualKwh)
            sObj.put("eur", sa.eur)
            root.put("scenarioA", sObj)
        }

        return if (indent > 0) root.toString(indent) else root.toString()
    }

    private fun deserialize(jsonStr: String): AppState {
        val def = DefaultStateFactory.createDefault()
        val root = JSONObject(jsonStr)

        val isDarkMode = root.optBoolean("isDarkMode", def.isDarkMode)
        val activeTabId = root.optString("activeTabId", def.activeTabId)
        val bomWastePct = root.optDouble("bomWastePct", def.bomWastePct)

        // Rafter helper
        val rafterObj = root.optJSONObject("rafterHelper")
        val rafterWidthCm = rafterObj?.optDouble("widthCm", def.rafterWidthCm)
            ?: root.optDouble("rafterWidthCm", def.rafterWidthCm)
        val rafterSpacingCm = rafterObj?.optDouble("spacingCm", def.rafterSpacingCm)
            ?: root.optDouble("rafterSpacingCm", def.rafterSpacingCm)

        // Boundary
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

        // Windows
        val winList = mutableListOf<WindowElement>()
        root.optJSONArray("windows")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                winList.add(
                    WindowElement(
                        id = o.optString("id", "win_$i"),
                        name = o.optString("name", "Okno"),
                        w = o.optDouble("w", 1.2),
                        h = o.optDouble("h", 1.4),
                        count = o.optInt("count", 1),
                        uw = o.optDouble("uw", 0.85),
                        g = o.optDouble("g", 0.50),
                        orient = o.optString("orient", "J"),
                        fx = o.optDouble("fx", 1.0)
                    )
                )
            }
        }
        val windows = if (winList.isNotEmpty()) winList else def.windows

        // Doors
        val doorList = mutableListOf<DoorElement>()
        root.optJSONArray("doors")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                doorList.add(
                    DoorElement(
                        id = o.optString("id", "d_$i"),
                        name = o.optString("name", "Vrata"),
                        area = o.optDouble("area", 2.0),
                        ud = o.optDouble("ud", 1.1),
                        fx = o.optDouble("fx", 1.0)
                    )
                )
            }
        }
        val doors = if (doorList.isNotEmpty()) doorList else def.doors

        // Bridges
        val bridges = root.optJSONObject("bridges")?.let { bo ->
            val items = mutableListOf<BridgeItem>()
            bo.optJSONArray("items")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    items.add(
                        BridgeItem(
                            id = o.optString("id", "tb_$i"),
                            name = o.optString("name", "Most"),
                            length = o.optDouble("length", 10.0),
                            psi = o.optDouble("psi", 0.10),
                            fx = o.optDouble("fx", 1.0)
                        )
                    )
                }
            }
            BridgeSettings(
                enabled = bo.optBoolean("enabled", def.bridges.enabled),
                useFlatRate = bo.optBoolean("useFlatRate", def.bridges.useFlatRate),
                flatDeltaU = bo.optDouble("flatDeltaU", def.bridges.flatDeltaU),
                items = if (items.isNotEmpty()) items else def.bridges.items
            )
        } ?: def.bridges

        // Ventilation
        val ventilation = root.optJSONObject("ventilation")?.let { vo ->
            VentilationSettings(
                enabled = vo.optBoolean("enabled", def.ventilation.enabled),
                volumeArea = vo.optDouble("volumeArea", def.ventilation.volumeArea),
                ceilingHeight = vo.optDouble("ceilingHeight", def.ventilation.ceilingHeight),
                airChanges = vo.optDouble("airChanges", def.ventilation.airChanges),
                heatRecoveryEff = vo.optDouble("heatRecoveryEff", def.ventilation.heatRecoveryEff),
                selectedUnitId = vo.optString("selectedUnitId", def.ventilation.selectedUnitId)
            )
        } ?: def.ventilation

        // DHW & Well
        val dhwAndWell = root.optJSONObject("dhwAndWell")?.let { dwo ->
            DhwAndWellSettings(
                dhwEnabled = dwo.optBoolean("dhwEnabled", def.dhwAndWell.dhwEnabled),
                dhwSourceMode = dwo.optString("dhwSourceMode", def.dhwAndWell.dhwSourceMode),
                mainHpDhwCop = dwo.optDouble("mainHpDhwCop", def.dhwAndWell.mainHpDhwCop),
                sanitaryHpCop = dwo.optDouble("sanitaryHpCop", def.dhwAndWell.sanitaryHpCop),
                persons = dwo.optInt("persons", def.dhwAndWell.persons),
                litersPerPerson = dwo.optDouble("litersPerPerson", def.dhwAndWell.litersPerPerson),
                tempCold = dwo.optDouble("tempCold", def.dhwAndWell.tempCold),
                tempHot = dwo.optDouble("tempHot", def.dhwAndWell.tempHot),
                tankLossPct = dwo.optDouble("tankLossPct", def.dhwAndWell.tankLossPct),
                wellPumpAutoSync = dwo.optBoolean("wellPumpAutoSync", def.dhwAndWell.wellPumpAutoSync),
                hpThermalPowerKw = dwo.optDouble("hpThermalPowerKw", def.dhwAndWell.hpThermalPowerKw),
                hpMachineCop = dwo.optDouble("hpMachineCop", def.dhwAndWell.hpMachineCop),
                wellPumpWatts = dwo.optDouble("wellPumpWatts", def.dhwAndWell.wellPumpWatts),
                wellWaterDeltaT = dwo.optDouble("wellWaterDeltaT", def.dhwAndWell.wellWaterDeltaT),
                summerIndoorTemp = dwo.optDouble("summerIndoorTemp", def.dhwAndWell.summerIndoorTemp),
                summerIndoorRh = dwo.optDouble("summerIndoorRh", def.dhwAndWell.summerIndoorRh)
            )
        } ?: def.dhwAndWell

        // HP Settings
        val hpSettings = root.optJSONObject("hpSettings")?.let { hpo ->
            HpSettings(
                selectedHpFuelId = hpo.optString("selectedHpFuelId", def.hpSettings.selectedHpFuelId),
                nominalThermalKw = hpo.optDouble("nominalThermalKw", def.hpSettings.nominalThermalKw),
                minModulationKw = hpo.optDouble("minModulationKw", def.hpSettings.minModulationKw),
                maxThermalKwAtPeak = hpo.optDouble("maxThermalKwAtPeak", def.hpSettings.maxThermalKwAtPeak),
                peakElMode = hpo.optString("peakElMode", def.hpSettings.peakElMode),
                manualPeakElKw = hpo.optDouble("manualPeakElKw", def.hpSettings.manualPeakElKw),
                maxCompressorElKw = hpo.optDouble("maxCompressorElKw", def.hpSettings.maxCompressorElKw),
                copAtDesignTe = hpo.optDouble("copAtDesignTe", def.hpSettings.copAtDesignTe),
                auxPumpsWatts = hpo.optDouble("auxPumpsWatts", def.hpSettings.auxPumpsWatts),
                bivalentTemp = hpo.optDouble("bivalentTemp", def.hpSettings.bivalentTemp),
                backupHeaterKw = hpo.optDouble("backupHeaterKw", def.hpSettings.backupHeaterKw),
                fuseAmps = hpo.optInt("fuseAmps", def.hpSettings.fuseAmps),
                baseHousePeakKw = hpo.optDouble("baseHousePeakKw", def.hpSettings.baseHousePeakKw)
            )
        } ?: def.hpSettings

        // Meter Data
        val meterData = root.optJSONObject("meterData")?.let { mdo ->
            val monthly = mutableListOf<Double>()
            mdo.optJSONArray("monthlyTotalKwh")?.let { arr ->
                for (i in 0 until arr.length()) monthly.add(arr.optDouble(i, 0.0))
            }
            MeterData(
                enabled = mdo.optBoolean("enabled", def.meterData.enabled),
                totalMeterKwh = mdo.optDouble("totalMeterKwh", def.meterData.totalMeterKwh),
                vtKwh = mdo.optDouble("vtKwh", def.meterData.vtKwh),
                mtKwh = mdo.optDouble("mtKwh", def.meterData.mtKwh),
                measuredHpElKwh = mdo.optDouble("measuredHpElKwh", def.meterData.measuredHpElKwh),
                measuredAcElKwh = mdo.optDouble("measuredAcElKwh", def.meterData.measuredAcElKwh),
                measuredHouseholdKwh = mdo.optDouble("measuredHouseholdKwh", def.meterData.measuredHouseholdKwh),
                annualBillEur = mdo.optDouble("annualBillEur", def.meterData.annualBillEur),
                monthlyTotalKwh = if (monthly.size == 12) monthly else def.meterData.monthlyTotalKwh
            )
        } ?: def.meterData

        // Solar and Grid
        val solarAndGrid = root.optJSONObject("solarAndGrid")?.let { sgo ->
            SolarAndGridSettings(
                pvKwp = sgo.optDouble("pvKwp", def.solarAndGrid.pvKwp),
                specificYield = sgo.optDouble("specificYield", def.solarAndGrid.specificYield),
                simLoadSource = sgo.optString("simLoadSource", def.solarAndGrid.simLoadSource),
                batteryCapKwh = sgo.optDouble("batteryCapKwh", def.solarAndGrid.batteryCapKwh),
                batteryDodPct = sgo.optDouble("batteryDodPct", def.solarAndGrid.batteryDodPct),
                batteryEffPct = sgo.optDouble("batteryEffPct", def.solarAndGrid.batteryEffPct),
                directSelfConsPct = sgo.optDouble("directSelfConsPct", def.solarAndGrid.directSelfConsPct),
                exportMode = sgo.optString("exportMode", def.solarAndGrid.exportMode),
                exportPriceEur = sgo.optDouble("exportPriceEur", def.solarAndGrid.exportPriceEur),
                gridImportPriceEur = sgo.optDouble("gridImportPriceEur", def.solarAndGrid.gridImportPriceEur),
                batteryInvestEur = sgo.optDouble("batteryInvestEur", def.solarAndGrid.batteryInvestEur)
            )
        } ?: def.solarAndGrid

        // Hydraulics
        val hydraulics = root.optJSONObject("hydraulics")?.let { hyo ->
            HydraulicsSettings(floorDeltaT = hyo.optDouble("floorDeltaT", def.hydraulics.floorDeltaT))
        } ?: def.hydraulics

        // Fuels
        val fuelList = mutableListOf<Fuel>()
        root.optJSONArray("fuels")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                fuelList.add(
                    Fuel(
                        id = o.optString("id", "f_$i"),
                        name = o.optString("name", "Energent"),
                        unit = o.optString("unit", "€"),
                        price = o.optDouble("price", 0.17),
                        eff = o.optDouble("eff", 1.0),
                        energyPerUnit = o.optDouble("energyPerUnit", 1.0),
                        invest = o.optDouble("invest", 0.0)
                    )
                )
            }
        }
        val fuels = if (fuelList.isNotEmpty()) fuelList else def.fuels

        // Rooms
        val roomList = mutableListOf<RoomElement>()
        root.optJSONArray("rooms")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                roomList.add(
                    RoomElement(
                        id = o.optString("id", "rm_$i"),
                        name = o.optString("name", "Soba"),
                        area = o.optDouble("area", 15.0),
                        ti = o.optDouble("ti", 21.0),
                        expFactor = o.optDouble("expFactor", 1.0),
                        spacing = o.optDouble("spacing", 15.0)
                    )
                )
            }
        }
        val rooms = if (roomList.isNotEmpty()) roomList else def.rooms

        // ROI
        var roiFuelId = root.optString("roiFuelId", def.roiFuelId)
        val roiMeasureList = mutableListOf<RoiMeasure>()
        val roiObj = root.optJSONObject("roi")
        if (roiObj != null) {
            roiFuelId = roiObj.optString("selectedFuelId", roiFuelId)
            roiObj.optJSONArray("measures")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    roiMeasureList.add(
                        RoiMeasure(
                            id = o.optString("id", "m_$i"),
                            name = o.optString("name", "Ukrep"),
                            area = o.optDouble("area", 100.0),
                            uBefore = o.optDouble("uBefore", 1.0),
                            uAfter = o.optDouble("uAfter", 0.2),
                            invest = o.optDouble("invest", 5000.0),
                            subsidy = o.optDouble("subsidy", 0.0)
                        )
                    )
                }
            }
        } else {
            root.optJSONArray("roiMeasures")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    roiMeasureList.add(
                        RoiMeasure(
                            id = o.optString("id", "m_$i"),
                            name = o.optString("name", "Ukrep"),
                            area = o.optDouble("area", 100.0),
                            uBefore = o.optDouble("uBefore", 1.0),
                            uAfter = o.optDouble("uAfter", 0.2),
                            invest = o.optDouble("invest", 5000.0),
                            subsidy = o.optDouble("subsidy", 0.0)
                        )
                    )
                }
            }
        }
        val roiMeasures = if (roiMeasureList.isNotEmpty()) roiMeasureList else def.roiMeasures

        // Custom Materials
        val customMats = mutableListOf<Material>()
        root.optJSONArray("customMaterials")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                customMats.add(
                    Material(
                        id = o.optString("id", "usr_$i"),
                        group = o.optString("group", "Moji materiali"),
                        name = o.optString("name", "Material"),
                        defD = o.optDouble("defD", 10.0),
                        lambda = o.optDouble("lambda", 0.035),
                        mu = o.optDouble("mu", 30.0),
                        rho = o.optDouble("rho", 50.0),
                        c = o.optDouble("c", 1400.0),
                        colorHex = o.optString("color", "#0284c7"),
                        ventilated = o.optBoolean("ventilated", false)
                    )
                )
            }
        }

        // Scenario A
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
            windows = windows,
            doors = doors,
            bridges = bridges,
            ventilation = ventilation,
            dhwAndWell = dhwAndWell,
            hpSettings = hpSettings,
            meterData = meterData,
            solarAndGrid = solarAndGrid,
            hydraulics = hydraulics,
            fuels = fuels,
            rooms = rooms,
            roiMeasures = roiMeasures,
            customMaterials = customMats,
            scenarioA = scenarioA
        )
    }
}
