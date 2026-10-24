package com.example.data

import com.example.model.*

data class AppState(
    val version: String = "10.2",
    val isDarkMode: Boolean = false,
    val activeTabId: String = "walls",
    val boundary: BoundaryCondition = BoundaryCondition(),
    val walls: List<EnvelopeElement> = emptyList(),
    val roofs: List<EnvelopeElement> = emptyList(),
    val floors: List<EnvelopeElement> = emptyList(),
    val windows: List<WindowElement> = emptyList(),
    val doors: List<DoorElement> = emptyList(),
    val bridges: BridgeSettings = BridgeSettings(),
    val ventilation: VentilationSettings = VentilationSettings(),
    val dhwAndWell: DhwAndWellSettings = DhwAndWellSettings(),
    val hpSettings: HpSettings = HpSettings(),
    val meterData: MeterData = MeterData(),
    val solarAndGrid: SolarAndGridSettings = SolarAndGridSettings(),
    val hydraulics: HydraulicsSettings = HydraulicsSettings(),
    val fuels: List<Fuel> = emptyList(),
    val rooms: List<RoomElement> = emptyList(),
    val roiMeasures: List<RoiMeasure> = emptyList(),
    val roiFuelId: String = "tc_zrak",
    val customMaterials: List<Material> = emptyList(),
    val bomWastePct: Double = 5.0,
    val rafterWidthCm: Double = 10.0,
    val rafterSpacingCm: Double = 80.0,
    val scenarioA: ScenarioSnapshot? = null
)

object DefaultStateFactory {
    fun createDefault(): AppState {
        val mats = CalculationEngine.BASE_MATERIALS.associateBy { it.id }

        fun l(id: String, d: Double, woodPct: Double = 0.0): Layer {
            val m = mats[id] ?: mats["custom"]!!
            return Layer(
                matId = id,
                name = m.name,
                d = d,
                lambda = m.lambda,
                mu = m.mu,
                rho = m.rho,
                c = m.c,
                woodPct = woodPct
            )
        }

        val walls = listOf(
            EnvelopeElement(
                id = "w1",
                name = "Zunanji zid - fasada (opeka + 16 cm grafitni EPS)",
                area = 240.0,
                fx = 1.0,
                rsi = 0.13,
                rse = 0.04,
                targetU = 0.18,
                layers = listOf(
                    l("omet_notranji", 1.5),
                    l("opeka_votla", 29.0),
                    l("eps_graf_70", 16.0),
                    l("fasadno_lepilo", 0.4),
                    l("omet_fasadni", 0.2)
                )
            ),
            EnvelopeElement(
                id = "w2",
                name = "Cokel / podzidek",
                area = 25.0,
                fx = 1.0,
                rsi = 0.13,
                rse = 0.04,
                targetU = 0.25,
                layers = listOf(
                    l("ab_beton", 25.0),
                    l("xps_300", 10.0),
                    l("fasadno_lepilo", 0.5)
                )
            )
        )

        val roofs = listOf(
            EnvelopeElement(
                id = "r1",
                name = "Poševna streha / mansarda (špirovci + zračni most + Gerard Klasik)",
                area = 115.0,
                fx = 1.0,
                rsi = 0.10,
                rse = 0.04,
                targetU = 0.14,
                layers = listOf(
                    l("knauf", 1.25),
                    l("parna_zapora", 0.02),
                    l("gw_035", 14.0),
                    l("gw_035", 16.0, woodPct = 12.5),
                    l("deske_opaz", 2.4),
                    l("sek_kritina", 0.05),
                    l("zrak_prezr", 5.0),
                    l("krit_gerard", 0.2)
                ),
                rafterTargetLayer = 3
            )
        )

        val floors = listOf(
            EnvelopeElement(
                id = "f1",
                name = "Tla na terenu",
                area = 95.0,
                fx = 0.5,
                rsi = 0.17,
                rse = 0.00,
                targetU = 0.25,
                layers = listOf(
                    l("parket", 1.5),
                    l("estrih_cem", 6.0),
                    l("eps_100", 10.0),
                    l("bitumen", 0.4),
                    l("ab_beton", 15.0)
                )
            )
        )

        val windows = listOf(
            WindowElement("win1", "Troslojna PVC okna (Južna stran)", 1.4, 1.4, 5, 0.85, 0.52, "J", 1.0),
            WindowElement("win2", "Panoramska drsna stena (Jugozahod)", 2.6, 2.2, 1, 0.80, 0.50, "JV_JZ", 1.0),
            WindowElement("win3", "Okna vzhod / zahod", 1.2, 1.4, 4, 0.85, 0.52, "V_Z", 1.0),
            WindowElement("win4", "Manjša okna sever (kopalnica, hodnik)", 0.8, 1.0, 3, 0.90, 0.50, "S", 1.0)
        )

        val doors = listOf(
            DoorElement("d1", "Glavna vhodna vrata (izolirana)", 2.2, 1.10, 1.0),
            DoorElement("d2", "Podstrešne stopnice Minka Polar Extreme (114x70)", 0.80, 0.59, 0.8)
        )

        val bridges = BridgeSettings(
            enabled = true,
            useFlatRate = false,
            flatDeltaU = 0.05,
            items = listOf(
                BridgeItem("tb1", "Stik zunanji zid – temelj / cokel", 42.0, 0.15, 1.0),
                BridgeItem("tb2", "Špalete in preklade okoli oken", 58.0, 0.08, 1.0),
                BridgeItem("tb3", "Stik streha – zunanji zid (venec)", 42.0, 0.12, 1.0)
            )
        )

        val fuels = listOf(
            Fuel("tc_voda", "TČ voda–voda (s potopno črp.)", "€/kWh", 0.170, 4.54, 1.0, 14500.0),
            Fuel("tc_zrak", "TČ zrak–voda", "€/kWh", 0.170, 3.50, 1.0, 11500.0),
            Fuel("drva", "Drva (bukev, uplinjevalni)", "€/prm", 95.0, 0.85, 2100.0, 9000.0),
            Fuel("peleti", "Lesni peleti (A1)", "€/tono", 360.0, 0.90, 4800.0, 7800.0),
            Fuel("plin", "Zemeljski plin", "€/kWh", 0.095, 0.96, 1.0, 6500.0),
            Fuel("olje", "Kurilno olje (ELKO)", "€/liter", 1.180, 0.92, 10.0, 7000.0),
            Fuel("el_dir", "Direktna elektrika / IR", "€/kWh", 0.170, 1.00, 1.0, 3500.0)
        )

        val rooms = listOf(
            RoomElement("rm1", "Dnevna soba + kuhinja in jedilnica", 45.0, 21.5, 1.1, 15.0),
            RoomElement("rm2", "Kopalnica zgoraj (walk-in)", 9.0, 24.0, 1.15, 10.0),
            RoomElement("rm3", "Spalnica", 16.0, 20.0, 1.0, 15.0),
            RoomElement("rm4", "Otroška soba 1", 14.0, 21.5, 1.1, 15.0),
            RoomElement("rm5", "Otroška soba 2", 14.0, 21.5, 1.1, 15.0),
            RoomElement("rm6", "Hodnik in stopnišče", 18.0, 20.0, 0.8, 20.0)
        )

        val roiMeasures = listOf(
            RoiMeasure("m1", "Fasada 16 cm grafitni EPS 0.031 (300 m²)", 300.0, 1.15, 0.17, 15500.0, 3000.0),
            RoiMeasure("m2", "Izolacija poševne strehe / mansarde (30 cm volna)", 115.0, 0.65, 0.13, 4200.0, 1100.0),
            RoiMeasure("m3", "Menjava starih oken za troslojna PVC (Uw=0.85)", 25.0, 2.40, 0.85, 8500.0, 0.0)
        )

        return AppState(
            walls = walls,
            roofs = roofs,
            floors = floors,
            windows = windows,
            doors = doors,
            bridges = bridges,
            fuels = fuels,
            rooms = rooms,
            roiMeasures = roiMeasures
        )
    }
}
