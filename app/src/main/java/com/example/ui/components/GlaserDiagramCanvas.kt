package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.AssemblyResult
import com.example.model.CalculationEngine
import com.example.model.EnvelopeElement
import com.example.model.Material
import kotlin.math.max
import kotlin.math.min

@Composable
fun GlaserDiagramCanvas(
    element: EnvelopeElement,
    result: AssemblyResult,
    materials: List<Material>,
    ti: Double,
    te: Double,
    modifier: Modifier = Modifier
) {
    var touchRatio by remember { mutableStateOf<Float?>(null) }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    val teEff = ti - element.fx * (ti - te)
    val minT = min(teEff, (result.nodes.map { it.dewPoint } + listOf(-15.0)).minOrNull() ?: -15.0) - 3.0
    val maxT = max(ti, 25.0) + 3.0

    val padL = 120f
    val padR = 40f
    val padT = 50f
    val padB = 60f

    val totalD = max(1.0, result.sumD)

    val matColorMap = remember(materials) {
        materials.associate { it.id to parseColor(it.colorHex) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .pointerInput(result) {
                detectTapGestures(
                    onPress = { offset ->
                        val plotW = size.width - padL - padR
                        if (plotW > 0) {
                            val ratio = ((offset.x - padL) / plotW).coerceIn(0f, 1f)
                            touchRatio = ratio
                        }
                    }
                )
            }
            .pointerInput(result) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val plotW = size.width - padL - padR
                        if (plotW > 0) {
                            touchRatio = ((offset.x - padL) / plotW).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = { touchRatio = null },
                    onDragCancel = { touchRatio = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val plotW = size.width - padL - padR
                        if (plotW > 0) {
                            touchRatio = ((change.position.x - padL) / plotW).coerceIn(0f, 1f)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val plotW = w - padL - padR
            val plotH = h - padT - padB

            fun yFromT(t: Double): Float {
                val norm = 1.0 - (t - minT) / (maxT - minT)
                return (padT + plotH * norm).toFloat()
            }

            fun xFromCm(cm: Double): Float {
                return (padL + plotW * (cm / totalD)).toFloat()
            }

            // Draw layer backgrounds
            var curCm = 0.0
            result.computedLayers.forEach { layer ->
                val x1 = xFromCm(curCm)
                val x2 = xFromCm(curCm + layer.d)
                val layerW = max(1f, x2 - x1)
                val baseCol = matColorMap[layer.matId] ?: Color(0xFFCBD5E1)

                drawRect(
                    color = baseCol.copy(alpha = 0.28f),
                    topLeft = Offset(x1, padT),
                    size = Size(layerW, plotH)
                )
                drawRect(
                    color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                    topLeft = Offset(x1, padT),
                    size = Size(layerW, plotH),
                    style = Stroke(width = 1f)
                )

                // Label thickness if room
                if (layerW > 40f) {
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = Paint().apply {
                            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
                            textSize = 26f
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(
                            "${layer.d} cm",
                            (x1 + x2) / 2f,
                            padT + 30f,
                            textPaint
                        )
                        if (layer.woodPct > 0) {
                            val woodPaint = Paint().apply {
                                color = android.graphics.Color.rgb(180, 83, 9)
                                textSize = 22f
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                                isAntiAlias = true
                            }
                            drawText(
                                "🪵 ${layer.woodPct}%",
                                (x1 + x2) / 2f,
                                padT + 60f,
                                woodPaint
                            )
                        }
                    }
                }

                curCm += layer.d
            }

            // 0 °C line
            if (minT < 0.0 && maxT > 0.0) {
                val y0 = yFromT(0.0)
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(padL, y0),
                    end = Offset(w - padR, y0),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawContext.canvas.nativeCanvas.apply {
                    val p = Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }
                    drawText("0 °C", padL - 10f, y0 + 8f, p)
                }
            }

            // Shading condensation zones
            for (i in 0 until result.nodes.size - 1) {
                if (result.nodes[i].condenses || result.nodes[i + 1].condenses) {
                    val x1 = xFromCm(result.nodes[i].xCm)
                    val x2 = xFromCm(result.nodes[i + 1].xCm)
                    drawRect(
                        color = Color(0xFFEF4444).copy(alpha = 0.28f),
                        topLeft = Offset(x1, padT),
                        size = Size(x2 - x1, plotH)
                    )
                }
            }

            // Dew point line (Blue dashed)
            val dewPath = Path()
            result.nodes.forEachIndexed { idx, n ->
                val x = xFromCm(n.xCm)
                val y = yFromT(n.dewPoint)
                if (idx == 0) dewPath.moveTo(x, y) else dewPath.lineTo(x, y)
            }
            drawPath(
                path = dewPath,
                color = Color(0xFF0284C7),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Temperature line (Red solid)
            val tempPath = Path()
            result.nodes.forEachIndexed { idx, n ->
                val x = xFromCm(n.xCm)
                val y = yFromT(n.temperature)
                if (idx == 0) tempPath.moveTo(x, y) else tempPath.lineTo(x, y)
            }
            drawPath(
                path = tempPath,
                color = Color(0xFFEF4444),
                style = Stroke(width = 5f)
            )

            // Node dots
            result.nodes.forEach { n ->
                val x = xFromCm(n.xCm)
                drawCircle(color = Color(0xFFEF4444), radius = 6f, center = Offset(x, yFromT(n.temperature)))
                drawCircle(color = Color(0xFF0284C7), radius = 5f, center = Offset(x, yFromT(n.dewPoint)))
            }

            // Labels for Inside and Outside
            drawContext.canvas.nativeCanvas.apply {
                val textPaint = Paint().apply {
                    color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    textSize = 26f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawText("Znotraj (${ti.toInt()}°C)", 10f, padT + 25f, textPaint)

                val tsiPaint = Paint().apply {
                    color = android.graphics.Color.rgb(239, 68, 68)
                    textSize = 24f
                    isAntiAlias = true
                }
                drawText("Tsi: ${String.format("%.1f", result.tsi)}°C", 10f, padT + 60f, tsiPaint)

                val outPaint = Paint().apply {
                    color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    textSize = 26f
                    textAlign = Paint.Align.RIGHT
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                drawText("Zunaj (${String.format("%.1f", teEff)}°C)", w - 10f, h - 15f, outPaint)
            }

            // Touch scrubber inspection
            touchRatio?.let { ratio ->
                val cmPos = ratio * totalD
                val xPx = xFromCm(cmPos)

                drawLine(
                    color = if (isDark) Color.White else Color.Black,
                    start = Offset(xPx, padT),
                    end = Offset(xPx, padT + plotH),
                    strokeWidth = 2.5f
                )

                var segIdx = 0
                for (i in 0 until result.nodes.size - 1) {
                    if (cmPos >= result.nodes[i].xCm && cmPos <= result.nodes[i + 1].xCm) {
                        segIdx = i
                        break
                    }
                }
                val n1 = result.nodes[segIdx]
                val n2 = result.nodes.getOrNull(segIdx + 1) ?: n1
                val span = max(0.001, n2.xCm - n1.xCm)
                val f = ((cmPos - n1.xCm) / span).coerceIn(0.0, 1.0)
                val curT = n1.temperature + f * (n2.temperature - n1.temperature)
                val curTdew = n1.dewPoint + f * (n2.dewPoint - n1.dewPoint)
                val curP = n1.vaporPressure + f * (n2.vaporPressure - n1.vaporPressure)
                val curRh = min(100.0, (curP / CalculationEngine.psat(curT)) * 100.0)
                val layerName = result.computedLayers.getOrNull(segIdx)?.name?.take(20) ?: ""

                // Scrubber tooltip box
                val boxW = 340f
                val boxH = 120f
                val bx = (xPx + 15f).coerceIn(padL + 10f, w - padR - boxW - 10f)
                val by = padT + 20f

                drawRoundRect(
                    color = Color(0xEE0F172A),
                    topLeft = Offset(bx, by),
                    size = Size(boxW, boxH)
                )

                drawContext.canvas.nativeCanvas.apply {
                    val pTitle = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawText("${String.format("%.1f", cmPos)} cm | $layerName", bx + 12f, by + 32f, pTitle)

                    val pT = Paint().apply {
                        color = android.graphics.Color.rgb(252, 165, 165)
                        textSize = 24f
                        isAntiAlias = true
                    }
                    drawText("T = ${String.format("%.1f", curT)} °C", bx + 12f, by + 68f, pT)

                    val pDew = Paint().apply {
                        color = android.graphics.Color.rgb(125, 211, 252)
                        textSize = 24f
                        isAntiAlias = true
                    }
                    drawText("T_ros = ${String.format("%.1f", curTdew)} °C", bx + 175f, by + 68f, pDew)

                    val pRh = Paint().apply {
                        color = android.graphics.Color.rgb(52, 211, 153)
                        textSize = 24f
                        isAntiAlias = true
                    }
                    drawText("Rel. vlaga φ = ${curRh.toInt()} %", bx + 12f, by + 102f, pRh)
                }
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16).toInt()
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt.toLong())
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Color(0xFF0284C7)
    }
}
