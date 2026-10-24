package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
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
import com.example.model.Material
import com.example.viewmodel.CalculatorViewModel

@Composable
fun BomMaterialsScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val materials by viewModel.materials.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: BOM, 1: Materials Library

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("📦 Popis materiala (BOM)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("📚 Knjižnica (${materials.size})", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        if (selectedTab == 0) {
            BomTabContent(
                state = state,
                onUpdateWaste = { viewModel.updateBomWaste(it) }
            )
        } else {
            MaterialsLibraryTabContent(
                materials = materials,
                onAddCustomMaterial = { viewModel.addCustomMaterial(it) },
                onDeleteCustomMaterial = { viewModel.deleteCustomMaterial(it) }
            )
        }
    }
}

private data class BomItem(
    val name: String,
    val dCm: Double,
    val netAreaM2: Double,
    val netVolM3: Double,
    val usedIn: MutableList<String>
)

@Composable
private fun BomTabContent(
    state: com.example.data.AppState,
    onUpdateWaste: (Double) -> Unit
) {
    val wasteFactor = 1.0 + (state.bomWastePct / 100.0)
    val bomMap = remember(state.walls, state.roofs, state.floors, state.bomWastePct) {
        val map = mutableMapOf<String, BomItem>()
        listOf(
            state.walls to "Zid",
            state.roofs to "Streha",
            state.floors to "Tla"
        ).forEach { (groupList, _) ->
            groupList.forEach { el ->
                el.layers.forEach { l ->
                    if (!l.matId.contains("zrak")) {
                        val key = "${l.matId}_${l.d}"
                        val item = map.getOrPut(key) {
                            BomItem(name = l.name, dCm = l.d, netAreaM2 = 0.0, netVolM3 = 0.0, usedIn = mutableListOf())
                        }
                        val effArea = if (l.woodPct > 0) el.area * (1.0 - l.woodPct / 100.0) else el.area
                        val itemCopy = item.copy(
                            netAreaM2 = item.netAreaM2 + effArea,
                            netVolM3 = item.netVolM3 + (effArea * (l.d / 100.0))
                        )
                        if (!itemCopy.usedIn.contains(el.name)) itemCopy.usedIn.add(el.name)
                        map[key] = itemCopy
                    }
                }
            }
        }
        map.values.toList()
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
                    Text("📦 Popis materiala za naročilo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Površine (m²) in volumni (m³) z odrezom", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = state.bomWastePct.toInt().toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateWaste(it) } },
                    label = { Text("Kalo (%)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(85.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            bomMap.forEach { item ->
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(text = "d = ${item.dCm} cm", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Text(
                            text = "Uporabljeno v: ${item.usedIn.joinToString(", ")}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Neto m²:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${String.format("%.1f", item.netAreaM2)} m²", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Za naročilo (+${state.bomWastePct.toInt()}%):", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${String.format("%.1f", item.netAreaM2 * wasteFactor)} m²", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Volumen m³:", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${String.format("%.2f", item.netVolM3 * wasteFactor)} m³", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialsLibraryTabContent(
    materials: List<Material>,
    onAddCustomMaterial: (Material) -> Unit,
    onDeleteCustomMaterial: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(materials, searchQuery) {
        materials.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.group.contains(searchQuery, ignoreCase = true)
        }
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
                    Text("📚 Knjižnica materialov", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Vseh materialov v bazi: ${materials.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_custom_material_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nov material", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Išči med ${materials.size} materiali...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered.size) { idx ->
                    val m = filtered[idx]
                    val isCustom = m.id.startsWith("usr_")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCustom) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = m.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    if (isCustom) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF10B981))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("Moj", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "${m.group} | λ=${m.lambda} W/mK | μ=${m.mu.toInt()} | ρ=${m.rho.toInt()} kg/m³ | c=${m.c.toInt()} J/kgK",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCustom) {
                                IconButton(onClick = { onDeleteCustomMaterial(m.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Izbriši", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNewMaterialDialog(
            onAdd = { newMat ->
                onAddCustomMaterial(newMat)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddNewMaterialDialog(
    onAdd: (Material) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Moji materiali") }
    var lambda by remember { mutableStateOf("0.035") }
    var mu by remember { mutableStateOf("30") }
    var rho by remember { mutableStateOf("50") }
    var c by remember { mutableStateOf("1400") }
    var defD by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj nov material", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naziv materiala", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = lambda,
                        onValueChange = { lambda = it },
                        label = { Text("λ (W/mK)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mu,
                        onValueChange = { mu = it },
                        label = { Text("μ (-)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = rho,
                        onValueChange = { rho = it },
                        label = { Text("ρ (kg/m³)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = c,
                        onValueChange = { c = it },
                        label = { Text("c (J/kgK)", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            Material(
                                id = "usr_${System.currentTimeMillis()}",
                                group = group,
                                name = name.trim(),
                                defD = defD.toDoubleOrNull() ?: 10.0,
                                lambda = lambda.toDoubleOrNull() ?: 0.035,
                                mu = mu.toDoubleOrNull() ?: 30.0,
                                rho = rho.toDoubleOrNull() ?: 50.0,
                                c = c.toDoubleOrNull() ?: 1400.0,
                                colorHex = "#0284c7"
                            )
                        )
                    }
                }
            ) {
                Text("Shrani material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Prekliči") }
        }
    )
}
