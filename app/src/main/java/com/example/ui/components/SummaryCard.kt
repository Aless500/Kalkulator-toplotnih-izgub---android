package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AllTotals
import com.example.model.ScenarioSnapshot

@Composable
fun SummaryCard(
    totals: AllTotals,
    scenarioA: ScenarioSnapshot?,
    onClearScenarioA: () -> Unit,
    modifier: Modifier = Modifier
) {
    var breakdownExpanded by remember { mutableStateOf(false) }

    val s = totals.specificKwh
    val (clsKey, stdTitle, stdDesc, badgeColor) = when {
        s <= 10.0 -> Quad("A1", "🏆 Pasivna hiša (PHPP Plus)", "Vrhunska pasivna gradnja (≤ 10 kWh/m²a)", Color(0xFF15803D))
        s <= 15.0 -> Quad("A2", "🏆 Pasivna hiša (PHPP)", "Ustreza kriteriju pasivne hiše (≤ 15 kWh/m²a)", Color(0xFF16A34A))
        s <= 25.0 -> Quad("B1", "🌿 Skoraj ničenergijska (sNEH)", "Sodobna nizkoenergijska gradnja / PURES-3", Color(0xFF22C55E))
        s <= 35.0 -> Quad("B2", "🏡 Nizkoenergijska hiša", "Zelo dobra toplotna zaščita ovoja", Color(0xFF84CC16))
        s <= 60.0 -> Quad("C", "🏠 Dobro izolirana hiša", "Srednja poraba energije za ogrevanje", Color(0xFFEAB308))
        s <= 105.0 -> Quad("D", "🧱 Standardna starejša gradnja", "Priporočena dodatna toplotna izolacija", Color(0xFFF59E0B))
        s <= 150.0 -> Quad("E", "⚠️ Slabo izolirana stavba", "Visoke toplotne izgube", Color(0xFFEA580C))
        else -> Quad("F/G", "🔥 Energetsko potratna stavba", "Nujna celovita energetska sanacija", Color(0xFFDC2626))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Scenario A Comparison Bar (if set)
            scenarioA?.let { sa ->
                val dW = totals.totalW - sa.totalW
                val dKwh = totals.annualKwh - sa.annualKwh
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⚖️ Primerjava s Scenarijem A",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "ΔMoč: ${if (dW > 0) "+" else ""}${dW.toInt()} W",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dW <= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                                Text(
                                    text = "ΔEnergija: ${if (dKwh > 0) "+" else ""}${dKwh.toInt()} kWh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dKwh <= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                        IconButton(onClick = onClearScenarioA, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Počisti scenarij", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Top Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ENERGETSKA IZKAZNICA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stdTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Razred $clsKey (${s.toInt()} kWh/m²a)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // EPC Scale Bar
            EpcScaleBar(specificKwh = s)

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stdDesc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Loss
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PROJEKTNE IZGUBE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${String.format("%.2f", totals.totalKw)} kW",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${totals.totalW.toInt()} W (${String.format("%.1f", totals.specificW)} W/m²)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Annual Heat
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "LETNA TOPLOTA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${totals.annualKwh.toInt()} kWh",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ogrev: ${totals.netHeatingKwh.toInt()} + STV: ${totals.annualDhwKwh.toInt()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compliance Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (totals.envelopeMeetsPures) {
                    AssistChip(
                        onClick = {},
                        label = { Text("✓ PURES-3 ustreza", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF10B981))
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("⚠ Del nad PURES-3", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFFF59E0B))
                    )
                }

                if (totals.envelopeMeetsPassive) {
                    AssistChip(
                        onClick = {},
                        label = { Text("🏆 Pasivni ovoj", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF0284C7))
                    )
                }
            }

            // Breakdown Toggle
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { breakdownExpanded = !breakdownExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Razčlenitev toplotnih izgub po skupinah",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (breakdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = breakdownExpanded) {
                val groups = listOf(
                    Triple("🧱 Zidovi", totals.wallsQ, Color(0xFF0284C7)),
                    Triple("🏠 Streha in strop", totals.roofsQ, Color(0xFFEA580C)),
                    Triple("🪵 Tla", totals.floorsQ, Color(0xFF10B981)),
                    Triple("🪟 Okna", totals.windowsQ, Color(0xFF8B5CF6)),
                    Triple("🚪 Vrata", totals.doorsQ, Color(0xFFF59E0B)),
                    Triple("📐 Toplotni mostovi", totals.bridgesQ, Color(0xFFEF4444)),
                    Triple("💨 Prezračevanje", totals.ventQ, Color(0xFF0D9488))
                )

                Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { (label, wVal, col) ->
                        val pct = if (totals.totalW > 0) (wVal / totals.totalW).toFloat() else 0f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${wVal.toInt()} W (${String.format("%.1f", pct * 100)}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { pct.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = col,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
