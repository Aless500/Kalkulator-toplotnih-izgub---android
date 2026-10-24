package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CalculatorViewModel
import kotlin.math.max

@Composable
fun DhwHpScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val totals by viewModel.totals.collectAsState()

    val dw = state.dhwAndWell
    val info = totals.dhwWell
    val hp = state.hpSettings

    val fuseAmps = hp.fuseAmps.toDouble()
    val maxFuseKw = (3.0 * 230.0 * fuseAmps) / 1000.0
    val totalHpPeakElKw = totals.effectiveHpElAtTeKw + hp.backupHeaterKw
    val totalHousePeakKw = totalHpPeakElKw + hp.baseHousePeakKw
    val fuseOk = totalHousePeakKw <= maxFuseKw
    val thermalReserveKw = hp.maxThermalKwAtPeak - totals.totalKw
    val deltaT = max(1.0, state.boundary.ti - state.boundary.te)
    val cyclingOutdoorTemp = state.boundary.ti - (hp.minModulationKw / max(0.1, totals.totalKw)) * deltaT

    var showModeMenu by remember { mutableStateOf(false) }
    var showFuseMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DHW Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🚿 Sanitarna topla voda (STV)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (dw.dhwEnabled) "Letna toplota za STV: ${info.annualDhwKwh.toInt()} kWh/leto" else "Izklopljeno (0 kWh)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dw.dhwEnabled,
                        onCheckedChange = { viewModel.updateDhw(dhwEnabled = it) },
                        modifier = Modifier.testTag("toggle_dhw_switch")
                    )
                }

                AnimatedVisibility(visible = dw.dhwEnabled) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {

                        // DHW Source Mode
                        Box {
                            val modeLabel = when (dw.dhwSourceMode) {
                                "main" -> "1. Glavni vir (TČ z nižjim COP za 55°C)"
                                "sanitary_hp" -> "2. Ločena sanitarna TČ (bojler s TČ)"
                                "el_boiler" -> "3. Električni bojler (COP 1.0)"
                                else -> "4. Kombinirano (TČ pozimi + el. poleti)"
                            }

                            OutlinedButton(
                                onClick = { showModeMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(modeLabel, fontSize = 12.sp, maxLines = 1)
                            }

                            DropdownMenu(
                                expanded = showModeMenu,
                                onDismissRequest = { showModeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("1. Glavni vir (TČ za 55°C)", fontSize = 12.sp) },
                                    onClick = { viewModel.updateDhw(dhwSourceMode = "main"); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("2. Ločena sanitarna TČ", fontSize = 12.sp) },
                                    onClick = { viewModel.updateDhw(dhwSourceMode = "sanitary_hp"); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("3. Klasični električni bojler", fontSize = 12.sp) },
                                    onClick = { viewModel.updateDhw(dhwSourceMode = "el_boiler"); showModeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("4. Kombinirano (TČ + bojler)", fontSize = 12.sp) },
                                    onClick = { viewModel.updateDhw(dhwSourceMode = "combo"); showModeMenu = false }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = dw.persons.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { viewModel.updateDhw(persons = it) } },
                                label = { Text("Oseb", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dw.litersPerPerson.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(litersPerPerson = it) } },
                                label = { Text("L/osebo/dan", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dw.tempHot.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(tempHot = it) } },
                                label = { Text("Bojler (°C)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = dw.mainHpDhwCop.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(mainHpDhwCop = it) } },
                                label = { Text("COP TČ (55°C)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dw.sanitaryHpCop.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(sanitaryHpCop = it) } },
                                label = { Text("SCOP sanit. TČ", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dw.tankLossPct.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(tankLossPct = it) } },
                                label = { Text("Izgube cevi (%)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("DNEVNO TOČENJE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                    Text("${(dw.persons * dw.litersPerPerson).toInt()} L/dan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("DNEVNA TOPLOTA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                    Text("${String.format("%.1f", info.dailyDhwKwh)} kWh/dan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("LETNA POTREBA STV", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                    Text("${info.annualDhwKwh.toInt()} kWh/leto", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Groundwater Well & Water-Water HP Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💧 Vrtina TČ voda–voda: Potopna črpalka & Pretok",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Potreben pretok podtalnice: ${String.format("%.2f", info.requiredWaterFlowM3h)} m³/h (${String.format("%.1f", info.requiredWaterFlowLmin)} L/min)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dw.hpThermalPowerKw.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(hpThermalPowerKw = it) } },
                        label = { Text("Grelna moč (kW)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dw.hpMachineCop.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(hpMachineCop = it) } },
                        label = { Text("COP same TČ", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dw.wellPumpWatts.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(wellPumpWatts = it) } },
                        label = { Text("Potopna (W)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dw.wellWaterDeltaT.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(wellWaterDeltaT = it) } },
                        label = { Text("Ohlajanje ΔT (K)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dw.summerIndoorTemp.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(summerIndoorTemp = it) } },
                        label = { Text("Poletna T (°C)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dw.summerIndoorRh.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateDhw(summerIndoorRh = it) } },
                        label = { Text("Poletna vlaga (%)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Results Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dejanski sistemski SCOP:", fontSize = 12.sp)
                            Text("${String.format("%.2f", info.systemScop)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Poraba potopne črpalke:", fontSize = 12.sp)
                            Text("+${info.wellPumpElKwh.toInt()} kWh el./leto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Poletno rosišče tal:", fontSize = 12.sp)
                            Text("${String.format("%.1f", info.floorSummerDewPoint)} °C", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Min. varna temp. hladilne vode:", fontSize = 12.sp)
                            Text("≥ ${String.format("%.1f", info.minSafeCoolingWaterTemp)} °C", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        // Heat Pump Settings & Electrical Fuses Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Kontrola varovalk & El. konica TČ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (fuseOk) "✓ Varovalke 3×${hp.fuseAmps}A zadoščajo" else "⚠ Preobremenitev varovalk!",
                            fontSize = 12.sp,
                            color = if (fuseOk) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedButton(
                            onClick = { showFuseMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Varovalke: 3×${hp.fuseAmps} A", fontSize = 11.sp, maxLines = 1)
                        }
                        DropdownMenu(expanded = showFuseMenu, onDismissRequest = { showFuseMenu = false }) {
                            listOf(16, 20, 25, 35).forEach { a ->
                                DropdownMenuItem(
                                    text = { Text("3×$a A (${String.format("%.1f", (3.0 * 230.0 * a) / 1000.0)} kW)") },
                                    onClick = {
                                        viewModel.updateHpSettings(fuseAmps = a)
                                        showFuseMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = hp.baseHousePeakKw.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateHpSettings(baseHousePeakKw = it) } },
                        label = { Text("Ostali porabniki (kW)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = (if (hp.peakElMode == "manual") hp.manualPeakElKw else totals.autoHpElAtTeKw).let { String.format("%.2f", it) },
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let {
                                viewModel.updateHpSettings(peakElMode = "manual", manualPeakElKw = it)
                            }
                        },
                        label = { Text("El. odjem TČ (${state.boundary.te}°C)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hp.copAtDesignTe.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateHpSettings(copAtDesignTe = it) } },
                        label = { Text("COP pri Te", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hp.minModulationKw.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateHpSettings(minModulationKw = it) } },
                        label = { Text("Min. mod (kW)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Maks. moč varovalk:", fontSize = 12.sp)
                            Text("${String.format("%.1f", maxFuseKw)} kW", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Skupna konica (TČ + hiša):", fontSize = 12.sp)
                            Text(
                                text = "${String.format("%.2f", totalHousePeakKw)} kW (${(totalHousePeakKw / maxFuseKw * 100).toInt()}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (fuseOk) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Meja taktiziranja kompresorja:", fontSize = 12.sp)
                            Text("nad +${String.format("%.1f", cyclingOutdoorTemp)} °C zunaj", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Priporočena dogovorjena moč (Blok 1):", fontSize = 12.sp)
                            Text("${String.format("%.1f", max(4.2, totalHpPeakElKw + 2.4))} kW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}
