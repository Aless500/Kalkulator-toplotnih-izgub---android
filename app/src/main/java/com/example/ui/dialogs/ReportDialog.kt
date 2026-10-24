package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppState
import com.example.model.AllTotals
import com.example.model.CalculationEngine
import com.example.model.Material

@Composable
fun ReportDialog(
    state: AppState,
    totals: AllTotals,
    materials: List<Material>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val reportText = buildReportText(state, totals, materials)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TEHNIČNO POROČILO",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ISO 13788 · PURES-3 · ISO 6946",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        IconButton(onClick = { shareReport(context, reportText) }) {
                            Icon(Icons.Default.Share, contentDescription = "Deli", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Zapri")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("GLAVNI REZULTATI STAVBE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Projektne toplotne izgube:", fontSize = 12.sp)
                                    Text("${String.format("%.2f", totals.totalKw)} kW (${totals.totalW.toInt()} W)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Specifična moč:", fontSize = 12.sp)
                                    Text("${String.format("%.1f", totals.specificW)} W/m²", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Letna energija za ogrevanje:", fontSize = 12.sp)
                                    Text("${totals.netHeatingKwh.toInt()} kWh/leto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sanitarna topla voda (STV):", fontSize = 12.sp)
                                    Text("${totals.annualDhwKwh.toInt()} kWh/leto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Skupaj ogrevanje + STV:", fontSize = 12.sp)
                                    Text("${totals.annualKwh.toInt()} kWh/leto", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Specifična poraba za ogrevanje:", fontSize = 12.sp)
                                    Text("${totals.specificKwh.toInt()} kWh/m²a", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    item {
                        Text("1. Konstrukcijski sklopi ovoja:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                listOf(
                                    state.walls to "Zid",
                                    state.roofs to "Streha",
                                    state.floors to "Tla"
                                ).forEach { (group, _) ->
                                    group.forEach { el ->
                                        val res = CalculationEngine.calcAssembly(el, state.boundary, materials)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1.5f)) {
                                                Text(el.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text("A = ${el.area.toInt()} m² | debelina ${String.format("%.1f", res.sumD)} cm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                Text("U = ${String.format("%.3f", res.uVal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text("Izguba: ${res.qLoss.toInt()} W", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("2. Okna, vrata in toplotni mostovi:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Transmisijska izguba oken:", fontSize = 11.sp)
                                    Text("${totals.windowsQ.toInt()} W", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Zimski solarni dobitki oken:", fontSize = 11.sp)
                                    Text("+${totals.solarGainKwh.toInt()} kWh/leto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Izguba zunanjih vrat:", fontSize = 11.sp)
                                    Text("${totals.doorsQ.toInt()} W", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Toplotni mostovi (Ψ):", fontSize = 11.sp)
                                    Text("${totals.bridgesQ.toInt()} W", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Prezračevalne izgube (η=${state.ventilation.heatRecoveryEff.toInt()}%):", fontSize = 11.sp)
                                    Text("${totals.ventQ.toInt()} W", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text("3. Toplotna črpalka in sončna elektrarna:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("El. odjem TČ pri ${state.boundary.te}°C:", fontSize = 11.sp)
                                    Text("${String.format("%.2f", totals.effectiveHpElAtTeKw)} kW el.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sistemski SCOP (ogrevanje + STV):", fontSize = 11.sp)
                                    Text("${String.format("%.2f", totals.dhwWell.systemScop)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SE (${state.solarAndGrid.pvKwp} kWp) + Baterija (${state.solarAndGrid.batteryCapKwh} kWh):", fontSize = 11.sp)
                                    Text("${String.format("%.1f", totals.solarBatSim.selfSuffWithBatAnnualPct)}% samooskrba", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Preostali dokup iz omrežja:", fontSize = 11.sp)
                                    Text("${totals.solarBatSim.sumGridWithBat.toInt()} kWh/leto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Zapri")
                    }
                    Button(
                        onClick = { shareReport(context, reportText) },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deli poročilo")
                    }
                }
            }
        }
    }
}

private fun buildReportText(
    state: AppState,
    totals: AllTotals,
    materials: List<Material>
): String {
    val sb = StringBuilder()
    sb.appendLine("TEHNIČNO POROČILO: TOPLOTNE IZGUBE IN ENERGETIKA STAVBE")
    sb.appendLine("Standardi: ISO 13788, ISO 6946, PURES-3")
    sb.appendLine("--------------------------------------------------")
    sb.appendLine("ROBNI POGOJI:")
    sb.appendLine("• Notranja temp. Ti: ${state.boundary.ti} °C (vlaga ${state.boundary.phiI.toInt()}%)")
    sb.appendLine("• Zunanja projektna temp. Te: ${state.boundary.te} °C (vlaga ${state.boundary.phiE.toInt()}%)")
    sb.appendLine("• Ogrevana površina: ${state.boundary.heatedArea.toInt()} m²")
    sb.appendLine("• Ure obratovanja: ${state.boundary.hours.toInt()} h/leto")
    sb.appendLine()
    sb.appendLine("REZULTATI:")
    sb.appendLine("• Projektne toplotne izgube: ${String.format("%.2f", totals.totalKw)} kW (${totals.totalW.toInt()} W)")
    sb.appendLine("• Specifična toplotna izguba: ${String.format("%.1f", totals.specificW)} W/m²")
    sb.appendLine("• Letna toplota za ogrevanje: ${totals.netHeatingKwh.toInt()} kWh/leto")
    sb.appendLine("• Letna toplota za STV: ${totals.annualDhwKwh.toInt()} kWh/leto")
    sb.appendLine("• Skupna letna toplota: ${totals.annualKwh.toInt()} kWh/leto")
    sb.appendLine("• Specifična letna toplota: ${totals.specificKwh.toInt()} kWh/m²a")
    sb.appendLine()
    sb.appendLine("SESTAVA OVOJA:")
    listOf(state.walls, state.roofs, state.floors).forEach { list ->
        list.forEach { el ->
            val r = CalculationEngine.calcAssembly(el, state.boundary, materials)
            sb.appendLine("• ${el.name} | A=${el.area.toInt()} m² | U=${String.format("%.3f", r.uVal)} W/m²K | Izguba: ${r.qLoss.toInt()} W | Zamik: ${String.format("%.1f", r.phaseShiftHours)}h")
        }
    }
    sb.appendLine()
    sb.appendLine("OGREVANJE IN SONČNA ELEKTRARNA:")
    sb.appendLine("• Toplotna črpalka odjem pri ${state.boundary.te}°C: ${String.format("%.2f", totals.effectiveHpElAtTeKw)} kW el.")
    sb.appendLine("• Sončna elektrarna (${state.solarAndGrid.pvKwp} kWp) + Baterija (${state.solarAndGrid.batteryCapKwh} kWh)")
    sb.appendLine("• Stopnja samooskrbe: ${String.format("%.1f", totals.solarBatSim.selfSuffWithBatAnnualPct)}%")
    sb.appendLine("• Preostali dokup: ${totals.solarBatSim.sumGridWithBat.toInt()} kWh/leto")

    return sb.toString()
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Poročilo toplotnih izgub stavbe")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Deli poročilo"))
}
