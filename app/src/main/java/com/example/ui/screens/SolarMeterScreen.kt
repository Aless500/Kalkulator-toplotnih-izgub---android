package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.components.SolarBatteryMonthlyChart
import com.example.viewmodel.CalculatorViewModel

@Composable
fun SolarMeterScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val totals by viewModel.totals.collectAsState()

    val md = state.meterData
    val sg = state.solarAndGrid
    val sim = totals.solarBatSim

    val modelHpElKwh = totals.modelHpElKwh
    val measuredHpEl = md.measuredHpElKwh
    val diffHpEl = modelHpElKwh - measuredHpEl
    val diffPct = if (measuredHpEl > 0) (diffHpEl / measuredHpEl) * 100.0 else 0.0

    val curBat = sg.batteryCapKwh

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Meter Data & Calibration Section
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
                            text = "📟 Števec elektrike & Kalibracija",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Primerjava modela s podatki števca (Shelly / Moj Elektro)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.calibrateModelToMeter() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.testTag("calibrate_meter_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kalibriraj ure", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = md.totalMeterKwh.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateMeterData(totalMeterKwh = it) } },
                        label = { Text("Skupaj števec (kWh)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = md.measuredHpElKwh.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateMeterData(measuredHpElKwh = it) } },
                        label = { Text("Izmerjena TČ (kWh)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = md.annualBillEur.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateMeterData(annualBillEur = it) } },
                        label = { Text("Letni račun (€)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            Text("MODEL TČ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${modelHpElKwh.toInt()} kWh el.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("ŠTEVEC TČ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${measuredHpEl.toInt()} kWh el.", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ODSTOPANJE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "${if (diffHpEl > 0) "+" else ""}${diffHpEl.toInt()} kWh (${String.format("%.1f", diffPct)}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (kotlin.math.abs(diffPct) <= 10.0) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }

        // Solar PV & Battery Section
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
                            text = "🔋 Sončna elektrarna & Baterija",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Samooskrba z baterijo: ${String.format("%.1f", sim.selfSuffWithBatAnnualPct)}% (brez: ${String.format("%.1f", sim.selfSuffNoBatAnnualPct)}%)",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Battery Buttons
                Text("Izberi kapaciteto baterije:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.0, 5.0, 10.0, 15.0, 20.0).forEach { bCap ->
                        FilterChip(
                            selected = curBat == bCap,
                            onClick = { viewModel.updateSolarAndGrid(batteryCapKwh = bCap) },
                            label = { Text("${bCap.toInt()} kWh", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sg.pvKwp.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateSolarAndGrid(pvKwp = it) } },
                        label = { Text("Moč SE (kWp)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sg.batteryCapKwh.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateSolarAndGrid(batteryCapKwh = it) } },
                        label = { Text("Baterija (kWh)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sg.batteryInvestEur.toInt().toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateSolarAndGrid(batteryInvestEur = it) } },
                        label = { Text("Cena bat. (€)", fontSize = 9.sp) },
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
                        value = sg.gridImportPriceEur.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateSolarAndGrid(gridImportPriceEur = it) } },
                        label = { Text("Dokup (€/kWh)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sg.exportPriceEur.toString(),
                        onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateSolarAndGrid(exportPriceEur = it) } },
                        label = { Text("Višek (€/kWh)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly Chart
                SolarBatteryMonthlyChart(sim = sim)

                Spacer(modifier = Modifier.height(10.dp))

                // Results Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Proizvodnja SE / Letna poraba:", fontSize = 12.sp)
                            Text("${sim.sumPv.toInt()} / ${sim.sumLoad.toInt()} kWh", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Direktno + iz baterije:", fontSize = 12.sp)
                            Text("${sim.sumDirect.toInt()} + ${sim.sumBatDischarged.toInt()} kWh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dokup iz omrežja:", fontSize = 12.sp)
                            Text("${sim.sumGridWithBat.toInt()} kWh/leto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Strošek s SE + baterijo:", fontSize = 12.sp)
                            Text("${sim.costPvWithBatEur.toInt()} €/leto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dodatni prihranek baterije:", fontSize = 12.sp)
                            Text(
                                text = "+${sim.batteryExtraSavingsEur.toInt()} €/leto (${if (sim.batPaybackYears > 0) String.format("%.1f", sim.batPaybackYears) + " let" else "-"})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
