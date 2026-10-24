package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BoundaryCard
import com.example.ui.components.SummaryCard
import com.example.ui.dialogs.ReportDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsState()
            val totals by viewModel.totals.collectAsState()
            val materials by viewModel.materials.collectAsState()

            var showReportDialog by remember { mutableStateOf(false) }
            var showResetConfirmDialog by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = state.isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Toplotne izgube",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "v10.2",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Scenario A button
                                IconButton(
                                    onClick = {
                                        if (state.scenarioA == null) viewModel.saveScenarioA()
                                        else viewModel.clearScenarioA()
                                    },
                                    modifier = Modifier.testTag("scenario_a_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Scenarij A",
                                        tint = if (state.scenarioA != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Full Report button
                                IconButton(
                                    onClick = { showReportDialog = true },
                                    modifier = Modifier.testTag("report_button")
                                ) {
                                    Icon(Icons.Default.Assessment, contentDescription = "Poročilo")
                                }

                                // Dark Mode Toggle
                                IconButton(onClick = { viewModel.toggleDarkMode() }) {
                                    Icon(
                                        imageVector = if (state.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Način prikaza"
                                    )
                                }

                                // Reset button
                                IconButton(onClick = { showResetConfirmDialog = true }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Ponastavi")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    val tabs = remember(totals) {
                        listOf(
                            TabItem("walls", "🧱 Zidovi", "${totals.wallsQ.toInt()} W"),
                            TabItem("roofs", "🏠 Streha", "${totals.roofsQ.toInt()} W"),
                            TabItem("floors", "🪵 Tla", "${totals.floorsQ.toInt()} W"),
                            TabItem("windows", "🪟 Okna & Vrata", "${(totals.windowsQ + totals.doorsQ).toInt()} W"),
                            TabItem("bridges", "📐 Mostovi", "${totals.bridgesQ.toInt()} W"),
                            TabItem("ventilation", "💨 Prezračevanje", "${totals.ventQ.toInt()} W"),
                            TabItem("dhw", "🚿 STV & Vrtina", "${totals.annualDhwKwh.toInt()} kWh"),
                            TabItem("hp", "⚙️ TČ & Varovalke", "${String.format("%.1f", totals.effectiveHpElAtTeKw)} kW"),
                            TabItem("solar", "🔋 SE & Števec", "${totals.solarBatSim.selfSuffWithBatAnnualPct.toInt()}%"),
                            TabItem("fuels", "📊 Energenti & ROI", "15L"),
                            TabItem("bom", "📦 Popis & Materiali", "BOM")
                        )
                    }

                    val selectedTabIndex = tabs.indexOfFirst { it.id == state.activeTabId }.let { if (it >= 0) it else 0 }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Climatic and boundary card
                        BoundaryCard(
                            boundary = state.boundary,
                            onUpdateBoundary = { ti, te, phiI, phiE, heatedArea, hours ->
                                viewModel.updateBoundary(ti, te, phiI, phiE, heatedArea, hours)
                            },
                            onSelectCityTe = { te -> viewModel.applyCityPreset(te) }
                        )

                        // Hero Energy Summary Card with EPC scale
                        SummaryCard(
                            totals = totals,
                            scenarioA = state.scenarioA,
                            onClearScenarioA = { viewModel.clearScenarioA() }
                        )

                        // Scrollable Category Tabs
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { idx, tab ->
                                val isSelected = selectedTabIndex == idx
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.switchTab(tab.id) },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tab.label,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tab.badge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Tab Screen Content
                        when (state.activeTabId) {
                            "walls" -> EnvelopeScreen(
                                groupKey = "walls",
                                title = "🧱 Zidovi (zunanje stene in cokel)",
                                viewModel = viewModel
                            )
                            "roofs" -> EnvelopeScreen(
                                groupKey = "roofs",
                                title = "🏠 Streha in mansarda (špirovci & kritina)",
                                viewModel = viewModel
                            )
                            "floors" -> EnvelopeScreen(
                                groupKey = "floors",
                                title = "🪵 Tla na terenu & kletne plošče",
                                viewModel = viewModel
                            )
                            "windows" -> WindowsDoorsScreen(viewModel = viewModel)
                            "bridges" -> BridgesVentScreen(viewModel = viewModel)
                            "ventilation" -> BridgesVentScreen(viewModel = viewModel)
                            "dhw" -> DhwHpScreen(viewModel = viewModel)
                            "hp" -> DhwHpScreen(viewModel = viewModel)
                            "solar" -> SolarMeterScreen(viewModel = viewModel)
                            "fuels" -> FuelsRoomsRoiScreen(viewModel = viewModel)
                            "bom" -> BomMaterialsScreen(viewModel = viewModel)
                            else -> EnvelopeScreen(
                                groupKey = "walls",
                                title = "🧱 Zidovi",
                                viewModel = viewModel
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                if (showReportDialog) {
                    ReportDialog(
                        state = state,
                        totals = totals,
                        materials = materials,
                        onDismiss = { showReportDialog = false }
                    )
                }

                if (showResetConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirmDialog = false },
                        title = { Text("Ponastavi podatke?") },
                        text = { Text("Ali želiš ponastaviti podatke hiše na privzeti začetni primer? Tvoji dodani materiali se ohranijo.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.resetToDefault()
                                    showResetConfirmDialog = false
                                }
                            ) {
                                Text("Ponastavi")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetConfirmDialog = false }) {
                                Text("Prekliči")
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class TabItem(val id: String, val label: String, val badge: String)
