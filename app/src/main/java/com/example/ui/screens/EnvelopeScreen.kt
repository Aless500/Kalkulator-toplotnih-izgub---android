package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.model.*
import com.example.ui.components.GlaserDiagramCanvas
import com.example.viewmodel.CalculatorViewModel
import kotlin.math.min

@Composable
fun EnvelopeScreen(
    groupKey: String, // "walls", "roofs", "floors"
    title: String,
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val materials by viewModel.materials.collectAsState()

    val list = when (groupKey) {
        "walls" -> state.walls
        "roofs" -> state.roofs
        else -> state.floors
    }

    val stdLimit = CalculationEngine.STANDARD_LIMITS[groupKey] ?: Pair(0.18, 0.15)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group Header & Add Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "PURES-3 meja: U ≤ ${stdLimit.first} W/m²K · Pasivni standard: U ≤ ${stdLimit.second} W/m²K",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.addElement(groupKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_assembly_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ Dodaj nov konstrukcijski sklop",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        list.forEachIndexed { elIdx, el ->
            AssemblyElementCard(
                groupKey = groupKey,
                elIdx = elIdx,
                element = el,
                boundary = state.boundary,
                materials = materials,
                stdLimit = stdLimit,
                rafterWidthCm = state.rafterWidthCm,
                rafterSpacingCm = state.rafterSpacingCm,
                onUpdateElement = { name, area, fx ->
                    viewModel.updateElementField(groupKey, elIdx, name, area, fx)
                },
                onCloneElement = { viewModel.cloneElement(groupKey, elIdx) },
                onRemoveElement = { viewModel.removeElement(groupKey, elIdx) },
                onAddLayer = { viewModel.addLayer(groupKey, elIdx) },
                onRemoveLayer = { lIdx -> viewModel.removeLayer(groupKey, elIdx, lIdx) },
                onMoveLayer = { lIdx, dir -> viewModel.moveLayer(groupKey, elIdx, lIdx, dir) },
                onUpdateLayer = { lIdx, d, lambda, woodPct ->
                    viewModel.updateLayer(groupKey, elIdx, lIdx, d = d, lambda = lambda, woodPct = woodPct)
                },
                onSelectMaterial = { lIdx, matId ->
                    viewModel.selectMaterialForLayer(groupKey, elIdx, lIdx, matId)
                },
                onUpdateRafterHelper = { w, s -> viewModel.updateRafterHelper(w, s) },
                onApplyRafterPct = { lIdx, pct -> viewModel.applyRafterPctToLayer(groupKey, elIdx, lIdx, pct) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssemblyElementCard(
    groupKey: String,
    elIdx: Int,
    element: EnvelopeElement,
    boundary: BoundaryCondition,
    materials: List<Material>,
    stdLimit: Pair<Double, Double>,
    rafterWidthCm: Double,
    rafterSpacingCm: Double,
    onUpdateElement: (name: String?, area: Double?, fx: Double?) -> Unit,
    onCloneElement: () -> Unit,
    onRemoveElement: () -> Unit,
    onAddLayer: () -> Unit,
    onRemoveLayer: (Int) -> Unit,
    onMoveLayer: (Int, Int) -> Unit,
    onUpdateLayer: (Int, Double?, Double?, Double?) -> Unit,
    onSelectMaterial: (Int, String) -> Unit,
    onUpdateRafterHelper: (Double?, Double?) -> Unit,
    onApplyRafterPct: (Int, Double) -> Unit
) {
    val res = remember(element, boundary, materials) {
        CalculationEngine.calcAssembly(element, boundary, materials)
    }

    val isCokel = element.name.contains("cokel", ignoreCase = true)
    val puresLimit = if (isCokel) 0.25 else stdLimit.first
    val passiveLimit = if (isCokel) 0.20 else stdLimit.second
    val meetsPures = res.uVal <= puresLimit
    val meetsPassive = res.uVal <= passiveLimit
    val hasCond = res.condensationZones.isNotEmpty()
    val moldOk = res.fRsi >= 0.70

    val reqPuresCm = remember(element, boundary, materials) {
        CalculationEngine.calcRequiredInsulationThickness(element, boundary, materials, puresLimit, 0.031)
    }
    val reqPassivCm = remember(element, boundary, materials) {
        CalculationEngine.calcRequiredInsulationThickness(element, boundary, materials, passiveLimit, 0.031)
    }

    val calcWoodPct = min(90.0, (rafterWidthCm / rafterSpacingCm) * 100.0)

    var showRafterBar by remember { mutableStateOf(groupKey == "roofs") }
    var selectedRafterLayerIdx by remember { mutableStateOf(element.rafterTargetLayer.coerceIn(0, maxOf(0, element.layers.size - 1))) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header: Name, Area, Fx, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = element.name,
                    onValueChange = { onUpdateElement(it, null, null) },
                    label = { Text("Naziv sklopa", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.8f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = element.area.toInt().toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateElement(null, it, null) } },
                    label = { Text("A (m²)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.9f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onCloneElement, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Podvoji", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onRemoveElement, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Izbriši", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fx selector
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Lokacija (fx):", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                FilterChip(
                    selected = element.fx == 1.0,
                    onClick = { onUpdateElement(null, null, 1.0) },
                    label = { Text("Zunanja (1.0)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = element.fx == 0.8,
                    onClick = { onUpdateElement(null, null, 0.8) },
                    label = { Text("Podstrešje (0.8)", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = element.fx == 0.5,
                    onClick = { onUpdateElement(null, null, 0.5) },
                    label = { Text("Klet / zemlja (0.5)", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PREHODNOST U",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${String.format("%.3f", res.uVal)} W/m²K",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (meetsPures) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Text(
                            text = "Debelina: ${String.format("%.1f", res.sumD)} cm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text(
                            text = "ZAMIK Φ (POLETJE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${String.format("%.1f", res.phaseShiftHours)} h",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (res.phaseShiftHours >= 9) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                        Text(
                            text = "f = ${String.format("%.2f", res.decrementFactor)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOPLOTNA IZGUBA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${res.qLoss.toInt()} W",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (meetsPures) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (meetsPures) "✓ PURES" else "✗ > PURES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (meetsPures) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glaser Dew-Point Diagram & Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💧 Glaserjev diagram (Rosišče & Vlaga)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (hasCond) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("⚠ Kondenz!", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("✓ Brez kondenza", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    GlaserDiagramCanvas(
                        element = element,
                        result = res,
                        materials = materials,
                        ti = boundary.ti,
                        te = boundary.te
                    )

                    Text(
                        text = "Sd = ${String.format("%.2f", res.sumSd)} m | Tsi = ${String.format("%.1f", res.tsi)} °C | Dotakni se grafa za pregled temperatur po plasteh.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layers List
            Text(
                text = "SLOJI SESTAVE (od znotraj proti zunanjosti):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            element.layers.forEachIndexed { lIdx, layer ->
                LayerItemRow(
                    index = lIdx,
                    layer = layer,
                    materials = materials,
                    canMoveUp = lIdx > 0,
                    canMoveDown = lIdx < element.layers.size - 1,
                    canDelete = element.layers.size > 1,
                    onMoveUp = { onMoveLayer(lIdx, -1) },
                    onMoveDown = { onMoveLayer(lIdx, 1) },
                    onDelete = { onRemoveLayer(lIdx) },
                    onUpdateD = { d -> onUpdateLayer(lIdx, d, null, null) },
                    onUpdateLambda = { lam -> onUpdateLayer(lIdx, null, lam, null) },
                    onUpdateWoodPct = { wp -> onUpdateLayer(lIdx, null, null, wp) },
                    onSelectMat = { matId -> onSelectMaterial(lIdx, matId) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Actions row: Add Layer & Rafter Helper Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddLayer,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dodaj sloj", fontSize = 12.sp)
                }

                TextButton(
                    onClick = { showRafterBar = !showRafterBar },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(if (showRafterBar) "Skrij špirovce" else "🪵 Špirovci / les", fontSize = 12.sp)
                }
            }

            // Rafter percentage helper
            AnimatedVisibility(visible = showRafterBar) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🪵 Kalkulator deleža špirovcev / lesenih stebrov:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = rafterWidthCm.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateRafterHelper(it, null) } },
                                label = { Text("Širina (cm)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text("÷", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = rafterSpacingCm.toString(),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateRafterHelper(null, it) } },
                                label = { Text("Razmak (cm)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text("=", fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format("%.1f", calcWoodPct)}%",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prenesi v sloj #${selectedRafterLayerIdx + 1}:",
                                fontSize = 11.sp
                            )
                            Button(
                                onClick = { onApplyRafterPct(selectedRafterLayerIdx, calcWoodPct) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Prenesi ${String.format("%.1f", calcWoodPct)}%", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Insulation advice
            Text(
                text = "💡 Pametni nasvet za izolacijo (λ=0.031): PURES min ~${reqPuresCm} cm, pasivni ~${reqPassivCm} cm.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerItemRow(
    index: Int,
    layer: Layer,
    materials: List<Material>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onUpdateD: (Double) -> Unit,
    onUpdateLambda: (Double) -> Unit,
    onUpdateWoodPct: (Double) -> Unit,
    onSelectMat: (String) -> Unit
) {
    var showMatDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (layer.woodPct > 0) Color(0xFFF59E0B).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(22.dp)
                    )
                    TextButton(
                        onClick = { showMatDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = layer.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canMoveUp) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Gor", modifier = Modifier.size(15.dp))
                        }
                    }
                    if (canMoveDown) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Dol", modifier = Modifier.size(15.dp))
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Odstrani", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = layer.d.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateD(it) } },
                    label = { Text("d (cm)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = layer.lambda.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateLambda(it) } },
                    label = { Text("λ (W/mK)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = layer.woodPct.toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onUpdateWoodPct(it) } },
                    label = { Text("Les (%)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.9f),
                    singleLine = true
                )
            }
        }
    }

    if (showMatDialog) {
        MaterialPickerSheet(
            materials = materials,
            selectedId = layer.matId,
            onSelect = { matId ->
                onSelectMat(matId)
                showMatDialog = false
            },
            onDismiss = { showMatDialog = false }
        )
    }
}

@Composable
fun MaterialPickerSheet(
    materials: List<Material>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val grouped = remember(materials, searchQuery) {
        materials
            .filter { it.name.contains(searchQuery, ignoreCase = true) || it.group.contains(searchQuery, ignoreCase = true) }
            .groupBy { it.group }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izberi material", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Išči material (npr. EPS, volna, opeka)...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    grouped.forEach { (groupName, matsInGroup) ->
                        item {
                            Text(
                                text = groupName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(matsInGroup.size) { i ->
                            val m = matsInGroup[i]
                            val isSel = m.id == selectedId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(m.id) },
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = m.name, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            text = "λ=${m.lambda} W/mK | μ=${m.mu.toInt()} | ρ=${m.rho.toInt()} kg/m³",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSel) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zapri") }
        }
    )
}
