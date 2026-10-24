package com.example.model

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.ceil

object CalculationEngine {

    val STANDARD_LIMITS = mapOf(
        "walls" to Pair(0.18, 0.15), // PURES-3, Passive
        "roofs" to Pair(0.14, 0.10),
        "floors" to Pair(0.25, 0.15)
    )

    data class CityPreset(val name: String, val te: Double)

    val SLOVENE_CITIES = listOf(
        CityPreset("Murska Sobota / Prekmurje", -13.0),
        CityPreset("Maribor / Podravska", -13.0),
        CityPreset("Ljubljana / Osrednja SLO", -13.0),
        CityPreset("Celje / Savinjska", -13.0),
        CityPreset("Kranj / Gorenjska", -15.0),
        CityPreset("Kranjska Gora / Alp. svet", -18.0),
        CityPreset("Novo mesto / Dolenjska", -14.0),
        CityPreset("Nova Gorica / Goriška", -9.0),
        CityPreset("Koper / Obala", -5.0),
        CityPreset("Postojna / Notranjska", -15.0)
    )

    data class PresetPsiBridge(val name: String, val psi: Double)

    val PRESET_PSI_BRIDGES = listOf(
        PresetPsiBridge("Stik talne plošče in cokla (izolirano z XPS)", 0.10),
        PresetPsiBridge("Stik talne plošče in cokla (neizolirano / slabo)", 0.35),
        PresetPsiBridge("Okenska špaleta – RAL montaža s toplotnim okvirjem", 0.04),
        PresetPsiBridge("Okenska špaleta – Klasična vgradnja ob oknu", 0.12),
        PresetPsiBridge("Stik stene in ravne strehe / venca", 0.12),
        PresetPsiBridge("Stik stene in poševne strehe (kapni venec)", 0.08),
        PresetPsiBridge("Vogal zgradbe (zunanji vogal 90°)", -0.05),
        PresetPsiBridge("Vmesna stropna plošča preboja skozi fasadni ovoj", 0.06)
    )

    data class SolarOrientation(val key: String, val label: String, val kwhM2: Double)

    val SOLAR_WINTER_IRR = mapOf(
        "J" to SolarOrientation("J", "Jug (J)", 340.0),
        "JV_JZ" to SolarOrientation("JV_JZ", "Jugovzhod / Jugozahod", 270.0),
        "V_Z" to SolarOrientation("V_Z", "Vzhod / Zahod (V/Z)", 195.0),
        "S" to SolarOrientation("S", "Sever (S)", 90.0),
        "STREHA" to SolarOrientation("STREHA", "Strešno okno (naklon)", 290.0)
    )

    data class HrvUnit(
        val id: String,
        val name: String,
        val eff: Double,
        val flow: Int,
        val spi: Double,
        val area: String,
        val type: String
    )

    val HRV_DEVICES_DB = listOf(
        HrvUnit("zehnder_q350", "Zehnder ComfoAir Q350 TR (standard)", 90.0, 350, 0.24, "120–220 m²", "Protitočni"),
        HrvUnit("zehnder_q350_erv", "Zehnder ComfoAir Q350 ERV (entalpijski)", 86.0, 350, 0.24, "120–220 m²", "Entalpijski"),
        HrvUnit("zehnder_q450", "Zehnder ComfoAir Q450 TR", 89.0, 450, 0.26, "180–280 m²", "Protitočni"),
        HrvUnit("paul_novus300", "Paul Novus 300 (PHI top izkoristek)", 93.0, 300, 0.24, "100–200 m²", "Protitočni"),
        HrvUnit("paul_novus300_f", "Paul Novus F 300 (entalpijski)", 89.0, 300, 0.25, "100–200 m²", "Entalpijski"),
        HrvUnit("helios_kwl300", "Helios KWL EC 300 W", 88.0, 300, 0.29, "110–190 m²", "Protitočni"),
        HrvUnit("helios_kwl340_et", "Helios KWL 340 D (entalpijski)", 85.0, 340, 0.30, "120–220 m²", "Entalpijski"),
        HrvUnit("pichler_lg350", "Pichler LG 350", 88.0, 350, 0.28, "130–230 m²", "Protitočni"),
        HrvUnit("dantherm_hcv400", "Dantherm HCV 400 P2", 87.0, 360, 0.29, "120–230 m²", "Protitočni"),
        HrvUnit("brink_flair325", "Brink Flair 325", 91.0, 325, 0.21, "110–210 m²", "Protitočni"),
        HrvUnit("mitsubishi_lossnay", "Mitsubishi Lossnay VL-350CZPVU", 85.0, 350, 0.27, "120–210 m²", "Protitočni")
    )

    data class MonthData(
        val m: String,
        val days: Int,
        val pvShare: Double,
        val heatShare: Double,
        val acShare: Double,
        val activeCycleFactor: Double
    )

    val MONTHS_DATA = listOf(
        MonthData("Jan", 31, 0.035, 0.200, 0.00, 0.65),
        MonthData("Feb", 28, 0.055, 0.165, 0.00, 0.72),
        MonthData("Mar", 31, 0.090, 0.130, 0.00, 0.82),
        MonthData("Apr", 30, 0.115, 0.070, 0.00, 0.88),
        MonthData("Maj", 31, 0.130, 0.015, 0.05, 0.90),
        MonthData("Jun", 30, 0.135, 0.000, 0.25, 0.92),
        MonthData("Jul", 31, 0.140, 0.000, 0.40, 0.94),
        MonthData("Avg", 31, 0.125, 0.000, 0.30, 0.92),
        MonthData("Sep", 30, 0.085, 0.015, 0.00, 0.86),
        MonthData("Okt", 31, 0.050, 0.075, 0.00, 0.76),
        MonthData("Nov", 30, 0.022, 0.140, 0.00, 0.60),
        MonthData("Dec", 31, 0.018, 0.190, 0.00, 0.55)
    )

    val BASE_MATERIALS: List<Material> = listOf(
        Material("eps_graf_70", "Toplotne izolacije", "Grafitni EPS 70/80 (fasadni stiropor)", 16.0, 0.031, 30.0, 16.0, 1450.0, "#64748b"),
        Material("eps_graf_100", "Toplotne izolacije", "Grafitni EPS 100 (obremenjeni)", 12.0, 0.030, 40.0, 20.0, 1450.0, "#475569"),
        Material("eps_beli_70", "Toplotne izolacije", "Beli fasadni EPS 70", 16.0, 0.039, 25.0, 15.0, 1450.0, "#e2e8f0"),
        Material("eps_80", "Toplotne izolacije", "Talni EPS 80", 8.0, 0.037, 35.0, 18.0, 1450.0, "#d1d5db"),
        Material("eps_100", "Toplotne izolacije", "Talni EPS 100 (pod estrihom)", 10.0, 0.036, 40.0, 20.0, 1450.0, "#cbd5e1"),
        Material("eps_150", "Toplotne izolacije", "Talni EPS 150 (trdi)", 10.0, 0.034, 50.0, 25.0, 1450.0, "#94a3b8"),
        Material("eps_200", "Toplotne izolacije", "Talni EPS 200 (visoko obremenjeni)", 10.0, 0.033, 60.0, 30.0, 1450.0, "#64748b"),
        Material("eps_t650", "Toplotne izolacije", "Elastificiran EPS T650 (udarni zvok)", 3.0, 0.042, 20.0, 12.0, 1450.0, "#bfdbfe"),
        Material("xps_300", "Toplotne izolacije", "XPS 300 / Stirodur (cokel, temelj)", 10.0, 0.035, 150.0, 33.0, 1450.0, "#38bdf8"),
        Material("xps_500", "Toplotne izolacije", "XPS 400/500 (pod temeljno ploščo)", 12.0, 0.034, 150.0, 40.0, 1450.0, "#0284c7"),
        Material("mw_fasada", "Toplotne izolacije", "Kamena volna - fasadna plošča/lamela", 16.0, 0.035, 1.0, 110.0, 1030.0, "#facc15"),
        Material("mw_streha", "Toplotne izolacije", "Kamena volna - strešna (DP-5 / DP-8)", 25.0, 0.035, 1.0, 60.0, 1030.0, "#fde047"),
        Material("mw_tla", "Toplotne izolacije", "Kamena volna - talna (Knauf TPS/TPT)", 3.0, 0.036, 1.0, 140.0, 1030.0, "#eab308"),
        Material("gw_035", "Toplotne izolacije", "Steklena volna 035 (Unifit / NatuRoll)", 16.0, 0.035, 1.0, 24.0, 1030.0, "#fde047"),
        Material("gw_032", "Toplotne izolacije", "Steklena volna 032 (visokoizolativna)", 16.0, 0.032, 1.0, 32.0, 1030.0, "#fef08a"),
        Material("pir_022", "Toplotne izolacije", "PIR / PUR izolacijska plošča (alu/flis)", 10.0, 0.022, 60.0, 32.0, 1400.0, "#fcd34d"),
        Material("gutex", "Toplotne izolacije", "Lesnovlaknena plošča (Gutex / Steico)", 8.0, 0.042, 5.0, 160.0, 2100.0, "#d97706"),
        Material("celuloza", "Toplotne izolacije", "Vpihana celulozna izolacija", 25.0, 0.039, 2.0, 50.0, 2020.0, "#a1a1aa"),
        Material("penjeno_steklo", "Toplotne izolacije", "Penjeno steklo (Glapor / Foamglas)", 15.0, 0.045, 50000.0, 130.0, 840.0, "#475569"),
        Material("perlit", "Toplotne izolacije", "Perlit / ekspandirana glina (nasutje)", 10.0, 0.060, 3.0, 100.0, 900.0, "#d6d3d1"),
        Material("zrak_prezr", "Strešne kritine in zračni kanali", "Prezračevan zračni kanal pod streho", 5.0, 1.000, 1.0, 1.2, 1005.0, "#bae6fd", ventilated = true),
        Material("zrak_slabo_prezr", "Strešne kritine in zračni kanali", "Slabše prezračevan zračni sloj", 3.0, 0.350, 1.0, 1.2, 1005.0, "#e0f2fe"),
        Material("zrak_mir", "Strešne kritine in zračni kanali", "Mirujoč (neprezračevan) zračni sloj", 2.0, 0.180, 1.0, 1.2, 1005.0, "#f0f9ff"),
        Material("krit_gerard", "Strešne kritine in zračni kanali", "Strešnik Gerard Klasik (pločevina s posipom)", 0.2, 50.000, 100000.0, 7800.0, 480.0, "#991b1b"),
        Material("krit_tondach", "Strešne kritine in zračni kanali", "Opečni strešnik Tondach (glinena kritina)", 1.5, 1.000, 40.0, 2000.0, 800.0, "#dc2626"),
        Material("krit_bramac", "Strešne kritine in zračni kanali", "Betonski strešnik Bramac / Creaton", 1.8, 1.500, 60.0, 2200.0, 1000.0, "#57534e"),
        Material("krit_trapez", "Strešne kritine in zračni kanali", "Trapezna pločevina / pločevinasta kritina", 0.1, 50.000, 100000.0, 7850.0, 460.0, "#475569"),
        Material("krit_trapez_filc", "Strešne kritine in zračni kanali", "Trapezna pločevina s filcem (antikondenzna)", 0.3, 0.500, 50000.0, 2500.0, 800.0, "#334155"),
        Material("krit_sendvic", "Strešne kritine in zračni kanali", "Sendvič strešni panel (trapez + PUR polnilo)", 4.0, 0.024, 10000.0, 45.0, 1400.0, "#94a3b8"),
        Material("krit_valovitka", "Strešne kritine in zračni kanali", "Vlaknocementna valovitka (Esal / Eternit)", 0.6, 0.350, 50.0, 1650.0, 1000.0, "#78716c"),
        Material("krit_tegola", "Strešne kritine in zračni kanali", "Bitumenska skodla (Tegola)", 0.5, 0.230, 20000.0, 1100.0, 1000.0, "#292524"),
        Material("opeka_votla", "Zidaki in beton", "Votla opeka / modularni blok (29/19 cm)", 29.0, 0.480, 8.0, 900.0, 1000.0, "#ea580c"),
        Material("opeka_termo", "Zidaki in beton", "Termo brušena opeka (Porotherm Profi)", 30.0, 0.190, 8.0, 680.0, 1000.0, "#f97316"),
        Material("opeka_termo_izo", "Zidaki in beton", "Termo opeka polnjena z volno (Porotherm IZO)", 38.0, 0.090, 6.0, 600.0, 1000.0, "#fb923c"),
        Material("opeka_polna", "Zidaki in beton", "Polna opeka (starejša gradnja)", 25.0, 0.760, 10.0, 1800.0, 1000.0, "#b91c1c"),
        Material("ytong", "Zidaki in beton", "Porobeton Ytong Termo (zun. zidaki)", 30.0, 0.110, 6.0, 400.0, 1050.0, "#f1f5f9"),
        Material("ytong_pregradni", "Zidaki in beton", "Porobeton Ytong (pregradne plošče 10/12/15 cm)", 10.0, 0.135, 6.0, 500.0, 1050.0, "#e2e8f0"),
        Material("siporex_stari", "Zidaki in beton", "Siporex / starejši plinobeton", 25.0, 0.160, 6.0, 550.0, 1050.0, "#cbd5e1"),
        Material("beton_votlak", "Zidaki in beton", "Betonski votlak (kletni zidak)", 25.0, 1.150, 25.0, 1400.0, 1000.0, "#9ca3af"),
        Material("ab_beton", "Zidaki in beton", "Armirani beton (plošča, zid, temelj)", 15.0, 2.100, 100.0, 2400.0, 1000.0, "#6b7280"),
        Material("pusti_beton", "Zidaki in beton", "Podložni / pusti beton", 10.0, 1.400, 60.0, 2100.0, 1000.0, "#94a3b8"),
        Material("tampon", "Zidaki in beton", "Gramozni tampon / nasip (0-32 mm)", 25.0, 1.800, 3.0, 1900.0, 840.0, "#a8a29e"),
        Material("zemljina", "Zidaki in beton", "Zemljina / ilovica pod temeljem", 50.0, 1.500, 10.0, 1800.0, 1200.0, "#78350f"),
        Material("omet_notranji", "Ometi, malte in estrihi", "Notranji apneno-cementni omet", 1.5, 0.800, 15.0, 1600.0, 1000.0, "#e5e7eb"),
        Material("omet_mavcni", "Ometi, malte in estrihi", "Notranji strojni mavčni omet", 1.5, 0.350, 10.0, 1000.0, 1000.0, "#f3f4f6"),
        Material("kit_kemaglet", "Ometi, malte in estrihi", "Izravnalna masa (Kemaglet FX / Jubolin)", 0.3, 0.550, 12.0, 1300.0, 1000.0, "#f8fafc"),
        Material("malta_zidar", "Ometi, malte in estrihi", "Klasična zidarska malta / grobi omet", 2.0, 0.850, 15.0, 1700.0, 1000.0, "#d1d5db"),
        Material("malta_termo", "Ometi, malte in estrihi", "Toplotnoizolacijska (termo) malta", 2.0, 0.190, 10.0, 700.0, 1000.0, "#e2e8f0"),
        Material("lepilo_ytong", "Ometi, malte in estrihi", "Tankoslojna lepilna malta (Ytong)", 0.3, 0.800, 20.0, 1500.0, 1000.0, "#cbd5e1"),
        Material("fasadno_lepilo", "Ometi, malte in estrihi", "Fasadno lepilo + mrežica (MultiWhite)", 0.4, 0.800, 25.0, 1500.0, 1000.0, "#cbd5e1"),
        Material("omet_fasadni", "Ometi, malte in estrihi", "Zaključni fasadni omet (silikon/akril)", 0.2, 0.700, 40.0, 1600.0, 1000.0, "#fef3c7"),
        Material("estrih_cem", "Ometi, malte in estrihi", "Cementni estrih (tlak)", 6.0, 1.400, 30.0, 2000.0, 1000.0, "#9ca3af"),
        Material("estrih_anh", "Ometi, malte in estrihi", "Anhidritni samorazlivni estrih", 5.0, 1.200, 15.0, 2100.0, 1000.0, "#d4d4d8"),
        Material("estrih_suhi", "Ometi, malte in estrihi", "Suhi montažni estrih (Knauf Brio / Fermacell)", 2.3, 0.380, 16.0, 1150.0, 1100.0, "#e2e8f0"),
        Material("knauf", "Les, suhomontaža in obloge", "Mavčnokartonska plošča (Knauf GKB)", 1.25, 0.250, 8.0, 800.0, 1050.0, "#f8fafc"),
        Material("knauf_zelena", "Les, suhomontaža in obloge", "Impregnirana plošča (Knauf GKBI zelena)", 1.25, 0.250, 10.0, 820.0, 1050.0, "#bbf7d0"),
        Material("fermacell", "Les, suhomontaža in obloge", "Mavčnovlaknena plošča (Fermacell)", 1.25, 0.320, 13.0, 1150.0, 1100.0, "#e2e8f0"),
        Material("aquapanel", "Les, suhomontaža in obloge", "Cementno vezana plošča (Aquapanel)", 1.25, 0.350, 40.0, 1150.0, 1000.0, "#94a3b8"),
        Material("osb", "Les, suhomontaža in obloge", "OSB 3 plošča", 1.8, 0.130, 150.0, 620.0, 1700.0, "#d97706"),
        Material("deske_opaz", "Les, suhomontaža in obloge", "Lesene deske / strešni opaž (colarice)", 2.4, 0.130, 35.0, 480.0, 1600.0, "#f59e0b"),
        Material("les_smreka", "Les, suhomontaža in obloge", "Konstrukcijski les (smreka/jelka - špirovci)", 14.0, 0.130, 40.0, 470.0, 1600.0, "#b45309"),
        Material("parket", "Les, suhomontaža in obloge", "Leseni parket (hrast)", 1.4, 0.180, 50.0, 700.0, 1600.0, "#92400e"),
        Material("laminat", "Les, suhomontaža in obloge", "Laminat s penasto podlogo", 1.0, 0.150, 100.0, 800.0, 1500.0, "#b45309"),
        Material("keramika", "Les, suhomontaža in obloge", "Keramične ploščice z lepilom", 1.2, 1.050, 150.0, 2000.0, 920.0, "#cbd5e1"),
        Material("vinil", "Les, suhomontaža in obloge", "Vinilna obloga (LVT)", 0.5, 0.230, 5000.0, 1400.0, 1200.0, "#78716c"),
        Material("granit", "Les, suhomontaža in obloge", "Granit / marmor / naravni kamen", 2.0, 2.800, 1000.0, 2700.0, 800.0, "#64748b"),
        Material("parna_zapora", "Folije in hidroizolacije", "Parna zapora PE folija (d=0.02cm, Sd=20m)", 0.02, 0.330, 100000.0, 950.0, 1800.0, "#2563eb"),
        Material("parna_zapora_alu", "Folije in hidroizolacije", "Alu parna zapora (d=0.02cm, Sd=100m)", 0.02, 0.330, 500000.0, 1100.0, 1500.0, "#1d4ed8"),
        Material("parna_ovira", "Folije in hidroizolacije", "Parna ovira za mansardo (d=0.05cm, Sd=5m)", 0.05, 0.330, 10000.0, 900.0, 1700.0, "#60a5fa"),
        Material("parna_ovira_vario", "Folije in hidroizolacije", "Pametna parna ovira (Vario, Sd=2.5m)", 0.05, 0.330, 5000.0, 900.0, 1700.0, "#38bdf8"),
        Material("sek_kritina", "Folije in hidroizolacije", "Paroprepustna sekundarna kritina (Sd=0.02m)", 0.05, 0.330, 40.0, 600.0, 1500.0, "#34d399"),
        Material("bitumen", "Folije in hidroizolacije", "Bitumenska hidroizolacija (Izotekt V4, 0.4cm)", 0.4, 0.230, 50000.0, 1100.0, 1000.0, "#1e293b"),
        Material("hi_tekoca", "Folije in hidroizolacije", "Tekoča hidroizolacija (Hidrozol / Mapelastic)", 0.3, 0.700, 1500.0, 1400.0, 1000.0, "#0ea5e9"),
        Material("epdm_membrana", "Folije in hidroizolacije", "EPDM / TPO strešna membrana (d=0.15cm)", 0.15, 0.200, 80000.0, 1200.0, 1100.0, "#334155"),
        Material("cepasta_folija", "Folije in hidroizolacije", "Čepasta (gumbasta) folija za temelje", 0.8, 0.350, 50000.0, 950.0, 1800.0, "#0f172a"),
        Material("custom", "Ostalo", "Poljuben material (ročni vnos)", 5.0, 0.100, 10.0, 500.0, 1000.0, "#a855f7")
    )

    // ISO 13788 saturation vapor pressure in Pa
    fun psat(t: Double): Double {
        return if (t >= 0.0) {
            610.5 * exp((17.269 * t) / (237.3 + t))
        } else {
            610.5 * exp((21.875 * t) / (265.5 + t))
        }
    }

    // Dew point temperature in °C from vapor pressure p in Pa
    fun tDewFromP(p: Double): Double {
        val sp = max(1.0, p)
        val g = ln(sp / 610.5)
        return if (sp >= 610.5) {
            (237.3 * g) / (17.269 - g)
        } else {
            (265.5 * g) / (21.875 - g)
        }
    }

    // Assembly calculation (U-value, phase shift, Glaser nodes, condensation)
    fun calcAssembly(
        element: EnvelopeElement,
        boundary: BoundaryCondition,
        materials: List<Material>
    ): AssemblyResult {
        val ti = boundary.ti
        val te = boundary.te
        val deltaT = max(0.0, ti - te)
        val teEff = ti - element.fx * (ti - te)
        val phiI = boundary.phiI
        val phiE = boundary.phiE

        var sumR = 0.0
        var sumD = 0.0
        var sumSd = 0.0
        var sumPhaseShift = 0.6
        var sumXi = 0.0
        var sumHeatCapKj = 0.0
        var reachedVent = false

        val computedLayers = element.layers.map { layer ->
            val matRef = materials.find { it.id == layer.matId }
            val isVentAir = layer.matId == "zrak_prezr" || (matRef?.ventilated == true)
            if (isVentAir) reachedVent = true

            val dCm = layer.d
            val dM = max(0.0001, dCm / 100.0)
            val lamBase = max(0.005, layer.lambda)
            val mu = max(1.0, layer.mu)
            val rhoBase = if (layer.rho > 0) layer.rho else (matRef?.rho ?: 500.0)
            val cBase = if (layer.c > 0) layer.c else (matRef?.c ?: 1000.0)

            val woodPct = min(90.0, max(0.0, layer.woodPct))
            val fW = woodPct / 100.0
            val lamEff = if (fW > 0) ((1 - fW) * lamBase + fW * 0.130) else lamBase
            val rhoEff = if (fW > 0) ((1 - fW) * rhoBase + fW * 470.0) else rhoBase
            val cEff = if (fW > 0) ((1 - fW) * cBase + fW * 1600.0) else cBase

            val r = if (reachedVent) 0.0 else (dM / lamEff)
            val sd = if (reachedVent) 0.0 else (mu * dM)
            val layerPhi = if (reachedVent) 0.15 else (0.02305 * dM * sqrt((rhoEff * cEff) / lamEff))

            sumR += r
            sumD += dCm
            sumSd += sd
            sumPhaseShift += layerPhi
            sumXi += layerPhi * (PI / 12.0)
            sumHeatCapKj += if (reachedVent) 0.0 else ((dM * rhoEff * cEff) / 1000.0)

            ComputedLayer(
                matId = layer.matId,
                name = layer.name,
                d = dCm,
                lambda = layer.lambda,
                mu = mu,
                rho = rhoBase,
                c = cBase,
                woodPct = woodPct,
                lamEff = lamEff,
                r = r,
                sd = sd,
                dM = dM,
                layerPhi = layerPhi,
                isVentAir = isVentAir,
                bypassedByVent = reachedVent
            )
        }

        val effectiveRse = if (reachedVent) element.rsi else element.rse
        val rTot = element.rsi + sumR + effectiveRse
        val uVal = if (rTot > 0.0) (1.0 / rTot) else 0.0
        val qLoss = uVal * element.area * deltaT * element.fx
        val decrementFactor = max(0.005, exp(-0.72 * sumXi))

        val pi = (phiI / 100.0) * psat(ti)
        val pe = (phiE / 100.0) * psat(teEff)
        val nodes = mutableListOf<GlaserNode>()
        var cumR = element.rsi
        var cumSd = 0.0
        var cumD = 0.0
        val tsi = ti - (cumR / rTot) * (ti - teEff)

        nodes.add(
            GlaserNode(
                label = "Notranja površina",
                xCm = 0.0,
                temperature = tsi,
                vaporPressure = pi,
                satVaporPressure = psat(tsi),
                dewPoint = tDewFromP(pi),
                condenses = pi >= psat(tsi)
            )
        )

        val condensationZones = mutableListOf<String>()
        computedLayers.forEach { l ->
            cumR += l.r
            cumSd += l.sd
            cumD += l.d
            val tk = if (l.bypassedByVent) teEff else (ti - (cumR / rTot) * (ti - teEff))
            val pk = if (l.bypassedByVent) pe else if (sumSd > 0) (pi - (cumSd / sumSd) * (pi - pe)) else pe
            val psatK = psat(tk)
            val tDewK = tDewFromP(pk)
            val cond = !l.bypassedByVent && (pk >= psatK)
            if (cond) {
                condensationZones.add("Za slojem \"${l.name}\" (T=${String.format("%.1f", tk)}°C ≤ T_ros ${String.format("%.1f", tDewK)}°C)")
            }
            nodes.add(
                GlaserNode(
                    label = l.name,
                    xCm = cumD,
                    temperature = tk,
                    vaporPressure = pk,
                    satVaporPressure = psatK,
                    dewPoint = tDewK,
                    condenses = cond,
                    bypassedByVent = l.bypassedByVent
                )
            )
        }

        val fRsi = if ((ti - teEff) > 0.0) ((tsi - teEff) / (ti - teEff)) else 1.0

        return AssemblyResult(
            computedLayers = computedLayers,
            sumD = sumD,
            sumR = sumR,
            sumSd = sumSd,
            rTot = rTot,
            uVal = uVal,
            qLoss = qLoss,
            tsi = tsi,
            fRsi = fRsi,
            phaseShiftHours = sumPhaseShift,
            decrementFactor = decrementFactor,
            sumHeatCapKj = sumHeatCapKj,
            nodes = nodes,
            condensationZones = condensationZones,
            hasVentilatedLayer = reachedVent
        )
    }

    fun calcRequiredInsulationThickness(
        element: EnvelopeElement,
        boundary: BoundaryCondition,
        materials: List<Material>,
        targetU: Double,
        lambda: Double
    ): Int {
        val currentRes = calcAssembly(element, boundary, materials)
        val currentRseRsi = element.rsi + element.rse
        val sumOtherR = currentRes.rTot - currentRseRsi
        val targetRTot = 1.0 / max(0.01, targetU)
        val requiredAdditionalR = max(0.0, targetRTot - sumOtherR)
        val requiredThicknessM = requiredAdditionalR * lambda
        return ceil(requiredThicknessM * 100.0).toInt()
    }

    fun calcDhwAndWell(
        dw: DhwAndWellSettings,
        heatingNetKwh: Double,
        fuels: List<Fuel>
    ): DhwAndWellResult {
        val persons = max(0, dw.persons)
        val liters = max(0.0, dw.litersPerPerson)
        val dT = max(5.0, dw.tempHot - dw.tempCold)
        val lossFactor = 1.0 + (max(0.0, dw.tankLossPct) / 100.0)
        val dailyDhwKwh = (persons * liters * 1.163 * dT / 1000.0) * lossFactor
        val annualDhwKwh = if (dw.dhwEnabled) (dailyDhwKwh * 365.0) else 0.0
        val totalThermalKwh = heatingNetKwh + annualDhwKwh

        val hpKw = max(2.0, dw.hpThermalPowerKw)
        val machineCop = max(1.5, dw.hpMachineCop)
        val pumpW = max(0.0, dw.wellPumpWatts)
        val waterDeltaT = max(1.5, dw.wellWaterDeltaT)

        val evaporatorKw = hpKw * (1.0 - 1.0 / machineCop)
        val requiredWaterFlowM3h = evaporatorKw / (1.163 * waterDeltaT)
        val requiredWaterFlowLmin = (requiredWaterFlowM3h * 1000.0) / 60.0

        val runHours = totalThermalKwh / hpKw
        val compressorElKwh = totalThermalKwh / machineCop
        val wellPumpElKwh = (pumpW / 1000.0) * runHours
        val totalHpWaterElKwh = compressorElKwh + wellPumpElKwh
        val systemScop = if (totalHpWaterElKwh > 0.0) (totalThermalKwh / totalHpWaterElKwh) else machineCop

        val tSum = dw.summerIndoorTemp
        val rhSum = dw.summerIndoorRh
        val floorSummerDewPoint = tDewFromP((rhSum / 100.0) * psat(tSum))
        val minSafeCoolingWaterTemp = floorSummerDewPoint + 1.5

        return DhwAndWellResult(
            dailyDhwKwh = dailyDhwKwh,
            annualDhwKwh = annualDhwKwh,
            totalThermalKwh = totalThermalKwh,
            runHours = runHours,
            compressorElKwh = compressorElKwh,
            wellPumpElKwh = wellPumpElKwh,
            totalHpWaterElKwh = totalHpWaterElKwh,
            systemScop = systemScop,
            evaporatorKw = evaporatorKw,
            requiredWaterFlowM3h = requiredWaterFlowM3h,
            requiredWaterFlowLmin = requiredWaterFlowLmin,
            floorSummerDewPoint = floorSummerDewPoint,
            minSafeCoolingWaterTemp = minSafeCoolingWaterTemp
        )
    }

    fun calcSolarAndBatterySim(
        sg: SolarAndGridSettings,
        md: MeterData,
        annualHeatElKwh: Double,
        annualDhwElKwh: Double
    ): SolarBatterySimResult {
        val annualPvKwh = max(0.0, sg.pvKwp) * max(400.0, sg.specificYield)
        val usableBatKwh = max(0.0, sg.batteryCapKwh) * (min(100.0, max(10.0, sg.batteryDodPct)) / 100.0)
        val batEff = min(0.99, max(0.50, sg.batteryEffPct / 100.0))
        val directRatioBase = min(0.90, max(0.10, sg.directSelfConsPct / 100.0))
        val acAnnual = md.measuredAcElKwh
        val houseAnnual = md.measuredHouseholdKwh

        var sumLoad = 0.0
        var sumPv = 0.0
        var sumDirect = 0.0
        var sumBatDischarged = 0.0
        var sumGridWithBat = 0.0
        var sumGridNoBat = 0.0
        var sumExportWithBat = 0.0
        var sumExportNoBat = 0.0

        val months = MONTHS_DATA.mapIndexed { idx, m ->
            val pvM = annualPvKwh * m.pvShare
            val hpModelM = (annualHeatElKwh * m.heatShare) + (annualDhwElKwh / 12.0)
            val modelTotalM = hpModelM + (acAnnual * m.acShare) + (houseAnnual / 12.0)
            val meterM = if (idx < md.monthlyTotalKwh.size) md.monthlyTotalKwh[idx] else modelTotalM
            val loadM = if (sg.simLoadSource == "model") modelTotalM else meterM

            val pvDay = pvM / m.days
            val loadDay = loadM / m.days
            val directDay = min(pvDay * 0.86, loadDay * min(0.85, directRatioBase * (if (m.acShare > 0) 1.15 else 1.0)))
            val surplusPvDay = max(0.0, pvDay - directDay)
            val remLoadDay = max(0.0, loadDay - directDay)
            val chargedIntoBatDay = min(surplusPvDay * 0.90, (usableBatKwh * m.activeCycleFactor) / batEff)
            val dischargedFromBatDay = min(remLoadDay, chargedIntoBatDay * batEff)

            val directM = directDay * m.days
            val batM = dischargedFromBatDay * m.days
            val selfTotalM = directM + batM
            val gridWithBatM = max(0.0, loadM - selfTotalM)
            val gridNoBatM = max(0.0, loadM - directM)
            val exportWithBatM = max(0.0, pvM - directM - ((dischargedFromBatDay / batEff) * m.days))
            val exportNoBatM = max(0.0, pvM - directM)

            sumLoad += loadM
            sumPv += pvM
            sumDirect += directM
            sumBatDischarged += batM
            sumGridWithBat += gridWithBatM
            sumGridNoBat += gridNoBatM
            sumExportWithBat += exportWithBatM
            sumExportNoBat += exportNoBatM

            MonthlySimRow(
                monthName = m.m,
                meterM = meterM,
                hpModelM = hpModelM,
                loadM = loadM,
                pvM = pvM,
                directM = directM,
                batM = batM,
                gridWithBatM = gridWithBatM,
                exportWithBatM = exportWithBatM,
                selfSuffPct = if (loadM > 0) min(100.0, (selfTotalM / loadM) * 100.0) else 0.0,
                selfSuffNoBatPct = if (loadM > 0) min(100.0, (directM / loadM) * 100.0) else 0.0
            )
        }

        val selfSuffNoBatAnnualPct = if (sumLoad > 0) (sumDirect / sumLoad) * 100.0 else 0.0
        val selfSuffWithBatAnnualPct = if (sumLoad > 0) ((sumDirect + sumBatDischarged) / sumLoad) * 100.0 else 0.0
        val eqFullCycles = if (usableBatKwh > 0) (sumBatDischarged / usableBatKwh) else 0.0
        val pGrid = max(0.01, sg.gridImportPriceEur)
        val pExp = if (sg.exportMode == "zero") 0.0 else max(0.0, sg.exportPriceEur)

        val costNoPvEur = sumLoad * pGrid
        val costPvOnlyEur = max(0.0, (sumGridNoBat * pGrid) - (sumExportNoBat * pExp))
        val costPvWithBatEur = max(0.0, (sumGridWithBat * pGrid) - (sumExportWithBat * pExp))
        val batteryExtraSavingsEur = max(0.0, costPvOnlyEur - costPvWithBatEur)
        val totalPvAndBatSavingsEur = max(0.0, costNoPvEur - costPvWithBatEur)
        val batPaybackYears = if (batteryExtraSavingsEur > 0.0) (sg.batteryInvestEur / batteryExtraSavingsEur) else 0.0

        return SolarBatterySimResult(
            months = months,
            usableBatKwh = usableBatKwh,
            sumLoad = sumLoad,
            sumPv = sumPv,
            sumDirect = sumDirect,
            sumBatDischarged = sumBatDischarged,
            sumGridWithBat = sumGridWithBat,
            sumGridNoBat = sumGridNoBat,
            sumExportWithBat = sumExportWithBat,
            sumExportNoBat = sumExportNoBat,
            selfSuffNoBatAnnualPct = selfSuffNoBatAnnualPct,
            selfSuffWithBatAnnualPct = selfSuffWithBatAnnualPct,
            eqFullCycles = eqFullCycles,
            costNoPvEur = costNoPvEur,
            costPvOnlyEur = costPvOnlyEur,
            costPvWithBatEur = costPvWithBatEur,
            batteryExtraSavingsEur = batteryExtraSavingsEur,
            totalPvAndBatSavingsEur = totalPvAndBatSavingsEur,
            batPaybackYears = batPaybackYears
        )
    }

    fun calculateAllTotals(
        boundary: BoundaryCondition,
        walls: List<EnvelopeElement>,
        roofs: List<EnvelopeElement>,
        floors: List<EnvelopeElement>,
        windows: List<WindowElement>,
        doors: List<DoorElement>,
        bridges: BridgeSettings,
        ventilation: VentilationSettings,
        dhwAndWell: DhwAndWellSettings,
        hpSettings: HpSettings,
        solarAndGrid: SolarAndGridSettings,
        meterData: MeterData,
        fuels: List<Fuel>,
        materials: List<Material>
    ): AllTotals {
        val deltaT = max(0.0, boundary.ti - boundary.te)
        val wallsQ = walls.sumOf { calcAssembly(it, boundary, materials).qLoss }
        val roofsQ = roofs.sumOf { calcAssembly(it, boundary, materials).qLoss }
        val floorsQ = floors.sumOf { calcAssembly(it, boundary, materials).qLoss }

        val envelopeMeetsPures =
            walls.all { calcAssembly(it, boundary, materials).uVal <= (if (it.name.contains("cokel", ignoreCase = true)) 0.25 else 0.18) } &&
            roofs.all { calcAssembly(it, boundary, materials).uVal <= 0.14 } &&
            floors.all { calcAssembly(it, boundary, materials).uVal <= 0.25 }

        val envelopeMeetsPassive =
            walls.all { calcAssembly(it, boundary, materials).uVal <= (if (it.name.contains("cokel", ignoreCase = true)) 0.20 else 0.15) } &&
            roofs.all { calcAssembly(it, boundary, materials).uVal <= 0.10 } &&
            floors.all { calcAssembly(it, boundary, materials).uVal <= 0.15 }

        var windowsQ = 0.0
        var solarGainKwh = 0.0
        windows.forEach { w ->
            val area = w.w * w.h * w.count
            windowsQ += area * w.uw * deltaT * w.fx
            val orientObj = SOLAR_WINTER_IRR[w.orient] ?: SOLAR_WINTER_IRR["J"]!!
            solarGainKwh += area * orientObj.kwhM2 * w.g * 0.70 * 0.75 * 0.85
        }

        val doorsQ = doors.sumOf { it.area * it.ud * deltaT * it.fx }
        val totalEnvArea = walls.sumOf { it.area } + roofs.sumOf { it.area } + floors.sumOf { it.area } +
                windows.sumOf { it.w * it.h * it.count } + doors.sumOf { it.area }

        val bridgesQ = if (!bridges.enabled) {
            0.0
        } else if (bridges.useFlatRate) {
            totalEnvArea * bridges.flatDeltaU * deltaT
        } else {
            bridges.items.sumOf { it.length * it.psi * deltaT * it.fx }
        }

        val vol = ventilation.volumeArea * ventilation.ceilingHeight
        val ventNaturalQ = 0.34 * vol * ventilation.airChanges * deltaT
        val ventQ = if (ventilation.enabled) {
            ventNaturalQ * (1.0 - min(95.0, max(0.0, ventilation.heatRecoveryEff)) / 100.0)
        } else 0.0

        val totalW = wallsQ + roofsQ + floorsQ + windowsQ + doorsQ + bridgesQ + ventQ
        val grossHeatingKwh = (totalW / 1000.0) * boundary.hours
        val netHeatingKwh = max(500.0, grossHeatingKwh - solarGainKwh)
        val heatedArea = max(1.0, boundary.heatedArea)

        val dhwWell = calcDhwAndWell(dhwAndWell, netHeatingKwh, fuels)
        val isWaterHp = hpSettings.selectedHpFuelId == "tc_voda"
        val hpScop = if (isWaterHp) dhwWell.systemScop else (fuels.find { it.id == "tc_zrak" }?.eff ?: 3.50)
        val dhwCop = if (isWaterHp) dhwAndWell.mainHpDhwCop else 2.60

        val annualHeatElKwh = netHeatingKwh / hpScop
        val annualDhwElKwh = dhwWell.annualDhwKwh / dhwCop
        val modelHpElKwh = annualHeatElKwh + annualDhwElKwh
        val autoHpElAtTeKw = (totalW / 1000.0) / max(1.2, hpSettings.copAtDesignTe) + (hpSettings.auxPumpsWatts / 1000.0)
        val effectiveHpElAtTeKw = if (hpSettings.peakElMode == "manual") hpSettings.manualPeakElKw else autoHpElAtTeKw
        val solarBatSim = calcSolarAndBatterySim(solarAndGrid, meterData, annualHeatElKwh, annualDhwElKwh)

        return AllTotals(
            wallsQ = wallsQ,
            roofsQ = roofsQ,
            floorsQ = floorsQ,
            windowsQ = windowsQ,
            doorsQ = doorsQ,
            bridgesQ = bridgesQ,
            ventQ = ventQ,
            ventNaturalQ = ventNaturalQ,
            totalW = totalW,
            totalKw = totalW / 1000.0,
            grossHeatingKwh = grossHeatingKwh,
            solarGainKwh = solarGainKwh,
            netHeatingKwh = netHeatingKwh,
            annualDhwKwh = dhwWell.annualDhwKwh,
            annualKwh = dhwWell.totalThermalKwh,
            specificKwh = netHeatingKwh / heatedArea,
            specificW = totalW / heatedArea,
            totalEnvArea = totalEnvArea,
            dhwWell = dhwWell,
            annualHeatElKwh = annualHeatElKwh,
            annualDhwElKwh = annualDhwElKwh,
            modelHpElKwh = modelHpElKwh,
            autoHpElAtTeKw = autoHpElAtTeKw,
            effectiveHpElAtTeKw = effectiveHpElAtTeKw,
            solarBatSim = solarBatSim,
            envelopeMeetsPures = envelopeMeetsPures,
            envelopeMeetsPassive = envelopeMeetsPassive
        )
    }

    fun calcFuelAnnualCost(
        fuel: Fuel,
        dhwSettings: DhwAndWellSettings,
        primaryElFuel: Fuel,
        heatingKwh: Double,
        dhwKwh: Double = 0.0
    ): FuelCostResult {
        val eff = max(0.1, fuel.eff)
        val ePerUnit = max(0.1, fuel.energyPerUnit)
        val price = max(0.0, fuel.price)
        val elPrice = primaryElFuel.price
        val heatingEur = (heatingKwh / (eff * ePerUnit)) * price

        var dhwEur = 0.0
        val mode = dhwSettings.dhwSourceMode
        val mainHpDhwCop = max(1.2, dhwSettings.mainHpDhwCop)
        val sanitaryHpCop = max(1.2, dhwSettings.sanitaryHpCop)

        if (dhwKwh > 0.0) {
            when (mode) {
                "main" -> {
                    dhwEur = if (fuel.id == "tc_voda" || fuel.id == "tc_zrak") {
                        val cop = if (fuel.id == "tc_voda") mainHpDhwCop else max(1.8, mainHpDhwCop - 0.5)
                        (dhwKwh / cop) * price
                    } else {
                        (dhwKwh / (eff * ePerUnit)) * price
                    }
                }
                "sanitary_hp" -> {
                    dhwEur = (dhwKwh / sanitaryHpCop) * elPrice
                }
                "el_boiler" -> {
                    dhwEur = dhwKwh * elPrice
                }
                "combo" -> {
                    val wCost = if (fuel.id == "tc_voda" || fuel.id == "tc_zrak") {
                        ((dhwKwh * 0.6) / mainHpDhwCop) * price
                    } else {
                        ((dhwKwh * 0.6) / (eff * ePerUnit)) * price
                    }
                    dhwEur = wCost + (dhwKwh * 0.4 * elPrice)
                }
            }
        }

        return FuelCostResult(
            heatingEur = heatingEur,
            dhwEur = dhwEur,
            annualEur = heatingEur + dhwEur,
            costPerEffectiveKwh = price / (eff * ePerUnit)
        )
    }
}
