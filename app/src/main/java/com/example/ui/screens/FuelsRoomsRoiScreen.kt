package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculationEngine
import com.example.model.Fuel
import com.example.model.RoomElement
import com.example.viewmodel.CalculatorViewModel
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun FuelsRoomsRoiScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val totals by viewModel.totals.collectAsState()

    val baseSpecW = totals.specificW
    val baseDeltaT = max(1.0, state.boundary.ti - state.boundary.te)
    val floorDT = state.hydraulics.floorDeltaT

    var selectedSection by remember { mutableStateOf(0) } // 0: Fuels, 1: Rooms/Hydraulics, 2: ROI

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                label = { Text("📊 Energenti (15L)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                label = { Text("🛋️ Talno & Cevi", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                label = { Text("📈 ROI Ukrepi", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        when (selectedSection) {
            0 -> FuelsCompareSection(state = state, totals = totals, onUpdateFuel = { idx, p, eff, inv -> viewModel.updateFuel(idx, p, eff, inv) })
            1 -> RoomsHydraulicsSection(
                state = state,
                baseSpecW = baseSpecW,
                baseDeltaT = baseDeltaT,
                floorDT = floorDT,
                onAddRoom = { viewModel.addRoom() },
                onRemoveRoom = { viewModel.removeRoom(it) },
                onUpdateRoom = { idx, n, a, ti, exp, sp -> viewModel.updateRoom(idx, n, a, ti, exp, sp) }
            )
            2 -> RoiSection(
                state = state,
                totals = totals,
                onAddMeasure = { viewModel.addRoiMeasure() },
                onRemoveMeasure = { viewModel.removeRoiMeasure(it) },
                onUpdateMeasure = { idx, n, a, ub, ua, inv, sub -> viewModel.updateRoiMeasure(idx, n, a, ub, ua, inv, sub) },
                onSelectFuel = { viewModel.updateRoiFuelId(it) }
            )
        }
    }
}

@Composable
private fun FuelsCompareSection(
    state: com.example.data.AppState,
    totals: com.example.model.AllTotals,
    onUpdateFuel: (Int, Double?, Double?, Double?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📊 Dolgoročna primerjava stroškov ogrevanja (15 let)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Toplota: ${(totals.netHeatingKwh + totals.annualDhwKwh).toInt()} kWh/leto (Ogrevanje + STV)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            state.fuels.forEachIndexed { fIdx, f ->
                val c = CalculationEngine.calcFuelAnnualCost(
                    fuel = f,
                    dhwSettings = state.dhwAndWell,
                    primaryElFuel = state.fuels.firstOrNull() ?: f,
                    heatingKwh = totals.netHeatingKwh,
                    dhwKwh = totals.annualDhwKwh
                )
                val cost15 = c.annualEur * 15.0
                val total15 = f.invest + cost15

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = f.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${c.annualEur.toInt()} €/leto",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = f.invest.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateFuel(fIdx, null, null, it) } },
                                label = { Text("Investicija (€)", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = f.price.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateFuel(fIdx, it, null, null) } },
                                label = { Text("Cena (${f.unit})", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = f.eff.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateFuel(fIdx, null, it, null) } },
                                label = { Text("SCOP / Izk.", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("15L energija: ${cost15.toInt()} €", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Skupaj 15L: ${total15.toInt()} €", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomsHydraulicsSection(
    state: com.example.data.AppState,
    baseSpecW: Double,
    baseDeltaT: Double,
    floorDT: Double,
    onAddRoom: () -> Unit,
    onRemoveRoom: (Int) -> Unit,
    onUpdateRoom: (Int, String?, Double?, Double?, Double?, Double?) -> Unit
) {
    var sumArea = 0.0
    var sumWatts = 0.0
    var sumPipes = 0
    var sumLoops = 0
    var sumFlow = 0.0

    state.rooms.forEach { r ->
        val roomSpecW = baseSpecW * (max(1.0, r.ti - state.boundary.te) / baseDeltaT) * r.expFactor
        val roomWatts = r.area * roomSpecW
        val pipeLen = if (r.area > 0) ((r.area / (r.spacing / 100.0)) + 10.0).toInt() else 0
        val loops = if (pipeLen > 0) max(1, ceil(pipeLen / 95.0).toInt()) else 0
        val flowLmin = roomWatts / (69.78 * floorDT)

        sumArea += r.area
        sumWatts += roomWatts
        sumPipes += pipeLen
        sumLoops += loops
        sumFlow += flowLmin
    }

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
                    Text("🛋️ Talno gretje & Hidravlika", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Pretoki (L/min) in cevi po prostorih", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onAddRoom,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_room_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dodaj sobo", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            state.rooms.forEachIndexed { idx, r ->
                val roomSpecW = baseSpecW * (max(1.0, r.ti - state.boundary.te) / baseDeltaT) * r.expFactor
                val roomWatts = r.area * roomSpecW
                val pipeLen = if (r.area > 0) ((r.area / (r.spacing / 100.0)) + 10.0).toInt() else 0
                val loops = if (pipeLen > 0) max(1, ceil(pipeLen / 95.0).toInt()) else 0
                val flowLmin = roomWatts / (69.78 * floorDT)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = r.name,
                                onValueChange = { onUpdateRoom(idx, it, null, null, null, null) },
                                label = { Text("Prostor", fontSize = 9.sp) },
                                modifier = Modifier.weight(1.8f),
                                singleLine = true
                            )
                            IconButton(onClick = { onRemoveRoom(idx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Izbriši", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = r.area.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateRoom(idx, null, it, null, null, null) } },
                                label = { Text("m²", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = r.ti.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateRoom(idx, null, null, it, null, null) } },
                                label = { Text("Ti (°C)", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = r.spacing.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateRoom(idx, null, null, null, null, it) } },
                                label = { Text("Razmak cm", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Moč: ${roomWatts.toInt()} W (${roomSpecW.toInt()} W/m²)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Cevi: ${pipeLen} m (${loops}x zank)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Pretok: ${String.format("%.2f", flowLmin)} L/min", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Totals
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
                        Text("SKUPAJ TLORIS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("${sumArea.toInt()} m²", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("VSOTA MOČI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("${sumWatts.toInt()} W", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("DOLŽINA CEVI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("${sumPipes} m (${sumLoops} zank)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SKUPNI PRETOK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("${String.format("%.1f", sumFlow)} L/min", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoiSection(
    state: com.example.data.AppState,
    totals: com.example.model.AllTotals,
    onAddMeasure: () -> Unit,
    onRemoveMeasure: (Int) -> Unit,
    onUpdateMeasure: (Int, String?, Double?, Double?, Double?, Double?, Double?) -> Unit,
    onSelectFuel: (String) -> Unit
) {
    val deltaT = max(1.0, state.boundary.ti - state.boundary.te)
    val hours = state.boundary.hours
    val selFuel = state.fuels.find { it.id == state.roiFuelId } ?: state.fuels.firstOrNull() ?: Fuel("tc_zrak", "TČ", "€", 0.17, 3.5, 1.0)
    val costResult = CalculationEngine.calcFuelAnnualCost(
        fuel = selFuel,
        dhwSettings = state.dhwAndWell,
        primaryElFuel = state.fuels.firstOrNull() ?: selFuel,
        heatingKwh = 1000.0,
        dhwKwh = 0.0
    )
    val eurPerKwh = costResult.costPerEffectiveKwh

    var sumSavedW = 0.0
    var sumSavedKwh = 0.0
    var sumSavedEur = 0.0
    var sumNetInvest = 0.0

    state.roiMeasures.forEach { m ->
        val deltaU = max(0.0, m.uBefore - m.uAfter)
        val savedW = m.area * deltaU * deltaT
        val savedKwh = (savedW / 1000.0) * hours
        val savedEur = savedKwh * eurPerKwh
        val netInv = max(0.0, m.invest - m.subsidy)
        sumSavedW += savedW
        sumSavedKwh += savedKwh
        sumSavedEur += savedEur
        sumNetInvest += netInv
    }

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
                    Text("📈 Investicije Prej / Potem (ROI)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Prihranki z energetsko sanacijo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onAddMeasure,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_roi_measure_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dodaj ukrep", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            state.roiMeasures.forEachIndexed { idx, m ->
                val deltaU = max(0.0, m.uBefore - m.uAfter)
                val savedW = m.area * deltaU * deltaT
                val savedKwh = (savedW / 1000.0) * hours
                val savedEur = savedKwh * eurPerKwh
                val netInv = max(0.0, m.invest - m.subsidy)
                val payback = if (savedEur > 0) (netInv / savedEur) else 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = m.name,
                                onValueChange = { onUpdateMeasure(idx, it, null, null, null, null, null) },
                                label = { Text("Ukrep", fontSize = 9.sp) },
                                modifier = Modifier.weight(1.8f),
                                singleLine = true
                            )
                            IconButton(onClick = { onRemoveMeasure(idx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Izbriši", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = m.area.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateMeasure(idx, null, it, null, null, null, null) } },
                                label = { Text("m²", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = m.uBefore.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateMeasure(idx, null, null, it, null, null, null) } },
                                label = { Text("U prej", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = m.uAfter.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateMeasure(idx, null, null, null, it, null, null) } },
                                label = { Text("U potem", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = m.invest.toInt().toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateMeasure(idx, null, null, null, null, it, null) } },
                                label = { Text("Cena €", fontSize = 9.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Prihranek: -${savedW.toInt()} W (-${savedKwh.toInt()} kWh)", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            Text("~${savedEur.toInt()} €/leto", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("Doba: ${if (payback > 0) String.format("%.1f", payback) + " let" else "-"}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
                        Text("INVESTICIJA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("${sumNetInvest.toInt()} €", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("ZMANJŠANJE MOČI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("-${String.format("%.2f", sumSavedW / 1000.0)} kW", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LETNI PRIHRANEK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text("${sumSavedEur.toInt()} €/leto (${if (sumSavedEur > 0) String.format("%.1f", sumNetInvest / sumSavedEur) + " let" else "-"})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
