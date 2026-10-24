package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationCity
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
import com.example.model.BoundaryCondition
import com.example.model.CalculationEngine

@Composable
fun BoundaryCard(
    boundary: BoundaryCondition,
    onUpdateBoundary: (ti: Double, te: Double, phiI: Double, phiE: Double, heatedArea: Double, hours: Double) -> Unit,
    onSelectCityTe: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showCityMenu by remember { mutableStateOf(false) }

    val deltaT = boundary.ti - boundary.te

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("boundary_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "🌡️", fontSize = 18.sp)
                    Column {
                        Text(
                            text = "Klimatski & robni pogoji",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ti: ${boundary.ti}°C | Te: ${boundary.te}°C | A: ${boundary.heatedArea.toInt()} m²",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ΔT = ${String.format("%.1f", deltaT)} K",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Zapri" else "Odpri"
                        )
                    }
                }
            }

            // Quick City Selector Button
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                OutlinedButton(
                    onClick = { showCityMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_preset_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Izberi slovensko mesto (hitra cona Te)",
                        fontSize = 13.sp
                    )
                }

                DropdownMenu(
                    expanded = showCityMenu,
                    onDismissRequest = { showCityMenu = false }
                ) {
                    CalculationEngine.SLOVENE_CITIES.forEach { city ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = city.name, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "${city.te.toInt()} °C",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                onSelectCityTe(city.te)
                                showCityMenu = false
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = boundary.ti.toString(),
                            onValueChange = { v ->
                                v.toDoubleOrNull()?.let { onUpdateBoundary(it, boundary.te, boundary.phiI, boundary.phiE, boundary.heatedArea, boundary.hours) }
                            },
                            label = { Text("Ti (°C)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = boundary.te.toString(),
                            onValueChange = { v ->
                                v.toDoubleOrNull()?.let { onUpdateBoundary(boundary.ti, it, boundary.phiI, boundary.phiE, boundary.heatedArea, boundary.hours) }
                            },
                            label = { Text("Te (°C)", fontSize = 11.sp) },
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
                            value = boundary.phiI.toInt().toString(),
                            onValueChange = { v ->
                                v.toDoubleOrNull()?.let { onUpdateBoundary(boundary.ti, boundary.te, it, boundary.phiE, boundary.heatedArea, boundary.hours) }
                            },
                            label = { Text("Vlaga φi (%)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = boundary.phiE.toInt().toString(),
                            onValueChange = { v ->
                                v.toDoubleOrNull()?.let { onUpdateBoundary(boundary.ti, boundary.te, boundary.phiI, it, boundary.heatedArea, boundary.hours) }
                            },
                            label = { Text("Vlaga φe (%)", fontSize = 11.sp) },
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
                            value = boundary.heatedArea.toInt().toString(),
                            onValueChange = { v ->
                                v.toDoubleOrNull()?.let { onUpdateBoundary(boundary.ti, boundary.te, boundary.phiI, boundary.phiE, it, boundary.hours) }
                            },
                            label = { Text("Tloris A (m²)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = boundary.hours.toInt().toString(),
                            onValueChange = { v ->
                                v.toDoubleOrNull()?.let { onUpdateBoundary(boundary.ti, boundary.te, boundary.phiI, boundary.phiE, boundary.heatedArea, it) }
                            },
                            label = { Text("Ure/leto (h)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
