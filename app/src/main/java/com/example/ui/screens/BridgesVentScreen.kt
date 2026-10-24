package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculationEngine
import com.example.viewmodel.CalculatorViewModel
import kotlin.math.max

@Composable
fun BridgesVentScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val deltaT = max(0.0, state.boundary.ti - state.boundary.te)

    val v = state.ventilation
    val vol = v.volumeArea * v.ceilingHeight
    val flowM3h = vol * v.airChanges
    val hours = state.boundary.hours
    val natW = totals.ventNaturalQ
    val natKwh = (natW / 1000.0) * hours
    val recW = totals.ventQ
    val recKwh = (recW / 1000.0) * hours
    val curUnit = CalculationEngine.HRV_DEVICES_DB.find { it.id == v.selectedUnitId } ?: CalculationEngine.HRV_DEVICES_DB[0]
    val fanElKwh = if (v.heatRecoveryEff > 0) ((flowM3h * curUnit.spi * 8760.0) / 1000.0).toInt() else 0

    var showPresetPsiMenu by remember { mutableStateOf(false) }
    var showHrvUnitMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Linear Thermal Bridges Section
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
                            text = "📐 Linijski toplotni mostovi (Ψ)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.bridges.enabled) "Izguba mostov: ${totals.bridgesQ.toInt()} W" else "Izklopljeno (0 W)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.bridges.enabled,
                        onCheckedChange = { viewModel.toggleBridgesEnabled(it) },
                        modifier = Modifier.testTag("toggle_bridges_switch")
                    )
                }

                AnimatedVisibility(visible = state.bridges.enabled) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.bridges.useFlatRate,
                                onCheckedChange = { viewModel.toggleBridgeMode(it) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Uporabi pavšalni pribitek ΔU_tb na celoten ovoj",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (state.bridges.useFlatRate) {
                            OutlinedTextField(
                                value = state.bridges.flatDeltaU.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateFlatDeltaU(it) } },
                                label = { Text("ΔU_tb (W/m²K)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                singleLine = true
                            )
                        } else {
                            // Preset insertion button
                            Box(modifier = Modifier.padding(vertical = 6.dp)) {
                                OutlinedButton(
                                    onClick = { showPresetPsiMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dodaj iz knjižnice standardov (Ψ)", fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = showPresetPsiMenu,
                                    onDismissRequest = { showPresetPsiMenu = false }
                                ) {
                                    CalculationEngine.PRESET_PSI_BRIDGES.forEach { b ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(b.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Ψ = ${b.psi} W/mK", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                            },
                                            onClick = {
                                                viewModel.addBridge(name = b.name, length = 20.0, psi = b.psi)
                                                showPresetPsiMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            state.bridges.items.forEachIndexed { idx, itm ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = itm.name,
                                                onValueChange = { viewModel.updateBridgeItem(idx, name = it) },
                                                label = { Text("Naziv mostu", fontSize = 9.sp) },
                                                modifier = Modifier.weight(1.8f),
                                                singleLine = true
                                            )
                                            IconButton(onClick = { viewModel.removeBridge(idx) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Izbriši", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = itm.length.toString(),
                                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateBridgeItem(idx, length = it) } },
                                                label = { Text("L (m)", fontSize = 9.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = itm.psi.toString(),
                                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateBridgeItem(idx, psi = it) } },
                                                label = { Text("Ψ (W/mK)", fontSize = 9.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                Text("Izguba:", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                Text("${(itm.length * itm.psi * deltaT * itm.fx).toInt()} W", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ventilation Section
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
                            text = "💨 Prezračevanje & Rekuperator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pretok zraka: ${flowM3h.toInt()} m³/h",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = v.enabled,
                        onCheckedChange = { viewModel.updateVentilation(enabled = it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // HRV Unit Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🏢 Centralni rekuperator (PHI certificirano):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { showHrvUnitMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("${curUnit.name} (η=${curUnit.eff.toInt()}%)", fontSize = 12.sp, maxLines = 1)
                            }
                            DropdownMenu(
                                expanded = showHrvUnitMenu,
                                onDismissRequest = { showHrvUnitMenu = false }
                            ) {
                                CalculationEngine.HRV_DEVICES_DB.forEach { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(u.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("η = ${u.eff.toInt()}% | max ${u.flow} m³/h | SPI ${u.spi} Wh/m³ (${u.type})", fontSize = 10.sp)
                                            }
                                        },
                                        onClick = {
                                            viewModel.applyVentUnit(u.id)
                                            showHrvUnitMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Izmenjevalnik: ${curUnit.type}", fontSize = 11.sp)
                            Text("Izkoristek: ${curUnit.eff.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text("El. SPI: ~${fanElKwh} kWh/leto", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = v.volumeArea.toInt().toString(),
                        onValueChange = { valNum -> valNum.toDoubleOrNull()?.let { viewModel.updateVentilation(volumeArea = it) } },
                        label = { Text("Površina (m²)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = v.ceilingHeight.toString(),
                        onValueChange = { valNum -> valNum.toDoubleOrNull()?.let { viewModel.updateVentilation(ceilingHeight = it) } },
                        label = { Text("Višina (m)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = v.airChanges.toString(),
                        onValueChange = { valNum -> valNum.toDoubleOrNull()?.let { viewModel.updateVentilation(airChanges = it) } },
                        label = { Text("n (1/h)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = v.heatRecoveryEff.toInt().toString(),
                        onValueChange = { valNum -> valNum.toDoubleOrNull()?.let { viewModel.updateVentilation(heatRecoveryEff = it) } },
                        label = { Text("η (%)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick preset buttons for recovery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = v.heatRecoveryEff == 0.0,
                        onClick = { viewModel.updateVentilation(heatRecoveryEff = 0.0) },
                        label = { Text("Brez (0%)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = v.heatRecoveryEff == 85.0,
                        onClick = { viewModel.updateVentilation(heatRecoveryEff = 85.0) },
                        label = { Text("85%", fontSize = 10.sp) },
                        modifier = Modifier.weight(0.7f)
                    )
                    FilterChip(
                        selected = v.heatRecoveryEff == 90.0,
                        onClick = { viewModel.updateVentilation(heatRecoveryEff = 90.0) },
                        label = { Text("90%", fontSize = 10.sp) },
                        modifier = Modifier.weight(0.7f)
                    )
                    FilterChip(
                        selected = v.heatRecoveryEff == 93.0,
                        onClick = { viewModel.updateVentilation(heatRecoveryEff = 93.0) },
                        label = { Text("93%", fontSize = 10.sp) },
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ventilation comparison summary
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
                            Text("BREZ REKUPERACIJE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${natW.toInt()} W (${natKwh.toInt()} kWh)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Column {
                            Text("Z REKUPERACIJO (${v.heatRecoveryEff.toInt()}%)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${recW.toInt()} W (${recKwh.toInt()} kWh)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PRIHRANEK MOČI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("-${(natW - recW).toInt()} W", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
