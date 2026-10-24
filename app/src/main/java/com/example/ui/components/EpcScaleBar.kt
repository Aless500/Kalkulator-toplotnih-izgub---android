package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EpcScaleBar(
    specificKwh: Double,
    modifier: Modifier = Modifier
) {
    val currentClass = when {
        specificKwh <= 10.0 -> "A1"
        specificKwh <= 15.0 -> "A2"
        specificKwh <= 25.0 -> "B1"
        specificKwh <= 35.0 -> "B2"
        specificKwh <= 60.0 -> "C"
        specificKwh <= 105.0 -> "D"
        specificKwh <= 150.0 -> "E"
        else -> "F/G"
    }

    val classes = listOf(
        Pair("A1", Color(0xFF15803D)),
        Pair("A2", Color(0xFF16A34A)),
        Pair("B1", Color(0xFF22C55E)),
        Pair("B2", Color(0xFF84CC16)),
        Pair("C", Color(0xFFEAB308)),
        Pair("D", Color(0xFFF59E0B)),
        Pair("E", Color(0xFFEA580C)),
        Pair("F/G", Color(0xFFDC2626))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        classes.forEach { (name, color) ->
            val isActive = name == currentClass
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) color else color.copy(alpha = 0.35f))
                    .then(
                        if (isActive) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.onSurface,
                            RoundedCornerShape(6.dp)
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = if (isActive) 12.sp else 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
