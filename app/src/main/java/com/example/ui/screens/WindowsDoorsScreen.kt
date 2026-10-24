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
import com.example.model.DoorElement
import com.example.model.WindowElement
import com.example.viewmodel.CalculatorViewModel
import kotlin.math.max

@Composable
fun WindowsDoorsScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val deltaT = max(0.0, state.boundary.ti - state.boundary.te)

    var totalWinArea = 0.0
    var totalWinLoss = 0.0
    var totalWinGain = 0.0

    state.windows.forEach { w ->
        val area = w.w * w.h * w.count
        val loss = area * w.uw * deltaT * w.fx
        val orientObj = CalculationEngine.SOLAR_WINTER_IRR[w.orient] ?: CalculationEngine.SOLAR_WINTER_IRR["J"]!!
        val gain = area * orientObj.kwhM2 * w.g * 0.70 * 0.75 * 0.85
        totalWinArea += area
        totalWinLoss += loss
        totalWinGain += gain
    }

    val totalDoorLoss = state.doors.sumOf { it.area * it.ud * deltaT * it.fx }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Windows Section
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
                            text = "🪟 Okna & Solarni dobitki",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Troslojna/dvoslojna zasteklitev in orientacija sonca",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.addWindow() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_window_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dodaj okno", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                state.windows.forEachIndexed { idx, win ->
                    WindowItemCard(
                        window = win,
                        deltaT = deltaT,
                        onUpdate = { name, w, h, count, uw, g, orient ->
                            viewModel.updateWindow(idx, name, w, h, count, uw, g, orient)
                        },
                        onDelete = { viewModel.removeWindow(idx) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Summary
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
                            Text("POVRŠINA OKEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${String.format("%.2f", totalWinArea)} m²", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("IZGUBA OKEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${totalWinLoss.toInt()} W", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SOLARNI DOBITKI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("+${totalWinGain.toInt()} kWh/leto", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        // Doors Section
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
                            text = "🚪 Vrata in podstrešne odprtine",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Izguba vrat: ${totalDoorLoss.toInt()} W",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.addDoor() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_door_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dodaj vrata", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                state.doors.forEachIndexed { idx, door ->
                    DoorItemCard(
                        door = door,
                        deltaT = deltaT,
                        onUpdate = { name, area, ud, fx ->
                            viewModel.updateDoor(idx, name, area, ud, fx)
                        },
                        onDelete = { viewModel.removeDoor(idx) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WindowItemCard(
    window: WindowElement,
    deltaT: Double,
    onUpdate: (name: String?, w: Double?, h: Double?, count: Int?, uw: Double?, g: Double?, orient: String?) -> Unit,
    onDelete: () -> Unit
) {
    val area = window.w * window.h * window.count
    val loss = area * window.uw * deltaT * window.fx
    val orientObj = CalculationEngine.SOLAR_WINTER_IRR[window.orient] ?: CalculationEngine.SOLAR_WINTER_IRR["J"]!!
    val gain = area * orientObj.kwhM2 * window.g * 0.70 * 0.75 * 0.85

    var showOrientMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    value = window.name,
                    onValueChange = { onUpdate(it, null, null, null, null, null, null) },
                    label = { Text("Opis", fontSize = 10.sp) },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedButton(
                        onClick = { showOrientMenu = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(orientObj.label, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = showOrientMenu, onDismissRequest = { showOrientMenu = false }) {
                        CalculationEngine.SOLAR_WINTER_IRR.forEach { (key, ori) ->
                            DropdownMenuItem(
                                text = { Text(ori.label, fontSize = 12.sp) },
                                onClick = {
                                    onUpdate(null, null, null, null, null, null, key)
                                    showOrientMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
                    value = window.w.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, it, null, null, null, null, null) } },
                    label = { Text("Š (m)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = window.h.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, null, it, null, null, null, null) } },
                    label = { Text("V (m)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = window.count.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { onUpdate(null, null, null, it, null, null, null) } },
                    label = { Text("Kos", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = window.uw.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, null, null, null, it, null, null) } },
                    label = { Text("Uw", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = window.g.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, null, null, null, null, it, null) } },
                    label = { Text("g", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.9f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("A: ${String.format("%.2f", area)} m²", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Izguba: ${loss.toInt()} W", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Dobitek: +${gain.toInt()} kWh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun DoorItemCard(
    door: DoorElement,
    deltaT: Double,
    onUpdate: (name: String?, area: Double?, ud: Double?, fx: Double?) -> Unit,
    onDelete: () -> Unit
) {
    val loss = door.area * door.ud * deltaT * door.fx

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    value = door.name,
                    onValueChange = { onUpdate(it, null, null, null) },
                    label = { Text("Naziv vrat", fontSize = 10.sp) },
                    modifier = Modifier.weight(1.8f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
                    value = door.area.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, it, null, null) } },
                    label = { Text("A (m²)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = door.ud.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, null, it, null) } },
                    label = { Text("Ud", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = door.fx.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdate(null, null, null, it) } },
                    label = { Text("fx (1.0/0.8)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Izguba:", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    Text("${loss.toInt()} W", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
