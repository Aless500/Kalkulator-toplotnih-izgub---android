package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.model.SolarBatterySimResult
import kotlin.math.max

@Composable
fun SolarBatteryMonthlyChart(
    sim: SolarBatterySimResult,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padL = 70f
            val padR = 20f
            val padT = 45f
            val padB = 40f
            val plotW = w - padL - padR
            val plotH = h - padT - padB

            val maxVal = max(500.0, sim.months.maxOfOrNull { max(it.pvM, it.loadM) } ?: 500.0) * 1.1

            val colW = plotW / 12f

            sim.months.forEachIndexed { i, m ->
                val xBase = padL + i * colW
                val hPv = ((m.pvM / maxVal) * plotH).toFloat()
                val selfCovered = m.directM + m.batM
                val hSelf = ((selfCovered / maxVal) * plotH).toFloat()
                val hGrid = ((m.gridWithBatM / maxVal) * plotH).toFloat()

                // PV bar (amber)
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(xBase + 4f, padT + plotH - hPv),
                    size = Size(colW * 0.38f, hPv)
                )

                // Load bar (Self-covered in green, grid in red)
                val xLoad = xBase + 4f + colW * 0.42f
                drawRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(xLoad, padT + plotH - hSelf),
                    size = Size(colW * 0.38f, hSelf)
                )
                drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(xLoad, padT + plotH - hSelf - hGrid),
                    size = Size(colW * 0.38f, hGrid)
                )

                // Month text
                drawContext.canvas.nativeCanvas.apply {
                    val p = Paint().apply {
                        color = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                        textSize = 22f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(m.monthName, xBase + colW * 0.45f, h - 10f, p)
                }
            }

            // Legend
            drawContext.canvas.nativeCanvas.apply {
                val pPv = Paint().apply {
                    color = android.graphics.Color.rgb(245, 158, 11)
                    textSize = 22f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawText("■ SE sonce", padL, 28f, pPv)

                val pSelf = Paint().apply {
                    color = android.graphics.Color.rgb(16, 185, 129)
                    textSize = 22f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawText("■ Pokrito (SE + Bat)", padL + 140f, 28f, pSelf)

                val pGrid = Paint().apply {
                    color = android.graphics.Color.rgb(239, 68, 68)
                    textSize = 22f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawText("■ Dokup omrežje", padL + 360f, 28f, pGrid)
            }
        }
    }
}
